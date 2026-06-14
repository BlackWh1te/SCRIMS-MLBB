package com.scrimslegends.app.data.model

import androidx.compose.runtime.Stable

/**
 * Message delivery state machine.
 *
 * PENDING   = in outbox, not yet sent to server
 * SENDING   = network request in flight
 * SENT      = server returned 200 with message body (has server id)
 * DELIVERED = recipient received via Realtime (detected via read receipt or presence)
 * FAILED    = permanent failure (max retries exceeded, validation error, rate limit)
 * CANCELLED = user explicitly cancelled
 */
enum class DeliveryStatus {
    PENDING,
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
    CANCELLED
}

/**
 * Local-only message wrapper that tracks delivery state.
 * Used by the UI to render sent/pending/failed indicators.
 */
@Stable
data class MessageWithDelivery(
    val message: Message,
    val status: DeliveryStatus = DeliveryStatus.SENT,
    val clientMessageId: String? = null,   // local idempotency key
    val retryCount: Int = 0,
    val failedAt: Long? = null,
    val errorReason: String? = null
) {
    val isPending: Boolean get() = status == DeliveryStatus.PENDING || status == DeliveryStatus.SENDING
    val isFailed: Boolean get() = status == DeliveryStatus.FAILED || status == DeliveryStatus.CANCELLED
}
