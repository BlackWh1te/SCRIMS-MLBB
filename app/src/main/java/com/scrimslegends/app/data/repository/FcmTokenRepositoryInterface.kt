package com.scrimslegends.app.data.repository

interface FcmTokenRepositoryInterface {
    suspend fun registerToken(token: String): Result<Unit>
    suspend fun deleteToken(token: String): Result<Unit>
}
