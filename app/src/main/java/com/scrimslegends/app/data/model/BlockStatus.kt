package com.scrimslegends.app.data.model

data class BlockStatus(
    val isBlockedByCurrentUser: Boolean = false,
    val isBlockedByOtherUser: Boolean = false
) {
    val isBlocked: Boolean
        get() = isBlockedByCurrentUser || isBlockedByOtherUser
}
