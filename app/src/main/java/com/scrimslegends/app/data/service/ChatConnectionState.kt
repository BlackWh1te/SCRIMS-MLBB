package com.scrimslegends.app.data.service

/**
 * Transport-level connection state for the messaging pipeline.
 * Exposed by the repository so UI can show connectivity indicators.
 */
enum class ChatConnectionState {
    DISCONNECTED,      // No network or explicit disconnect
    CONNECTING,      // Initial WebSocket handshake
    CONNECTED,       // WebSocket open, channels joined
    RECONNECTING,    // Backoff after disconnect
    FALLBACK_POLLING // WebSocket failed, using REST polling
}
