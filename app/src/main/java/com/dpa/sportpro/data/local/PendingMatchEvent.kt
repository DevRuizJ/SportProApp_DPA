package com.dpa.sportpro.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Durable local command. The ID must also be used remotely to avoid duplicate events. */
@Entity(tableName = "pending_match_events")
data class PendingMatchEvent(
    @PrimaryKey val operationId: String,
    val ownerUid: String,
    val matchId: String,
    val payloadJson: String,
    val createdAtMillis: Long,
    @ColumnInfo(defaultValue = "0") val attemptCount: Int = 0,
    val lastError: String? = null,
    val confirmedAtMillis: Long? = null,
)
