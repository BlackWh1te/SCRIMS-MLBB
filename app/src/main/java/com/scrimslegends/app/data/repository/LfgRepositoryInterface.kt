package com.scrimslegends.app.data.repository

import com.scrimslegends.app.data.model.LfgPost
import kotlinx.coroutines.flow.Flow

interface LfgRepositoryInterface {
    fun getAllPosts(): Flow<Result<List<LfgPost>>>
    fun createPost(post: LfgPost): Flow<Result<LfgPost>>
    fun deletePost(postId: String): Flow<Result<Unit>>
    fun getPostsByPlayer(playerId: String): Flow<Result<List<LfgPost>>>
    fun subscribeToLfgPosts(): Flow<LfgPost>
    suspend fun incrementViewCount(postId: String): Result<Unit>
}
