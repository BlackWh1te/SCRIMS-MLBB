package com.mlbb.scrim.data.model

import java.util.UUID

data class Profile(
    val id: UUID,
    val username: String,
    val email: String,
    val mlbbId: String? = null,
    val isAdmin: Boolean = false,
    val createdAt: String
)