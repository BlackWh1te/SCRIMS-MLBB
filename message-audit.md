# Message System Audit — MLBB Scrim Host

> **Scope:** End-to-end pipeline for sending, receiving, caching, and persisting messages and conversations in the Android app.  
> **Last updated:** 2026-05-27

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Data Models](#2-data-models)
3. [Cache Hierarchy (L1 / L2 / L3)](#3-cache-hierarchy-l1--l2--l3)
4. [Conversation Lifecycle](#4-conversation-lifecycle)
5. [Sending a Message (Full Pipeline)](#5-sending-a-message-full-pipeline)
6. [Receiving a Message (Full Pipeline)](#6-receiving-a-message-full-pipeline)
7. [Realtime Subscription (WebSocket)](#7-realtime-subscription-websocket)
8. [Typing Status](#8-typing-status)
9. [Chat Gate (Time Lock)](#9-chat-gate-time-lock)
10. [Security & Hardening](#10-security--hardening)
11. [Offline Behavior](#11-offline-behavior)
12. [Memory Leak Protections](#12-memory-leak-protections)
13. [Database Schema (Relevant)](#13-database-schema-relevant)
14. [File Reference Map](#14-file-reference-map)

---

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              UI Layer                                        │
│  ChatScreen ──► MessageViewModel ──► MessageRepositoryInterface               │
│         ▲                              │                                     │
│         │                              ▼                                     │
│         │              ┌──────────────────────────────┐                      │
│         │              │  SupabaseMessageRepository   │                      │
│         │              │                              │                      │
│         │              │  ┌────────────────────────┐  │                      │
│         │              │  │ conversationLookupCache │  │  ← L1 in-memory LRU  │
│         │              │  │ (LinkedHashMap, max 20)│  │    (2 min TTL)       │
│         │              │  └────────────────────────┘  │                      │
│         │              │              │               │                      │
│         │              │  ┌───────────▼───────────┐   │                      │
│         │              │  │ UnifiedCacheManager   │   │  ← L1+L2 coordinator│
│         │              │  │ (stale-while-revalidate)│  │                      │
│         │              │  └───────────┬───────────┘   │                      │
│         │              │              │               │                      │
│         └──────────────┤  ┌───────────▼───────────┐   │                      │
│    Realtime updates    │  │    Room Database      │   │  ← L2 disk cache    │
│    (Flow.collect)      │  │  ConversationDao      │   │                      │
│                        │  │  MessageDao           │   │                      │
│                        │  └───────────┬───────────┘   │                      │
│                        │              │               │                      │
│                        │  ┌───────────▼───────────┐   │                      │
│                        │  │   SupabaseService.api │   │  ← L3 network       │
│                        │  │   (REST + Realtime)   │   │                      │
│                        │  └───────────────────────┘   │                      │
│                        └──────────────────────────────┘                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Key principle:** Every read goes L1 → L2 → L3. Every write invalidates L1+L2 and pushes to L3.

---

## 2. Data Models

### 2.1 Domain Models

**`Message`** (`data/model/Message.kt`)
```kotlin
data class Message(
    val id: String = "",
    val conversationId: String = "",
    val matchId: String? = null,
    val senderId: String = "",
    val senderTeamId: String? = null,
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val readAt: Long? = null,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val voiceUrl: String? = null,
    val voiceDuration: Int? = null
)
```

**`Conversation`** (`data/model/Message.kt`)
```kotlin
data class Conversation(
    val id: String = "",
    val scrimId: String = "",          // empty for direct messages
    val scrimTitle: String = "",
    val participantAId: String = "",
    val participantAName: String = "",
    val participantATeamId: String = "",
    val participantATeamName: String = "",
    val participantBId: String = "",
    val participantBName: String = "",
    val participantBTeamId: String = "",
    val participantBTeamName: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val messages: List<Message> = emptyList(),
    val chatOpensAt: Long = System.currentTimeMillis(),
    val isChatLocked: Boolean = true,     // computed: now < chatOpensAt
    val adminVisible: Boolean = true,
    val isParticipantATyping: Boolean = false,
    val isParticipantBTyping: Boolean = false,
    val tournamentMatchId: String? = null,
    val participantCount: Int = 2,
    val isGroupChat: Boolean = false
)
```

**`MessageType`** (`TEXT | SYSTEM | APPLY | IMAGE | VOICE`)

### 2.2 Room Entities

**`ConversationEntity`** (`data/local/ConversationEntity.kt`)
- Maps 1:1 to `conversations` table (minus the `messages` list, loaded separately)
- `@PrimaryKey val id: String`
- Stores `lastMessage`, `lastMessageTime`, `chatOpensAt`, typing flags, unread count

**`MessageEntity`** (`data/local/MessageEntity.kt`)
- Maps 1:1 to `messages` table
- `@PrimaryKey val id: String`
- Stores `conversationId`, `senderId`, `content`, `timestamp`, `isRead`, `type`, media URLs

### 2.3 DTOs (Network)

**`MessageDto`** / **`ConversationDto`** (`data/service/SupabaseService.kt`)
- Snake-case fields matching Supabase column names
- `parseRealtimeRecordToMessageDto()` converts Realtime JsonObject → MessageDto
- `parseRealtimeRecordToConversationDto()` converts Realtime JsonObject → ConversationDto

---

## 3. Cache Hierarchy (L1 / L2 / L3)

### 3.1 L1 — In-Memory LRU (`conversationLookupCache`)

**Location:** `SupabaseMessageRepository.kt` lines 56–60

```kotlin
private val conversationLookupCache = object : LinkedHashMap<String, CachedConversation>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedConversation>?): Boolean {
        return size > 20   // HARDENED: bounded to prevent OOM
    }
}
```

| Property | Value |
|----------|-------|
| Type | `LinkedHashMap<String, CachedConversation>` (access-ordered LRU) |
| Max size | 20 entries |
| TTL | 2 minutes (`CONV_MEMORY_TTL`) |
| Message freshness window | 60 seconds (`areMessagesFresh()`) |

**What it caches:**
- Individual `Conversation` objects keyed by `conversationId`
- Used by: `sendMessage`, `setTypingStatus`, `getConversationById`, `startDirectConversation`

**Operations:**
- `cacheConversation(conv)` — puts into cache
- `getCachedConversation(id)` — returns if valid (within TTL)
- `invalidateConversationCache(id)` — removes entry

### 3.2 L2 — Room Database

**Tables:** `conversations`, `messages`, `cache_metadata`

**Dao interfaces:**
- `ConversationDao` — query by user, by id, insert, update last message/typing
- `MessageDao` — query by conversation, insert, mark as read
- `CacheMetadataDao` — tracks `cacheKey`, `lastFetched`, `expiresAt`

**TTLs:**
| Data | Memory TTL | Room TTL |
|------|-----------|----------|
| Conversation list | 2 min | 10 min |
| Single conversation | 5 min | 15 min |

### 3.3 L3 — Supabase Network

**REST endpoints used:**
- `getConversationsForUserRpc(p_user_id)` — RPC returning conversation list
- `getConversations(idFilter=eq.{id})` — single conversation
- `getMessages(conversationId=eq.{id})` — message history
- `createConversation(body)` — new conversation
- `sendMessage(dto)` — insert message
- `updateConversation(id, fields)` — typing status, last message
- `markConversationAsRead(params)` — RPC for read receipts

**Realtime WebSocket:**
- `wss://{URL}/realtime/v1/websocket`
- Phoenix channel protocol
- Subscribes to `INSERT` on `messages`, `UPDATE` on `conversations`

---

## 4. Conversation Lifecycle

### 4.1 Creating a Scrim Conversation (`getOrCreateConversation`)

```
User taps "Message" on scrim card
  │
  ▼
MessageViewModel.sendApplyMessage() ──► getOrCreateConversation()
  │
  ├── L1: Check conversationLookupCache for matching scrimId + participant pair
  │   └── HIT → emit cached conversation
  │
  ├── L3: Query API direction 1 (A=sender, B=recipient)
  │   └── HIT → cache + emit
  │
  ├── L3: Query API direction 2 (A=recipient, B=sender)
  │   └── HIT → cache + emit
  │
  └── L3: CREATE new conversation
      ├── Body: scrim_id, participant_a/b_* fields, last_message="", chat_opens_at=NOW+5min
      ├── Server auto-generates UUID for id (HARDENED: no client UUID)
      ├── Persist returned conversation to Room
      ├── Cache in conversationLookupCache
      ├── Invalidate list cache (CACHE_KEY_CONVERSATIONS_PREFIX)
      └── Emit created conversation
```

### 4.2 Creating a Direct Message (`startDirectConversation`)

```
User taps "Message" on player profile
  │
  ▼
MessageViewModel.startDirectConversation()
  │
  ├── L1: Check conversationLookupCache for DM (scrimId.isEmpty()) + participant pair
  │   └── HIT → emit cached
  │
  ├── L2: Query Room for conversations where scrimId IS NULL and participants match
  │   └── HIT → cache + emit
  │
  ├── L3: Query API direction 1 (A=sender, B=recipient)
  │   └── HIT → cache + emit
  │
  ├── L3: Query API direction 2 (A=recipient, B=sender)
  │   └── HIT → cache + emit
  │
  └── L3: CREATE new direct conversation
      ├── Body: participant_a_id/name, participant_b_id/name, last_message="Conversation started"
      ├── No scrim_id → triggers `unique_direct_conversation` index on DB
      ├── Persist to Room, cache, invalidate list cache
      └── Emit
```

**DB protection:** `idx_unique_direct_conversation` (using `LEAST/GREATEST`) prevents duplicate DMs.

### 4.3 Loading Conversation List (`getConversationsForUser`)

```
MessageViewModel.loadConversations(userId)
  │
  ▼
SupabaseMessageRepository.getConversationsForUser(userId)
  │
  └── cacheManager.getFlow<List<Conversation>>(
        key = "conversations_{userId}",
        roomLoader = { conversationDao.getConversationsForUser(userId) },
        networkLoader = { api.getConversationsForUserRpc(...) }
      )
        │
        ├── L1: Memory cache valid? → emit immediately
        ├── L2: Room valid? → emit, promote to L1
        └── L3: Network → emit fresh, save to L1+L2
```

### 4.4 Loading Single Conversation (`getConversationById`)

**Three-tier fallback with deduplication hardening:**

```
getConversationById(conversationId)
  │
  ├── L1: conversationLookupCache hit?
  │   ├── Yes → Load Room messages for instant display
  │   │         If messages fresh (<60s): emit once, return
  │   │         Otherwise: skip emit, fetch network messages, emit once
  │   └── No → continue
  │
  ├── L2: Room conversation hit?
  │   ├── Yes → If messages fresh: emit, return
  │   │         Otherwise: fetch network messages, emit once
  │   └── No → continue
  │
  └── L3: Network
        ├── Fetch conversation + messages
        ├── Persist conversation + messages to Room
        ├── Cache in conversationLookupCache
        └── Emit once
```

**HARDENED:** Previously emitted stale Room data then fresh network data (double emit → UI flicker). Now skips Room emit when about to fetch network.

---

## 5. Sending a Message (Full Pipeline)

### 5.1 Text Message Flow

```
User types message → taps Send
  │
  ▼
MessageViewModel.sendMessage(conversationId, senderId, senderName, content)
  │
  ├── 1. OPTIMISTIC UI UPDATE
  │   ├── Generate local temp ID: "local_${UUID.randomUUID()}"
  │   ├── Create temp Message with timestamp = now
  │   ├── Append to _selectedConversation.value.messages
  │   └── UI instantly shows greyed/temp message
  │
  └── 2. NETWORK SEND (launched in viewModelScope)
        │
        ▼
        SupabaseMessageRepository.sendMessage(...)
          │
          ├── CHAT GATE ENFORCEMENT
          │   ├── Check L1 cache for conversation.chatOpensAt
          │   ├── If locked → emit failure("Chat is locked. Opens in Xs")
          │   └── If not cached → fetch from API, then check
          │
          ├── CONTENT VALIDATION (HARDENED)
          │   ├── content.isBlank() && no media → reject
          │   ├── content.length > 2000 → reject
          │   └── Strip control chars (\x00-\x08, \x0B, \x0C, \x0E-\x1F)
          │
          ├── BUILD DTO
          │   └── MessageDto(conversationId, senderId, senderName, content, type)
          │
          ├── API CALL
          │   └── api.sendMessage(dto) → POST to Supabase
          │
          └── SUCCESS HANDLER
              ├── Map response DTO → domain Message
              ├── Persist to Room: messageDao.insertMessage()
              ├── Update conversation lastMessage in Room
              ├── Best-effort: PATCH server last_message + last_message_time
              └── Emit Result.success(message)

  └── 3. UI REPLACEMENT (on success)
        ├── Find temp message in list by tempId
        ├── Replace with server Message (has real UUID, server timestamp)
        └── _selectedConversation.value = updated list

  └── 4. ERROR HANDLER (on failure)
        └── _error.value = failure message (temp message stays or user retries)
```

### 5.2 Image Message Flow

```
User selects image
  │
  ▼
MessageViewModel.sendImageMessage(conversationId, senderId, senderName, imageBytes)
  │
  ├── Set _isLoading = true
  ├── Upload to Supabase Storage: "chat-media" bucket, path = "chat/{convId}/{timestamp}.png"
  ├── On success:
  │   └── sendMessage(type=IMAGE, imageUrl=url, content="[Image]")
  └── On failure:
      └── _error.value = "Image upload failed: ..."
```

### 5.3 Voice Message Flow

Same as image but:
- Path: `"chat/{convId}/{timestamp}.m4a"`
- Type: `VOICE`
- `voiceUrl=url`, `voiceDuration=duration`
- Content: `"[Voice Note]"`

---

## 6. Receiving a Message (Full Pipeline)

### 6.1 Active Chat Screen (`startChatPolling`)

```
MessageViewModel.startChatPolling(conversationId, userId)
  │
  ├── A. Mark conversation as read (RPC + Room)
  │
  ├── B. Load conversation initially
  │   └── getConversationById(conversationId) → _selectedConversation
  │
  ├── C. Merge two streams:
  │   │
  │   ├── Realtime stream (WebSocket)
  │   │   └── subscribeToMessages(conversationId)
  │   │       ├── Phase 1: Emit cached Room messages (instant display)
  │   │       ├── Phase 2: Bridge fetch — API messages since cache (deduplicate by ID)
  │   │       └── Phase 3: Realtime WebSocket INSERT events
  │   │
  │   └── Polling fallback stream
  │       └── Every 30s: getConversationById → emit each message
  │
  └── D. Deduplication collector
      └── merge(realtimeFlow, pollingFlow)
          ├── If message ID already in list → SKIP
          ├── If message matches a "local_" temp message by content → REPLACE
          └── Otherwise → APPEND + sort by timestamp
```

### 6.2 Conversation List Updates (`startConversationsPolling`)

```
MessageViewModel.startConversationsPolling(userId)
  │
  └── Every 30s:
      └── getConversationsForUser(userId)
          ├── L1/L2 cache hit → emit instantly
          └── L3 refresh if cache expired
              └── _conversations.value = fresh list
```

### 6.3 Background / Push Notification

When app is backgrounded:
- Realtime WebSocket may disconnect
- Room cache persists
- Next foreground: `getConversationById` loads Room messages instantly
- Then re-subscribes to Realtime

---

## 7. Realtime Subscription (WebSocket)

### 7.1 Message Realtime (`subscribeToMessages`)

```kotlin
override fun subscribeToMessages(conversationId: String): Flow<Message> = flow {
    // Phase 1: Cached Room messages
    val cachedIds = mutableSetOf<String>()
    messageDao.getMessagesForConversation(conversationId).first()
        .forEach { emit(it.toDomainModel()); cachedIds.add(it.id) }

    // Phase 2: Bridge fetch (messages between cache time and Realtime start)
    val serverMessages = api.getMessages(conversationId = eq(conversationId))
    serverMessages.forEach { if (it.id !in cachedIds) { emit(it); cachedIds.add(it.id) } }
    messageDao.insertMessages(serverMessages.map { toEntity(it) })

    // Phase 3: Realtime WebSocket
    realtimeClient.connect()
    realtimeClient.subscribe(
        channelName = "public:messages:conv_$conversationId",
        configs = [PostgresChangeConfig(INSERT, table=messages, filter="conversation_id=eq.$conversationId")]
    ).filter { it.eventType == INSERT && it.record != null }
     .collect { event ->
         val msg = parseRealtimeRecordToMessageDto(event.record!!)
         if (msg.id !in cachedIds) {
             cachedIds.add(msg.id)
             messageDao.insertMessage(toEntity(msg))   // persist to Room
             emit(msg)
         }
     }
}
```

### 7.2 Conversation Realtime (`subscribeToConversation`)

```kotlin
override fun subscribeToConversation(conversationId: String): Flow<Conversation> = flow {
    realtimeClient.connect()
    realtimeClient.subscribe(
        channelName = "public:conversations:conv_$conversationId",
        configs = [PostgresChangeConfig(UPDATE, table=conversations, filter="id=eq.$conversationId")]
    ).filter { it.eventType == UPDATE && it.record != null }
     .collect { event ->
         val conv = parseRealtimeRecordToConversationDto(event.record!!)
         emit(mapDtoToConversation(conv))
     }
}
```

**Used for:** Typing status updates (`participant_a_typing`, `participant_b_typing`)

---

## 8. Typing Status

### 8.1 Sending Typing Status

```
User starts typing
  │
  ▼
MessageViewModel.updateTypingStatus(conversationId, userId, isTyping=true)
  │
  ├── Cancel previous typingStatusJob
  ├── Launch new job:
  │   ├── Immediately: api.updateConversation(convId, {participant_a_typing: true})
  │   ├── delay(3000)
  │   └── Auto-clear: api.updateConversation(convId, {participant_a_typing: false})
  │
  └── User stops typing (isTyping=false):
      └── Immediately send false
```

**HARDENED:** Debounced — sends `true` immediately, auto-`false` after 3s inactivity. Prevents API spam on every keystroke.

### 8.2 Receiving Typing Status

```
Other user types
  │
  └── Supabase DB UPDATE on conversations.participant_a/b_typing
      │
      └── Realtime pushes UPDATE event
          │
          └── MessageViewModel.conversationUpdatesJob collects
              └── _selectedConversation.value = current.copy(
                      isParticipantATyping = updated.isParticipantATyping,
                      isParticipantBTyping = updated.isParticipantBTyping
                  )
```

---

## 9. Chat Gate (Time Lock)

### 9.1 Server-Side Enforcement

**Database trigger:** `enforce_chat_gate()` (`supabase/schema.sql`)

```sql
CREATE OR REPLACE FUNCTION enforce_chat_gate()
RETURNS TRIGGER AS $$
DECLARE
    v_chat_opens_at TIMESTAMP WITH TIME ZONE;
BEGIN
    SELECT chat_opens_at INTO v_chat_opens_at
    FROM conversations
    WHERE id = NEW.conversation_id AND scrim_id IS NOT NULL;

    IF v_chat_opens_at IS NOT NULL AND v_chat_opens_at > TIMEZONE('utc', NOW()) THEN
        RAISE EXCEPTION 'Chat is locked until %', v_chat_opens_at;
    END IF;
    RETURN NEW;
END;
```

**RLS policy also enforces:** `chat_opens_at <= TIMEZONE('utc', NOW())`

### 9.2 Client-Side Pre-Check

In `sendMessage()`:
1. Check L1 cache for `chatOpensAt`
2. If locked → emit failure immediately (no network call)
3. If not cached → fetch conversation from API, then check

This provides fast UX feedback even if server is the ultimate authority.

---

## 10. Security & Hardening

| Issue | Fix Location | Description |
|-------|-------------|-------------|
| Unbounded cache memory | `SupabaseMessageRepository.kt:56` | LRU `LinkedHashMap` max 20 entries |
| Empty message crash | `sendMessage():348` | Reject blank content with no media |
| Message length DoS | `sendMessage():353` | Max 2000 characters |
| Control char injection | `sendMessage():357` | Strip `\x00-\x1F` control chars |
| Double emit on load | `getConversationById():135` | Skip stale Room emit when fetching network |
| Client UUID generation | `getOrCreateConversation():280` | Removed `UUID.randomUUID()` — server generates IDs |
| Typing API spam | `MessageViewModel:263` | 3-second debounce with auto-clear |
| Duplicate DMs | `schema.sql:739` | `idx_unique_direct_conversation` partial unique index |
| Chat gate bypass | `schema.sql` + `sendMessage()` | Client pre-check + server trigger + RLS |
| Read receipts | `markConversationAsRead()` | RPC + Room `markMessagesAsRead()` |

---

## 11. Offline Behavior

### 11.1 Loading Conversations (Offline)

```
getConversationsForUser()
  ├── Network fails
  ├── Catch: try Room fallback
  │   └── conversationDao.getConversationsForUser(userId)
  │       └── If not empty → emit cached list
  │       └── If empty → emit failure
  └── User sees last-known conversation list
```

### 11.2 Loading Single Conversation (Offline)

```
getConversationById()
  ├── Network fails at any stage
  ├── Catch: try Room
  │   └── conversationDao.getConversationById(id)
  │   └── messageDao.getMessagesForConversation(id)
  │   └── If found → emit conversation with cached messages
  │   └── If not found → emit failure
  └── User sees last-known messages
```

### 11.3 Sending Message (Offline)

```
sendMessage()
  ├── Network fails
  ├── emit Result.failure(Exception)
  ├── UI shows error toast
  └── Temp "local_" message remains visible (user can retry)
```

**Note:** There is currently no offline outbox/queue. Messages are not automatically retried when connection returns.

---

## 12. Memory Leak Protections

### 12.1 ViewModel Cleanup

`MessageViewModel.onCleared()` cancels all active jobs:

```kotlin
override fun onCleared() {
    super.onCleared()
    chatPollingJob?.cancel()          // Message polling
    convPollingJob?.cancel()          // Conversation list polling
    typingStatusJob?.cancel()         // Typing debounce
    conversationUpdatesJob?.cancel()  // Realtime conversation subscription
}
```

### 12.2 Realtime Subscription Lifecycle

- `startChatPolling()` → subscribes to Realtime
- `stopChatPolling()` → cancels jobs (Realtime channel may still exist in `SupabaseRealtimeClient`)
- `onCleared()` → cancels all coroutine jobs

**Note:** `SupabaseRealtimeClient` maintains a singleton WebSocket connection. Channels are not explicitly unsubscribed when ViewModel clears — they are filtered by `conversationId` in the collector.

---

## 13. Database Schema (Relevant)

### 13.1 `conversations` Table

```sql
CREATE TABLE conversations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scrim_id        UUID REFERENCES scrims(id) ON DELETE CASCADE,
    participant_a_id    UUID REFERENCES profiles(id) ON DELETE CASCADE,
    participant_a_name  TEXT,
    participant_a_team_id   UUID REFERENCES teams(id),
    participant_a_team_name TEXT,
    participant_b_id    UUID REFERENCES profiles(id) ON DELETE CASCADE,
    participant_b_name  TEXT,
    participant_b_team_id   UUID REFERENCES teams(id),
    participant_b_team_name TEXT,
    last_message    TEXT DEFAULT '',
    last_message_time   TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    chat_opens_at   TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    participant_a_typing    BOOLEAN DEFAULT FALSE,
    participant_b_typing    BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW())
);
```

### 13.2 `messages` Table

```sql
CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    match_id        UUID REFERENCES matches(id) ON DELETE CASCADE,
    sender_id       UUID REFERENCES profiles(id) ON DELETE CASCADE,
    sender_team_id  UUID REFERENCES teams(id),
    sender_name     TEXT,
    content         TEXT NOT NULL,
    type            TEXT DEFAULT 'TEXT',
    image_url       TEXT,
    voice_url       TEXT,
    voice_duration  INTEGER,
    is_read         BOOLEAN DEFAULT FALSE,
    read_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()),
    is_edited       BOOLEAN DEFAULT FALSE
);
```

### 13.3 Triggers

| Trigger | Table | Purpose |
|---------|-------|---------|
| `update_conversation_on_message` | `messages` | Auto-updates `conversations.last_message` and `last_message_time` on INSERT |
| `enforce_chat_gate` | `messages` | Rejects INSERT if `chat_opens_at > NOW()` |

### 13.4 Indexes

```sql
CREATE UNIQUE INDEX idx_unique_direct_conversation ON conversations (
    LEAST(participant_a_id, participant_b_id),
    GREATEST(participant_a_id, participant_b_id)
) WHERE scrim_id IS NULL;

CREATE INDEX idx_conversations_participant_a_time ON conversations(participant_a_id, last_message_time DESC);
CREATE INDEX idx_conversations_participant_b_time ON conversations(participant_b_id, last_message_time DESC);
```

### 13.5 RLS (Messages)

```sql
CREATE POLICY messages_insert_policy ON messages FOR INSERT
WITH CHECK (
    auth.uid() = sender_id AND
    EXISTS (
        SELECT 1 FROM conversations c
        WHERE c.id = messages.conversation_id
        AND (c.participant_a_id = auth.uid() OR c.participant_b_id = auth.uid())
        AND c.chat_opens_at <= TIMEZONE('utc', NOW())
    )
);
```

---

## 14. File Reference Map

| File | Role |
|------|------|
| `data/model/Message.kt` | Domain models: `Message`, `Conversation`, `MessageType` |
| `data/local/ConversationEntity.kt` | Room entity for `conversations` table |
| `data/local/MessageEntity.kt` | Room entity for `messages` table |
| `data/local/ConversationDao.kt` | Room DAO for conversations |
| `data/local/MessageDao.kt` | Room DAO for messages |
| `data/cache/UnifiedCacheManager.kt` | L1/L2 cache coordinator |
| `data/repository/MessageRepositoryInterface.kt` | Repository interface |
| `data/repository/SupabaseMessageRepository.kt` | **Main implementation** — all message logic |
| `viewmodel/MessageViewModel.kt` | ViewModel exposing StateFlows to UI |
| `data/service/SupabaseRealtimeClient.kt` | WebSocket client for Realtime |
| `data/service/SupabaseService.kt` | Retrofit API service |
| `supabase/schema.sql` | Database schema + triggers + RLS |
| `supabase/migrations/20260531060004_ultimate_messaging_fix.sql` | Latest messaging fixes |

---

## Appendix: Quick Trace — "Send a text message"

```
ChatScreen.SendButton.onClick
  → MessageViewModel.sendMessage(convId, senderId, senderName, content)
    → 1. Create temp "local_" Message, append to _selectedConversation (instant UI)
    → 2. SupabaseMessageRepository.sendMessage(...)
      → 2a. Chat gate check (L1 cache → API fallback)
      → 2b. Content validation (empty, length, control chars)
      → 2c. Build MessageDto → api.sendMessage(dto)
      → 2d. On success: persist to Room, update conversation last_message
    → 3. On success: replace temp message with server Message in _selectedConversation
    → 4. Realtime broadcasts to other client via WebSocket
    → 5. Other client's subscribeToMessages() emits new message → UI updates
```
