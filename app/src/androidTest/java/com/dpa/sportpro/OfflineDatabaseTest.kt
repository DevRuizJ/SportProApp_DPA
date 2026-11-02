package com.dpa.sportpro

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dpa.sportpro.data.local.MatchEventOutbox
import com.dpa.sportpro.data.local.PendingMatchEvent
import com.dpa.sportpro.data.local.SportProDatabase
import com.dpa.sportpro.domain.match.EventPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OfflineDatabaseTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private inline fun withDatabase(name: String, block: (SportProDatabase) -> Unit) {
        val db = SportProDatabase.builder(context, name).build()
        try {
            block(db)
        } finally {
            db.close()
        }
    }

    @Test
    fun migrationPreservesPendingEventAndAddsDefaults() = runBlocking {
        val name = "migration-${UUID.randomUUID()}"
        try {
            // Use the exported historical schema, then open with the production builder.
            // Room itself executes the registered migration and validates the resulting schema.
            val schema = instrumentation.context.assets.open(
                "com.dpa.sportpro.data.local.SportProDatabase/1.json",
            ).bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            val file = context.getDatabasePath(name)
            file.parentFile?.mkdirs()
            SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
                val setup = schema.getJSONArray("setupQueries")
                for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
                db.version = 1
                db.execSQL(
                    "INSERT INTO pending_match_events (operationId, ownerUid, matchId, payloadJson, createdAtMillis) VALUES (?, ?, ?, ?, ?)",
                    arrayOf<Any>("event-1", "dt-1", "match-1", "{\"elapsedSeconds\":91}", 1234L),
                )
            }
            withDatabase(name) { db ->
                val row = db.pendingEvents().observe("dt-1", "match-1").first().single()
                assertEquals(
                    PendingMatchEvent("event-1", "dt-1", "match-1", "{\"elapsedSeconds\":91}", 1234L),
                    row,
                )
                assertEquals(2, db.openHelper.readableDatabase.version)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun pendingEventsSurviveReopenAndAreScopedToOwner() = runBlocking {
        val name = "outbox-${UUID.randomUUID()}"
        try {
            withDatabase(name) { db ->
                MatchEventOutbox(db.pendingEvents()).enqueue(
                    "dt-1", "match-1", EventPayload(typeId = "start", elapsedSeconds = 0), "event-1",
                )
                db.pendingEvents().recordFailure("dt-1", "event-1", "offline")
            }
            withDatabase(name) { db ->
                val dao = db.pendingEvents()
                val pending = dao.observe("dt-1", "match-1").first().single()
                assertEquals(1, pending.attemptCount)
                assertEquals("offline", pending.lastError)
                assertEquals(1, JSONObject(pending.payloadJson).getInt("payloadVersion"))
                assertTrue(JSONObject(pending.payloadJson).isNull("playerId"))
                assertTrue(dao.observe("pad-1", "match-1").first().isEmpty())
                assertNull(dao.get("pad-1", "event-1"))
                assertEquals(0, dao.confirm("pad-1", "event-1", 50L))
                assertEquals(1, dao.confirm("dt-1", "event-1", 50L))
                assertTrue(dao.observe("dt-1", "match-1").first().isEmpty())
                assertEquals(50L, dao.get("dt-1", "event-1")?.confirmedAtMillis)
                assertEquals(0, dao.recordFailure("dt-1", "event-1", "late failure"))
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun duplicateOperationCannotOverwriteUnsentData() = runBlocking {
        val name = "duplicate-${UUID.randomUUID()}"
        try {
            withDatabase(name) { db ->
                val dao = db.pendingEvents()
                val original = PendingMatchEvent("event-1", "dt-1", "match-1", "original", 1L)
                dao.insert(original)
                try {
                    dao.insert(original.copy(payloadJson = "changed"))
                    fail("Duplicate operation must be rejected")
                } catch (_: SQLiteConstraintException) {
                    assertEquals(original, dao.get("dt-1", "event-1"))
                }
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
