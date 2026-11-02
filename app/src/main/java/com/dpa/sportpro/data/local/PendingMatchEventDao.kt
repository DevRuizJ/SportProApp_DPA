package com.dpa.sportpro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMatchEventDao {
    // ABORT on duplicate ID: never silently overwrite an unsent event.
    @Insert
    suspend fun insert(event: PendingMatchEvent)

    @Query("SELECT * FROM pending_match_events WHERE ownerUid = :ownerUid AND matchId = :matchId AND confirmedAtMillis IS NULL ORDER BY createdAtMillis, operationId")
    fun observe(ownerUid: String, matchId: String): Flow<List<PendingMatchEvent>>

    @Query("SELECT * FROM pending_match_events WHERE ownerUid = :ownerUid AND operationId = :operationId")
    suspend fun get(ownerUid: String, operationId: String): PendingMatchEvent?

    /** Call only after the backend acknowledges this operation. Retains the local record. */
    @Query("UPDATE pending_match_events SET confirmedAtMillis = :confirmedAtMillis, lastError = NULL WHERE ownerUid = :ownerUid AND operationId = :operationId AND confirmedAtMillis IS NULL")
    suspend fun confirm(ownerUid: String, operationId: String, confirmedAtMillis: Long): Int

    @Query("UPDATE pending_match_events SET attemptCount = attemptCount + 1, lastError = :error WHERE ownerUid = :ownerUid AND operationId = :operationId AND confirmedAtMillis IS NULL")
    suspend fun recordFailure(ownerUid: String, operationId: String, error: String): Int
}
