# Messaging Production Implementation Plan — MLBB Scrim Host

> **Status:** Implementation in progress  
> **Scope:** Android Kotlin messaging pipeline (MVVM + Room + Supabase + Realtime + StateFlow)  
> **Target:** Production-grade messaging with idempotency, offline delivery, lifecycle management, and security hardening.

---

## # Current Problems

1. **Optimistic deduplication is brittle** — `local_UUID` temp messages are matched by content string equality. Collisions possible, no server acknowledgement guarantee.
2. **Polling + WebSocket conflict** — `startChatPolling` merges a 30s REST polling loop with Realtime WebSocket, causing duplicate message emissions and wasted battery.
3. **Realtime subscriptions leak** — `subscribeToMessages` returns a `Flow` but the channel is never explicitly unsubscribed when the ViewModel clears.
4. **No offline outbox** — Failed messages remain as orphaned local temp state. No retry, no sync on reconnect, no process-death survival.
5. **Cache lacks invalidation determinism** — `UnifiedCacheManager` uses TTL but no version vectors or event-driven invalidation. Race conditions possible on rapid send/receive.
6. **Concurrency holes** — `conversationLookupCache` is accessed from multiple coroutines without synchronization. `sendMessage` has no Mutex.
7. **Typing system spams API** — `updateTypingStatus(true)` fires immediately, then auto-false after 3s. Rapid typing causes back-to-back `true` calls.
8. **Database missing delivery columns** — `messages` table has no `client_message_id`, `delivery_status`, or idempotency constraints.
9. **Security gaps** — No message rate limiting, no media validation beyond file extension, no replay protection on `sendMessage`.
10. **No delivery states in UI** — Messages appear instantly but user cannot distinguish SENT vs DELIVERED vs READ vs FAILED.

---

## # Root Cause

| Problem | Root Cause |
|---------|-----------|
| Deduplication by content | Missing `client_message_id` concept; no idempotency key round-tripped to server |
| Polling + WebSocket dual path | No connection state machine; polling and Realtime are treated as independent flows instead of primary/fallback |
| Subscription leaks | `SupabaseRealtimeClient.subscribe()` returns `callbackFlow` with `awaitClose`, but `MessageViewModel` cancels the collection job without calling `unsubscribe()` on the channel |
| Offline message loss | `sendMessage` is fire-and-forget with no persistent outbox; `Result.failure` is only emitted to a cold Flow collector that may be gone |
| Cache races | `LinkedHashMap` is not thread-safe for read-modify-write; `getConversationById` does concurrent cache + Room + network access without Mutex |
| Typing spam | `typingStatusJob` is cancelled and recreated on every keystroke; no `distinctUntilChanged` gate |
| DB schema gap | Original design assumed immediate server success; no need for delivery tracking columns |
| Security | MVP focus on functionality over abuse prevention |

---

## # Architecture Changes

```
BEFORE                                          AFTER
─────────────────────────────────────────────────────────────────────────
sendMessage() → API call (fire & forget)        sendMessage() → outbox INSERT → Mutex-locked
                                                  network attempt → retry with
                                                  exponential backoff → SENT/FAILED

local_UUID + content match                       client_message_id (cm_UUID) +
                                                 server unique index

Polling 30s + Realtime simultaneous              Realtime PRIMARY; polling only
                                                 for conversation list; chat
                                                 screen has NO polling

subscribeToMessages() cold Flow                subscribeToMessages() with
                                                 explicit unsubscribeFromMessages()
                                                 + activeSubscriptions tracking

No outbox → lost on process death                pending_messages Room table +
                                                 WorkManager periodic sync +
                                                 battery-aware constraints

Cache TTL only                                  Cache TTL + Mutex + hit/miss
                                                 metrics + event-driven invalidate

Typing: immediate true + 3s delayed false       Debounced 300ms + distinctUntilChanged
                                                 + 3s auto-timeout

Messages: id, content, timestamp                + clientMessageId, deliveryStatus,
                                                 readAt, isEdited

No connection state in UI                       ChatConnectionState StateFlow:
                                                 DISCONNECTED/CONNECTING/CONNECTED/
                                                 RECONNECTING/FALLBACK_POLLING
```

---

## # Files To Modify

| File | Change Type |
|------|-------------|
| `app/build.gradle.kts` | ADD WorkManager + Hilt WorkManager compiler |
| `data/model/DeliveryStatus.kt` | NEW enum + `MessageWithDelivery` wrapper |
| `data/service/ChatConnectionState.kt` | NEW top-level enum |
| `data/local/PendingMessageEntity.kt` | NEW Room entity |
| `data/local/PendingMessageDao.kt` | NEW Room DAO |
| `data/local/MessageEntity.kt` | ADD `deliveryStatus`, `clientMessageId` columns |
| `data/local/DatabaseMigrations.kt` | ADD `MIGRATION_10_11` |
| `data/local/MLBBScrimDatabase.kt` | Version 11, add `PendingMessageEntity` + DAO |
| `data/repository/MessageRepositoryInterface.kt` | EXPAND with `clientMessageId`, `retryMessage`, `cancelMessage`, `syncOutbox`, `unsubscribeFromMessages`, `observeConnectionState` |
| `data/repository/SupabaseMessageRepository.kt` | FULL REWRITE — Mutex, outbox, retry, connection bridge, cache metrics |
| `viewmodel/MessageViewModel.kt` | REWRITE — `messagesWithDelivery`, `connectionState`, `typingIndicator`, `startChatSubscription`/`stopChatSubscription` |
| `di/DatabaseModule.kt` | ADD `PendingMessageDao` provider |
| `di/RepositoryModule.kt` | ADD `pendingMessageDao` to repository constructor |
| `di/WorkerModule.kt` | NEW — WorkManager + periodic `MessageSyncWorker` enqueue |
| `worker/MessageSyncWorker.kt` | NEW HiltWorker |
| `supabase/migrations/20260527090001_production_messaging_hardening.sql` | NEW — DB schema hardening |

---

## # Exact Kotlin Changes

### 1. build.gradle.kts — WorkManager dependency
```kotlin
// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")
implementation("androidx.hilt:hilt-work:1.2.0")
ksp("androidx.hilt:hilt-compiler:1.2.0")
```

### 2. MessageRepositoryInterface — new contract
```kotlin
suspend fun sendMessage(
    conversationId: String, senderId: String, senderName: String,
    content: String, type: MessageType, clientMessageId: String,
    imageUrl: String? = null, voiceUrl: String? = null, voiceDuration: Int? = null
): Flow<MessageWithDelivery>

suspend fun retryMessage(clientMessageId: String): Flow<MessageWithDelivery>
suspend fun cancelMessage(clientMessageId: String): Result<Unit>
suspend fun syncOutbox(): Result<Int>
fun unsubscribeFromMessages(conversationId: String)
fun observeConnectionState(): Flow<ChatConnectionState>
```

### 3. SupabaseMessageRepository — idempotent send pipeline
```kotlin
private val sendMutex = Mutex()
private val cacheMutex = Mutex()

override suspend fun sendMessage(...): Flow<MessageWithDelivery> = flow {
    val pending = PendingMessageEntity(clientMessageId = clientMessageId, ...)
    pendingMessageDao.insert(pending)          // Phase 1: persist to outbox
    emit(pending.toDomainModel())               // Phase 2: UI sees PENDING
    val result = sendMessageInternal(pending)     // Phase 3: Mutex-locked network attempt
    emit(result)                                 // Phase 4: UI sees SENT or FAILED
}

private suspend fun sendMessageInternal(pending: PendingMessageEntity): MessageWithDelivery {
    return sendMutex.withLock {
        pendingMessageDao.updateStatus(pending.clientMessageId, "SENDING")
        // chat gate + validation + api.sendMessage()
        // on success: messageDao.insertMessage(...deliveryStatus="SENT", clientMessageId=...)
        // on failure: handleRetryableFailure() with exponential backoff
    }
}
```

### 4. MessageViewModel — delivery-aware UI state
```kotlin
private val _messagesWithDelivery = MutableStateFlow<List<MessageWithDelivery>>(emptyList())
val messagesWithDelivery: StateFlow<List<MessageWithDelivery>> = _messagesWithDelivery.asStateFlow()

fun sendMessage(conversationId: String, senderId: String, senderName: String, content: String) {
    val clientMessageId = "cm_${UUID.randomUUID()}"
    // Optimistic: append SENDING item immediately
    _messagesWithDelivery.value = current + MessageWithDelivery(tempMessage, SENDING, clientMessageId)
    viewModelScope.launch {
        messageRepository.sendMessage(..., clientMessageId = clientMessageId).collect { delivery ->
            updateDeliveryState(clientMessageId, delivery)
        }
    }
}
```

### 5. MessageSyncWorker — WorkManager outbox drain
```kotlin
@HiltWorker
class MessageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageRepository: MessageRepositoryInterface
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return messageRepository.syncOutbox().fold(
            onSuccess = { count -> if (count > 0) Result.retry() else Result.success() },
            onFailure = { Result.retry() }
        )
    }
}
```

### 6. SupabaseRealtimeClient — lifecycle unsubscribe bridge
```kotlin
override fun unsubscribeFromMessages(conversationId: String) {
    activeSubscriptions.remove(conversationId)
    val channelName = "public:${SupabaseConfig.TABLE_MESSAGES}:conv_$conversationId"
    realtimeClient.unsubscribe(channelName)
}
```

---

## # SQL Migrations

File: `supabase/migrations/20260527090001_production_messaging_hardening.sql`

Key statements:
1. `ALTER TABLE messages ADD COLUMN client_message_id TEXT;`
2. `ALTER TABLE messages ADD COLUMN delivery_status TEXT NOT NULL DEFAULT 'SENT';`
3. `CREATE INDEX idx_messages_client_id ON messages(client_message_id);`
4. `CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at DESC);`
5. `CREATE INDEX idx_messages_unread ON messages(conversation_id, sender_id, is_read) WHERE is_read = FALSE;`
6. `CREATE TABLE message_rate_limits (user_id UUID PRIMARY KEY, window_start TIMESTAMPTZ, message_count INT);`
7. `CREATE TRIGGER trg_message_rate_limit BEFORE INSERT ON messages ...`
8. `CREATE UNIQUE INDEX idx_unique_client_message ON messages(conversation_id, client_message_id) WHERE client_message_id IS NOT NULL;`
9. `CREATE FUNCTION upsert_message_with_idempotency(...) ...`
10. `CREATE FUNCTION get_conversation_unread_count(...) ...`

---

## # Repository Refactor

### Before
- `sendMessage(...)` returns `Flow<Result<Message>>`
- No outbox, no retry
- `conversationLookupCache` is raw `LinkedHashMap` accessed without synchronization
- `subscribeToMessages` leaks channel on ViewModel cancellation

### After
- `sendMessage(...)` returns `Flow<MessageWithDelivery>` with PENDING → SENDING → SENT/FAILED
- `PendingMessageDao` outbox with exponential backoff retry
- `sendMutex` guards network call; `cacheMutex` guards LRU map
- `activeSubscriptions` tracks live channels; `unsubscribeFromMessages()` explicitly leaves channel
- `syncOutbox()` callable by WorkManager for background drain
- Cache hit/miss counters for telemetry
- `ChatConnectionState` bridge from `SupabaseRealtimeClient.ConnectionState`

---

## # Room Changes

### New entities
- `PendingMessageEntity` — outbox table with `clientMessageId`, `status`, `retryCount`, `nextRetryAt`, `errorReason`

### Modified entities
- `MessageEntity` — added `deliveryStatus` (default `"SENT"`), `clientMessageId` (nullable)

### New DAO
- `PendingMessageDao` — `getPendingMessages()`, `getMessagesReadyForRetry()`, `markRetry()`, `markFailed()`, `pruneSent()`

### Migration
- `MIGRATION_10_11` — creates `pending_messages` table + indexes, adds columns to `messages`

---

## # Supabase Changes

### Schema
- `messages.client_message_id` — idempotency key
- `messages.delivery_status` — `'SENT' | 'DELIVERED' | 'READ'`
- `idx_unique_client_message` — partial unique index preventing duplicate sends
- `idx_messages_conversation_created` — pagination performance
- `idx_messages_unread` — fast unread count per conversation

### Functions / Triggers
- `enforce_message_rate_limit()` — 30 messages/minute per user
- `upsert_message_with_idempotency()` — RPC for atomic insert-or-return-existing
- `get_conversation_unread_count()` — fast server-side unread count

### RLS
- `messages_insert_policy` tightened: `auth.uid() = sender_id` + participant membership + chat gate

---

## # New Classes

| Class | Package | Role |
|-------|---------|------|
| `DeliveryStatus` | `data.model` | Enum: PENDING, SENDING, SENT, DELIVERED, READ, FAILED, CANCELLED |
| `MessageWithDelivery` | `data.model` | Wrapper: `Message` + `status` + `clientMessageId` + retry metadata |
| `ChatConnectionState` | `data.service` | Enum: DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FALLBACK_POLLING |
| `PendingMessageEntity` | `data.local` | Room outbox entity |
| `PendingMessageDao` | `data.local` | Room outbox DAO |
| `MessageSyncWorker` | `worker` | HiltWorker periodic outbox sync |
| `WorkerModule` | `di` | Hilt module: WorkManager + periodic enqueue |

---

## # Code Diff Examples

### Diff: sendMessage signature (Repository)
```diff
- suspend fun sendMessage(
-     conversationId: String, senderId: String, senderName: String,
-     content: String, type: MessageType,
-     imageUrl: String? = null, voiceUrl: String? = null, voiceDuration: Int? = null
- ): Flow<Result<Message>>
+ suspend fun sendMessage(
+     conversationId: String, senderId: String, senderName: String,
+     content: String, type: MessageType, clientMessageId: String,
+     imageUrl: String? = null, voiceUrl: String? = null, voiceDuration: Int? = null
+ ): Flow<MessageWithDelivery>
```

### Diff: ViewModel chat lifecycle
```diff
- fun startChatPolling(conversationId: String, userId: String) {
-     chatPollingJob?.cancel()
-     chatPollingJob = viewModelScope.launch {
-         // ... merge realtimeFlow + pollingFlow
-     }
- }
+ fun startChatSubscription(conversationId: String, userId: String) {
+     chatSubscriptionJob?.cancel()
+     messageRepository.unsubscribeFromMessages(conversationId)
+     chatSubscriptionJob = viewModelScope.launch {
+         messageRepository.subscribeToMessages(conversationId).collect { integrateMessage(it) }
+     }
+ }
+ fun stopChatSubscription(conversationId: String) {
+     chatSubscriptionJob?.cancel()
+     messageRepository.unsubscribeFromMessages(conversationId)
+ }
```

### Diff: onCleared cleanup
```diff
  override fun onCleared() {
      super.onCleared()
-     chatPollingJob?.cancel()
+     chatSubscriptionJob?.cancel()
      convPollingJob?.cancel()
-     typingStatusJob?.cancel()
+     typingDebounceJob?.cancel()
      conversationUpdatesJob?.cancel()
+     _selectedConversation.value?.id?.let { messageRepository.unsubscribeFromMessages(it) }
  }
```

---

## # Step-by-Step Implementation Order

1. **Gradle** — Add WorkManager + Hilt WorkManager dependencies
2. **Models** — Create `DeliveryStatus.kt`, `MessageWithDelivery`, `ChatConnectionState`
3. **Room** — Create `PendingMessageEntity`, `PendingMessageDao`, update `MessageEntity`, add migration 10→11, bump DB version
4. **DI** — Provide `PendingMessageDao`, update repository constructor, create `WorkerModule`
5. **Interface** — Expand `MessageRepositoryInterface` with new methods
6. **Repository** — Rewrite `SupabaseMessageRepository` with Mutex, outbox, retry, connection state bridge
7. **ViewModel** — Rewrite `MessageViewModel` with delivery tracking, proper lifecycle, debounced typing
8. **Worker** — Create `MessageSyncWorker` with exponential backoff policy
9. **Supabase SQL** — Run migration for `client_message_id`, indexes, rate limit trigger, idempotency function
10. **QA** — Run `./gradlew test`, verify no regression in existing conversation flows

---

## # Regression Risks

| Risk | Mitigation |
|------|-----------|
| DB migration failure on user devices with v10 | `fallbackToDestructiveMigration()` already present; migration 10→11 is additive-only (safe) |
| `clientMessageId` breaks existing tests that call `sendMessage` without new param | Fixed: updated interface + all call sites in ViewModel; test files referencing old signature must be updated |
| WorkManager not initialized if `Application` class lacks `Configuration.Provider` | Hilt auto-initializes WorkManager via `@HiltAndroidApp`; if custom config exists, add `Configuration.Provider` to Application |
| `GlobalScope` in repository `init` block | Scoped to `GlobalScope` for bridging only; no UI logic inside it. Consider `applicationScope` injected from Hilt in future refactor |
| Rate limit trigger rejects legitimate burst traffic | Set to 30/minute (generous); monitor and tune via `v_max_per_minute` constant |
| Realtime client internal `ConnectionState` renamed | No rename done; mapped to new `ChatConnectionState` externally |
| Increased Room write frequency (outbox) | `PendingMessageEntity` is lightweight; `pruneSent()` removes old rows daily |

---

## # Production Readiness Score

| Dimension | Before | After | Delta |
|-----------|--------|-------|-------|
| Idempotency | 2/10 | 9/10 | +7 |
| Offline resilience | 1/10 | 8/10 | +7 |
| Realtime lifecycle | 4/10 | 8/10 | +4 |
| Concurrency safety | 3/10 | 8/10 | +5 |
| Delivery transparency | 1/10 | 8/10 | +7 |
| Security (rate limits) | 2/10 | 7/10 | +5 |
| Cache correctness | 5/10 | 8/10 | +3 |
| Observability | 2/10 | 6/10 | +4 |
| **Overall** | **2.5/10** | **7.8/10** | **+5.3** |

**Remaining to reach 9+/10:**
- Server-side `delivery_status` webhook propagation (DELIVERED/READ states)
- End-to-end encryption for direct messages
- Message edit/delete with tombstones
- Push notification integration (FCM) for offline delivery
- Distributed tracing / structured logging to backend telemetry

---

## Build Verification

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

Run after all files are applied. Fix any `No value passed for parameter 'clientMessageId'` errors in test call sites.
