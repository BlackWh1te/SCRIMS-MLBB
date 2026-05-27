package com.mlbb.scrim.data.service

import timber.log.Timber
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import okhttp3.*
import okio.ByteString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase Realtime client using OkHttp WebSocket + Phoenix channels protocol.
 *
 * Connects to `wss://{SUPABASE_URL}/realtime/v1/websocket` and subscribes to
 * Postgres Change events (INSERT/UPDATE/DELETE) for instant data delivery.
 *
 * Uses Supabase Realtime v2 channel-based protocol:
 * - Channel names are arbitrary (e.g. "public:messages", "public:scrims")
 * - `postgres_changes` config is sent in the phx_join payload
 * - Server sends `postgres_changes` events with `payload.data` containing the record
 *
 * Falls back gracefully: if the WebSocket fails or the Supabase project has Realtime
 * disabled, consumers should use REST polling as a fallback.
 */
@Singleton
class SupabaseRealtimeClient @Inject constructor() {

    companion object {
        private const val TAG = "SupabaseRealtime"
        private const val PHOENIX_EVENT_JOIN = "phx_join"
        private const val PHOENIX_EVENT_REPLY = "phx_reply"
        private const val PHOENIX_EVENT_HEARTBEAT = "heartbeat"
        private const val PHOENIX_EVENT_CLOSE = "phx_close"
        private const val PHOENIX_EVENT_ERROR = "phx_error"

        // Supabase Realtime event types
        const val EVENT_INSERT = "INSERT"
        const val EVENT_UPDATE = "UPDATE"
        const val EVENT_DELETE = "DELETE"

        private val defaultOkHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build()
        }

        /** Build the WebSocket URL from the Supabase HTTP URL. */
        fun buildWsUrl(): String {
            val httpUrl = SupabaseConfig.SUPABASE_URL.trimEnd('/')
            val wsUrl = httpUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://")
            return "$wsUrl/realtime/v1/websocket?apikey=${SupabaseConfig.SUPABASE_ANON_KEY}&vsn=1.0.0"
        }
    }

    private val client: OkHttpClient = defaultOkHttpClient
    private val gson: Gson = Gson()

    // ─── Phoenix channel protocol state (thread-safe) ───

    @Volatile
    private var ws: WebSocket? = null
    private val refCounter = AtomicLong(1L)
    private val reconnectJob = AtomicReference<Job?>(null)
    private val isConnected = AtomicBoolean(false)
    private val reconnectAttempt = AtomicInteger(0)
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Internal event bus — subscribe() reads from this via callbackFlow
    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)

    // Track active channel subscriptions: channelName -> PostgresChangeConfig (thread-safe)
    private val activeChannels = ConcurrentHashMap<String, List<PostgresChangeConfig>>()

    // Track join refs awaiting reply (thread-safe)
    private val pendingJoins = ConcurrentHashMap<Long, CompletableDeferred<Boolean>>()

    // Track per-channel subscriber counts for lifecycle management
    private val channelSubscribers = ConcurrentHashMap<String, AtomicInteger>()

    // Track per-channel join refs — only incremented on actual phx_join
    private val channelJoinRefs = ConcurrentHashMap<String, AtomicLong>()

    private val heartbeatLoopJob = AtomicReference<Job?>(null)

    /**
     * A Realtime event emitted from the WebSocket.
     */
    data class RealtimeEvent(
        val table: String,
        val eventType: String, // INSERT, UPDATE, DELETE
        val record: JsonObject?,
        val oldRecord: JsonObject?,
        val channelName: String = ""
    )

    /**
     * Connection state for UI indicators (Connecting..., Offline mode, etc.)
     */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    // Expose connection state for UI consumption
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /**
     * Configuration for a Postgres Change subscription.
     */
    data class PostgresChangeConfig(
        val event: String = "*",       // INSERT, UPDATE, DELETE, or *
        val schema: String = "public",
        val table: String,
        val filter: String? = null     // e.g. "conversation_id=eq.abc123"
    )

    // ─── Public API ───

    /**
     * Connect to the WebSocket. Safe to call multiple times.
     * Returns true if connection is established or already connected.
     */
    fun connect(): Boolean {
        if (isConnected.get() && ws != null) return true

        _connectionState.value = ConnectionState.CONNECTING
        val url = buildWsUrl()
        val request = Request.Builder().url(url).build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.d(TAG, "WebSocket connected")
                isConnected.set(true)
                _connectionState.value = ConnectionState.CONNECTED
                reconnectAttempt.set(0)
                startHeartbeat()
                // Re-subscribe to any active channels
                scope.launch { resubscribeAll() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handlePhoenixMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Not used by Phoenix channels
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.w(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.d(TAG, "WebSocket closed: $code $reason")
                handleDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.e(TAG, "WebSocket failure: ${t.message}")
                handleDisconnect()
            }
        })
        return true
    }

    /**
     * Disconnect the WebSocket and cancel all subscriptions.
     */
    fun disconnect() {
        heartbeatLoopJob.getAndSet(null)?.cancel()
        reconnectJob.getAndSet(null)?.cancel()
        ws?.close(1000, "Client disconnect")
        ws = null
        isConnected.set(false)
        _connectionState.value = ConnectionState.DISCONNECTED
        activeChannels.clear()
        channelSubscribers.clear()
        channelJoinRefs.clear()
        pendingJoins.clear()
        // Cancel the scope so orphaned coroutines (heartbeat, reconnect) don't linger,
        // then recreate it so the client can be reconnected later.
        scope.cancel()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    /**
     * Subscribe to Postgres Change events on a specific table.
     * Automatically connects the WebSocket if not already connected.
     *
     * Uses callbackFlow so that when the collector cancels, the channel is
     * automatically left via awaitClose. Events are filtered per-channel
     * so subscribers only receive events relevant to their subscription.
     *
     * @param channelName A unique name for this subscription channel (e.g. "public:messages:conv_123")
     * @param configs List of PostgresChangeConfig describing what events to listen for
     * @return Flow of RealtimeEvent filtered for this channel only
     */
    fun subscribe(
        channelName: String,
        configs: List<PostgresChangeConfig>
    ): Flow<RealtimeEvent> = callbackFlow {
        // Register this subscriber
        val count = channelSubscribers.getOrPut(channelName) { AtomicInteger(0) }
        count.incrementAndGet()

        activeChannels[channelName] = configs

        if (!isConnected.get()) {
            connect()
            // Give connection a moment to establish before joining
            scope.launch {
                delay(500)
                joinChannel(channelName, configs)
            }
        } else {
            joinChannel(channelName, configs)
        }

        // Collect from the global SharedFlow, filtering for this channel only
        val collectJob = scope.launch {
            _events.filter { it.channelName == channelName }.collect { event ->
                trySend(event)
            }
        }

        awaitClose {
            // When the collector cancels, clean up this subscription
            collectJob.cancel()
            val remaining = channelSubscribers[channelName]?.decrementAndGet() ?: 0
            if (remaining <= 0) {
                channelSubscribers.remove(channelName)
                unsubscribe(channelName)
            }
        }
    }

    /**
     * Subscribe to a single table with optional filter.
     * Convenience method wrapping subscribe().
     */
    fun subscribeToTable(
        table: String,
        event: String = "*",
        filter: String? = null,
        channelName: String = buildChannelName(table, filter)
    ): Flow<RealtimeEvent> {
        val config = PostgresChangeConfig(
            event = event,
            table = table,
            filter = filter
        )
        return subscribe(channelName, listOf(config))
    }

    /**
     * Unsubscribe from a specific channel.
     */
    fun unsubscribe(channelName: String) {
        activeChannels.remove(channelName)
        if (isConnected.get()) {
            leaveChannel(channelName)
        }
        // If no more subscriptions, disconnect
        if (activeChannels.isEmpty()) {
            disconnect()
        }
    }

    /**
     * Check if currently connected.
     */
    fun isConnected(): Boolean = isConnected.get()

    // ─── Phoenix channel protocol ───

    private fun nextRef(): Long = refCounter.getAndIncrement()

    /**
     * Phoenix channel message format: [join_ref, ref, topic, event, payload]
     * join_ref should be 0 for non-join messages (heartbeat, leave, push).
     */
    private fun sendPhoenixMessage(
        topic: String,
        event: String,
        payload: JsonObject = JsonObject(),
        joinRef: Long = 0L
    ): Long {
        val ref = nextRef()
        val message = gson.toJson(arrayOf(joinRef, ref, topic, event, payload))
        val sent = ws?.send(message)
        if (sent != true) {
            Timber.w(TAG, "Failed to send Phoenix message: topic=$topic event=$event")
        }
        return ref
    }

    private fun joinChannel(channelName: String, configs: List<PostgresChangeConfig>) {
        val payload = JsonObject().apply {
            // Supabase Realtime v2: postgres_changes config in join payload
            val postgresChanges = JsonArray()
            for (config in configs) {
                val changeObj = JsonObject().apply {
                    addProperty("event", config.event)
                    addProperty("schema", config.schema)
                    addProperty("table", config.table)
                    config.filter?.let { addProperty("filter", it) }
                }
                postgresChanges.add(changeObj)
            }

            add("postgres_changes", postgresChanges)
        }

        // Per-channel joinRef — only incremented on actual phx_join
        val joinRef = channelJoinRefs.getOrPut(channelName) { AtomicLong(0L) }.incrementAndGet()
        val ref = nextRef()
        pendingJoins[ref] = CompletableDeferred()

        val message = gson.toJson(arrayOf(joinRef, ref, channelName, PHOENIX_EVENT_JOIN, payload))
        val sent = ws?.send(message)
        if (sent != true) {
            Timber.w(TAG, "Failed to join channel: $channelName")
        } else {
            Timber.d(TAG, "Joined channel: $channelName with ${configs.size} postgres_changes")
        }
    }

    private fun leaveChannel(channelName: String) {
        // join_ref=0 for non-join messages per Phoenix protocol
        sendPhoenixMessage(channelName, PHOENIX_EVENT_CLOSE, joinRef = 0L)
        channelJoinRefs.remove(channelName)
        Timber.d(TAG, "Left channel: $channelName")
    }

    private fun resubscribeAll() {
        for ((channelName, configs) in activeChannels.toList()) {
            joinChannel(channelName, configs)
        }
    }

    private fun startHeartbeat() {
        heartbeatLoopJob.getAndSet(null)?.cancel()
        heartbeatLoopJob.set(scope.launch {
            while (isActive) {
                delay(30_000) // Phoenix heartbeat every 30s
                if (isConnected.get()) {
                    val ref = nextRef()
                    val message = gson.toJson(arrayOf(0, ref, "phoenix", PHOENIX_EVENT_HEARTBEAT, JsonObject()))
                    ws?.send(message)
                }
            }
        })
    }

    private fun handleDisconnect() {
        isConnected.set(false)
        _connectionState.value = ConnectionState.RECONNECTING
        heartbeatLoopJob.getAndSet(null)?.cancel()

        // Exponential backoff reconnect
        reconnectJob.getAndSet(null)?.cancel()
        val attempt = reconnectAttempt.getAndIncrement()
        reconnectJob.set(scope.launch {
            val delayMs = minOf(30_000L, (1000L * (1L shl attempt.coerceAtMost(4))))
            Timber.d(TAG, "Reconnecting in ${delayMs}ms (attempt ${attempt + 1})")
            delay(delayMs)
            if (activeChannels.isNotEmpty()) {
                connect()
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        })
    }

    private fun handlePhoenixMessage(text: String) {
        try {
            val array = JsonParser().parse(text).asJsonArray
            // [join_ref, ref, topic, event, payload]
            @Suppress("UNUSED_VARIABLE") val joinRef = array.get(0)?.asLong ?: 0
            val ref = array.get(1)?.asLong ?: 0
            val topic = array.get(2)?.asString ?: ""
            val event = array.get(3)?.asString ?: ""
            val payload = array.get(4)?.asJsonObject ?: JsonObject()

            when (event) {
                PHOENIX_EVENT_REPLY -> {
                    // Reply to a join/push — resolve pending join if any
                    pendingJoins.remove(ref)?.complete(true)
                }
                PHOENIX_EVENT_ERROR -> {
                    Timber.e(TAG, "Phoenix error on topic=$topic: $payload")
                    pendingJoins.remove(ref)?.complete(false)
                }
                PHOENIX_EVENT_CLOSE -> {
                    Timber.d(TAG, "Phoenix channel closed: $topic")
                }
                PHOENIX_EVENT_HEARTBEAT -> {
                    // Heartbeat reply — connection alive
                }
                else -> {
                    // Realtime event — parse it
                    // v2 format: event is "postgres_changes" and payload has "data" object
                    if (event == "postgres_changes" || topic.startsWith("realtime:")) {
                        parseAndEmitRealtimeEvent(topic, event, payload)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.w(TAG, "Failed to parse Phoenix message: ${e.message}")
        }
    }

    private fun parseAndEmitRealtimeEvent(topic: String, event: String, payload: JsonObject) {
        try {
            // v2 format: payload contains "data" with "record", "old_record", "type", "table"
            val data = if (payload.has("data")) {
                payload.getAsJsonObject("data")
            } else {
                payload
            }

            val eventType = data.get("type")?.asString
                ?: data.get("event_type")?.asString
                ?: event

            val tableName = data.get("table")?.asString
                ?: try {
                    topic.substringAfter("realtime:public:").substringBefore(":")
                } catch (_: Exception) {
                    topic.substringAfterLast(":")
                }

            val record = data.get("record")?.asJsonObject
                ?: data.get("new")?.asJsonObject
                ?: data.get("columns")?.asJsonObject  // v2 sometimes uses "columns"

            val oldRecord = data.get("old_record")?.asJsonObject
                ?: data.get("old")?.asJsonObject

            val realtimeEvent = RealtimeEvent(
                table = tableName,
                eventType = eventType,
                record = record,
                oldRecord = oldRecord,
                channelName = topic
            )

            scope.launch {
                _events.emit(realtimeEvent)
            }
        } catch (e: Exception) {
            Timber.w(TAG, "Failed to parse Realtime event: ${e.message}")
        }
    }

    // ─── Helpers ───

    /**
     * Build a channel name from table and filter.
     * v2 format: "public:{table}" or "public:{table}:{filter}"
     */
    private fun buildChannelName(table: String, filter: String?): String {
        return if (filter != null) {
            "public:$table:$filter"
        } else {
            "public:$table"
        }
    }
}
