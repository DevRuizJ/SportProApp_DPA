package com.dpa.sportpro.data.local

import com.dpa.sportpro.domain.match.EventPayload
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.util.UUID

/** Local capture only. A future sender must authenticate ownerUid before replaying commands. */
class MatchEventOutbox(private val dao: PendingMatchEventDao) {
    suspend fun enqueue(
        ownerUid: String,
        matchId: String,
        payload: EventPayload,
        operationId: String = UUID.randomUUID().toString(),
    ): String {
        require(ownerUid.isNotBlank() && matchId.isNotBlank() && operationId.isNotBlank())
        val json = JSONObject().apply {
            put("payloadVersion", 1)
            put("command", "RECORD_EVENT")
            put("typeId", payload.typeId)
            put("elapsedSeconds", payload.elapsedSeconds)
            put("side", payload.side?.name ?: JSONObject.NULL)
            put("playerId", payload.playerId ?: JSONObject.NULL)
            put("observations", payload.observations)
            put("fields", JSONObject(payload.fields))
        }
        dao.insert(PendingMatchEvent(operationId, ownerUid, matchId, json.toString(), System.currentTimeMillis()))
        return operationId
    }

    fun observePending(ownerUid: String, matchId: String): Flow<List<PendingMatchEvent>> {
        require(ownerUid.isNotBlank() && matchId.isNotBlank())
        return dao.observe(ownerUid, matchId)
    }
}
