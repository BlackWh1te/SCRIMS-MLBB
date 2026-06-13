package com.scrimslegends.app.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.scrimslegends.app.data.local.MessageEntity
import com.scrimslegends.app.data.local.ScrimsLegendsDatabase
import com.scrimslegends.app.data.service.MessageDto
import com.scrimslegends.app.data.service.SupabaseApiService
import timber.log.Timber
import java.time.Instant

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val conversationId: String,
    private val api: SupabaseApiService,
    private val database: ScrimsLegendsDatabase,
    private val mapDtoToEntity: (MessageDto) -> MessageEntity
) : RemoteMediator<Int, MessageEntity>() {

    private val messageDao = database.messageDao()

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>
    ): MediatorResult {
        return try {
            val cursorTimestamp: String? = when (loadType) {
                LoadType.REFRESH -> null
                LoadType.PREPEND -> {
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = true)
                    
                    // ISO-8601 string for Supabase filter
                    Instant.ofEpochMilli(lastItem.timestamp).toString()
                }
            }

            Timber.d("MessageRemoteMediator: Loading $loadType with cursor $cursorTimestamp")

            val response = api.getMessages(
                conversationId = "eq.$conversationId",
                order = "created_at.desc",
                limit = state.config.pageSize,
                createdAfter = cursorTimestamp?.let { "lt.$it" },
                range = null // Don't use Range header, use cursor + limit instead
            )

            if (!response.isSuccessful) {
                return MediatorResult.Error(Exception("Failed to load messages: ${response.code()}"))
            }

            val messagesDto = response.body() ?: emptyList()
            val endOfPaginationReached = messagesDto.isEmpty()

            database.withTransaction {
                val entities = messagesDto.map(mapDtoToEntity)
                messageDao.insertMessages(entities)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: Exception) {
            Timber.e(e, "MessageRemoteMediator failed to load")
            MediatorResult.Error(e)
        }
    }
}
