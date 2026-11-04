package com.dpa.sportpro.data.repository

import android.content.Context
import android.os.SystemClock
import com.dpa.sportpro.data.local.PendingMatchEvent
import com.dpa.sportpro.data.local.SportProDatabase
import com.dpa.sportpro.domain.match.*
import com.dpa.sportpro.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

data class RemoteCatalog(val enabled: Set<String> = LiveEventTypes.labels.keys, val version: Long = 1, val fromCache: Boolean = true)

/** Firestore owns confirmed state; Room owns unsent commands, never an independent scoreboard. */
class LiveMatchRepository(context: Context) {
    private val appContext = context.applicationContext
    private val calibrationPrefs get() = LocalAccountScope.preferences(appContext, "match_clock_anchor")
    private fun bootCount() = android.provider.Settings.Global.getInt(appContext.contentResolver, android.provider.Settings.Global.BOOT_COUNT, -1)
    fun cachedClockAnchor(): Pair<Long, Long>? {
        val p = calibrationPrefs
        val elapsed = p.getLong("elapsed", -1)
        return if (p.getInt("boot", -2) == bootCount() && bootCount() >= 0 && elapsed >= 0 && SystemClock.elapsedRealtime() >= elapsed)
            p.getLong("server", 0) to elapsed else null
    }
    private val db get() = SportProFirestore.instance
    private val dao = SportProDatabase.getInstance(context.applicationContext).pendingEvents()
    private val matches get() = db.collection("matches")
    private fun uid() = requireNotNull(FirebaseAuth.getInstance().currentUser?.uid) { "Inicia sesión." }
    private fun requireOperatorRole() {
        require(AuthSession.profile.value?.authorizedRole in setOf(UserRole.ADMIN, UserRole.COACH)) {
            "Tu rol debe ser aprobado por un administrador."
        }
    }

    fun observeMatches() = callbackFlow {
        val listener = matches.whereArrayContains("viewerUids", uid()).addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
            if (error != null) close(error)
            else try { trySend(value!!.documents.map { it.toMatch() }) } catch (e: Exception) { close(e) }
        }
        awaitClose { listener.remove() }
    }
    fun observeMatch(id: String) = callbackFlow {
        val listener = matches.document(id).addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
            if (error != null) close(error)
            else try { trySend(value?.takeIf { it.exists() }?.toMatch()) } catch (e: Exception) { close(e) }
        }
        awaitClose { listener.remove() }
    }
    fun observeEvents(id: String) = callbackFlow {
        val listener = matches.document(id).collection("events").addSnapshotListener { value, error ->
            if (error != null) close(error)
            else try { trySend(value!!.documents.map { it.toEvent() }.sortedWith(
                compareByDescending<LiveEvent> { it.seconds }.thenBy { it.id })) } catch (e: Exception) { close(e) }
        }
        awaitClose { listener.remove() }
    }
    fun pending(id: String) = dao.observe(uid(), id)
    fun catalogKey(owner: String, category: String): String = owner + "_" +
        MessageDigest.getInstance("SHA-256").digest(category.trim().lowercase(java.util.Locale.ROOT).toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }
    fun observeCatalog(owner: String, category: String) = callbackFlow {
        val listener = db.collection("matchCatalogs").document(catalogKey(owner, category))
            .addSnapshotListener(MetadataChanges.INCLUDE) { value, error ->
                if (error != null) close(error)
                else trySend(RemoteCatalog((value?.get("enabled") as? List<*>)?.filterIsInstance<String>()?.toSet()
                    ?: emptySet(), value?.getLong("version") ?: 0, value?.metadata?.isFromCache ?: true))
            }
        awaitClose { listener.remove() }
    }

    suspend fun create(home: String, away: String, category: String, minutes: Int, viewers: List<String>, operators: List<String>, requestId: String): String {
        requireOperatorRole()
        require(home.trim().length in 1..80 && away.trim().length in 1..80 && category.trim().length in 1..80) { "Completa equipos y categoría (máximo 80 caracteres)." }
        require(minutes in 5..60) { "La duración por periodo debe estar entre 5 y 60 minutos." }
        val owner = uid()
        val ops = (operators + owner).distinct()
        val readers = (viewers + ops).distinct()
        require(readers.size <= 100 && ops.size <= 10 && readers.all { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }) { "Revisa los UID y el número de participantes." }
        require(requestId.matches(Regex("[A-Za-z0-9_-]{1,128}")))
        val reference = matches.document(requestId)
        val catalog = db.collection("matchCatalogs").document(catalogKey(owner, category))
        withTimeout(20_000) {
            db.runTransaction { tx ->
                val existing = tx.get(reference)
                val config = tx.get(catalog)
                val categoryLabel = config.getString("category") ?: category.trim()
                if (existing.exists()) {
                    require(existing.getString("ownerUid") == owner && existing.getString("home") == home.trim() &&
                        existing.getString("away") == away.trim() && existing.getString("category") == categoryLabel &&
                        existing.getLong("periodMinutes") == minutes.toLong() &&
                        (existing.get("viewerUids") as? List<*>)?.toSet() == readers.toSet() &&
                        (existing.get("operatorUids") as? List<*>)?.toSet() == ops.toSet()) {
                        "La solicitud anterior ya fue guardada. Cierra el formulario y consulta tus partidos."
                    }
                    return@runTransaction
                }
                if (!config.exists()) tx.set(catalog, mapOf("ownerUid" to owner, "category" to categoryLabel,
                    "enabled" to LiveEventTypes.labels.keys.toList(), "version" to 1L, "updatedAt" to FieldValue.serverTimestamp()))
                tx.set(reference, mapOf("ownerUid" to owner, "home" to home.trim(), "away" to away.trim(),
                    "category" to categoryLabel, "catalogId" to catalog.id, "periodMinutes" to minutes,
                    "viewerUids" to readers, "operatorUids" to ops, "phase" to "READY", "accumulatedSeconds" to 0L,
                    "startedAt" to null, "clockVersion" to 0L, "lastClockEvent" to "",
                    "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            }.await()
        }
        return reference.id
    }

    suspend fun setCatalog(match: LiveMatch, enabled: Set<String>, expectedVersion: Long) {
        requireOperatorRole()
        require(match.ownerUid == uid()) { "Solo el responsable configura el catálogo." }
        require(enabled.all { it in LiveEventTypes.labels } && enabled.containsAll(LiveEventTypes.periodTypes))
        val ref = db.collection("matchCatalogs").document(catalogKey(match.ownerUid, match.category))
        withTimeout(20_000) { db.runTransaction { tx ->
            val current = tx.get(ref)
            require(current.getLong("version") == expectedVersion) { "El catálogo cambió. Revisa los cambios." }
            tx.update(ref, mapOf("enabled" to enabled.toList(), "version" to expectedVersion + 1, "updatedAt" to FieldValue.serverTimestamp()))
        }.await() }
    }

    suspend fun calibrateClock(): Pair<Long, Long> {
        val before = SystemClock.elapsedRealtime()
        val ref = db.collection("clockProbes").document(uid())
        ref.set(mapOf("at" to FieldValue.serverTimestamp())).await()
        val server = requireNotNull(ref.get(Source.SERVER).await().getTimestamp("at")).toDate().time
        val after = SystemClock.elapsedRealtime()
        // Bounded by request round trip; never derives the running clock from the handset wall clock.
        val estimated = server + (after - before) / 2
        calibrationPrefs.edit().putLong("server", estimated).putLong("elapsed", after).putInt("boot", bootCount()).apply()
        return estimated to after
    }

    suspend fun clock(match: LiveMatch, action: ClockAction, serverNow: Long) {
        requireOperatorRole()
        val actor = uid()
        require(actor in match.operatorUids)
        val next = match.clock.transition(action, serverNow)
        val eventType = when (action) {
            ClockAction.START -> "period_start"
            ClockAction.END_PERIOD, ClockAction.FINISH -> "period_end"
            else -> null
        }
        val ref = matches.document(match.id)
        val clockEvent = if (eventType != null) "clock_${next.version}" else ""
        withTimeout(20_000) {
            db.runTransaction { tx ->
                val current = tx.get(ref)
                require(current.getLong("clockVersion") == match.clock.version) { "Otro operador cambió el reloj. Revisa su estado." }
                tx.update(ref, mapOf("phase" to next.phase.name, "accumulatedSeconds" to next.accumulatedSeconds,
                    "startedAt" to if (next.phase.running) FieldValue.serverTimestamp() else null,
                    "clockVersion" to next.version, "lastClockEvent" to clockEvent, "updatedAt" to FieldValue.serverTimestamp()))
                if (eventType != null) {
                    val event = eventData(clockEvent, clockEvent, 1, eventType, next.accumulatedSeconds.toInt(),
                        "", "", "", if (next.phase in setOf(ClockPhase.FIRST, ClockPhase.HALF_TIME)) "1" else "2",
                        "", false, actor, "", 0, System.currentTimeMillis())
                    val er = ref.collection("events").document(clockEvent)
                    tx.set(er, event)
                    tx.set(er.collection("history").document(clockEvent), event)
                }
            }.await()
        }
    }

    suspend fun enqueue(match: LiveMatch, type: String, seconds: Int, side: String, player: String,
        other: String, detail: String, observations: String, catalogVersion: Long,
        existing: LiveEvent? = null, voided: Boolean = false, reason: String = ""): String {
        if (AuthSession.profile.value?.offlineSession != true) requireOperatorRole()
        val actor = uid()
        require(actor in match.operatorUids) { "No eres operador de este partido." }
        LiveEventTypes.validate(type, seconds, side, player.trim(), other.trim(), detail.trim())
        require(observations.length <= 2000 && reason.length <= 500)
        require(existing == null || (!existing.voided && reason.isNotBlank())) { "Indica motivo; un evento anulado no puede editarse." }
        require(existing != null || match.clock.phase != ClockPhase.READY) { "Inicia el partido antes de registrar eventos." }
        val operation = UUID.randomUUID().toString()
        val payload = JSONObject().apply {
            put("payloadVersion", 2); put("eventId", existing?.id ?: operation)
            put("expectedVersion", existing?.version ?: 0); put("typeId", type); put("seconds", seconds)
            put("side", side); put("player", player.trim()); put("otherPlayer", other.trim()); put("detail", detail.trim())
            put("observations", observations.trim()); put("voided", voided); put("reason", reason.trim())
            put("catalogVersion", catalogVersion)
        }
        dao.insert(PendingMatchEvent(operation, actor, match.id, payload.toString(), System.currentTimeMillis()))
        return operation
    }

    suspend fun flushPending(owner: String) = syncMutex.withLock {
        if (uid() != owner) return@withLock
        for (row in dao.unsent(owner)) {
            if (FirebaseAuth.getInstance().currentUser?.uid != owner) return@withLock
            try {
                withTimeout(15_000) { send(row) }
                dao.confirm(owner, row.operationId, System.currentTimeMillis())
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                val permanent = e is IllegalArgumentException ||
                    e is FirebaseFirestoreException && e.code in setOf(FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.NOT_FOUND)
                if (permanent) dao.reject(owner, row.operationId, System.currentTimeMillis(),
                    if (e is IllegalArgumentException) e.message ?: "Revisa el evento." else "Permiso rechazado. Revisa rol, acceso o catálogo; el registro local se conserva.")
                else {
                    dao.recordFailure(owner, row.operationId, "Pendiente de conexión/confirmación. Se reintentará con el mismo ID.")
                    return@withLock
                }
            }
        }
    }

    private suspend fun send(row: PendingMatchEvent) {
        val p = JSONObject(row.payloadJson)
        require(p.optInt("payloadVersion") == 2) { "Registro anterior conservado. Revisa su contenido antes de reenviarlo con el nuevo formato." }
        val eventRef = matches.document(row.matchId).collection("events").document(p.getString("eventId"))
        val historyRef = eventRef.collection("history").document(row.operationId)
        db.runTransaction { tx ->
            if (tx.get(historyRef).exists()) return@runTransaction
            val current = tx.get(eventRef)
            val expected = p.getLong("expectedVersion")
            require((current.getLong("version") ?: 0L) == expected) { "Conflicto: el evento fue modificado por otro operador. Abre la última versión y registra una nueva corrección." }
            require(current.getBoolean("voided") != true) { "El evento ya fue anulado." }
            val data = eventData(eventRef.id, row.operationId, expected + 1, p.getString("typeId"), p.getInt("seconds"),
                p.getString("side"), p.getString("player"), p.getString("otherPlayer"), p.getString("detail"),
                p.getString("observations"), p.getBoolean("voided"), row.ownerUid, p.getString("reason"),
                p.getLong("catalogVersion"), row.createdAtMillis)
            tx.set(eventRef, data)
            tx.set(historyRef, data)
        }.await()
    }

    suspend fun history(matchId: String, eventId: String): List<LiveEvent> =
        matches.document(matchId).collection("events").document(eventId).collection("history")
            .get(Source.SERVER).await().documents.map { it.toEvent() }.sortedBy { it.version }

    private fun DocumentSnapshot.toMatch() = LiveMatch(id, getString("ownerUid")!!, getString("home")!!,
        getString("away")!!, getString("category")!!, getLong("periodMinutes")!!.toInt(),
        (get("viewerUids") as List<*>).filterIsInstance<String>(), (get("operatorUids") as List<*>).filterIsInstance<String>(),
        MatchClock(ClockPhase.valueOf(getString("phase")!!), getLong("accumulatedSeconds")!!,
            getTimestamp("startedAt")?.toDate()?.time, getLong("clockVersion")!!), metadata.isFromCache)

    private fun DocumentSnapshot.toEvent() = LiveEvent(getString("eventId")!!, getString("operationId")!!,
        getLong("version")!!, getString("typeId")!!, getLong("seconds")!!.toInt(), getString("side")!!,
        getString("player")!!, getString("otherPlayer")!!, getString("detail")!!, getString("observations")!!,
        getBoolean("voided")!!, getString("actorUid")!!, getString("reason")!!, getTimestamp("recordedAt")?.toDate()?.time,
        getLong("catalogVersion")!!)

    private fun eventData(eventId: String, operation: String, version: Long, type: String, seconds: Int,
        side: String, player: String, other: String, detail: String, observations: String, voided: Boolean,
        actor: String, reason: String, catalogVersion: Long, capturedAt: Long): Map<String, Any> = mapOf(
        "eventId" to eventId, "operationId" to operation, "version" to version, "typeId" to type,
        "seconds" to seconds, "side" to side, "player" to player, "otherPlayer" to other, "detail" to detail,
        "observations" to observations, "voided" to voided, "actorUid" to actor, "reason" to reason,
        "recordedAt" to FieldValue.serverTimestamp(), "catalogVersion" to catalogVersion, "capturedAtMillis" to capturedAt)

    companion object { private val syncMutex = Mutex() }
}
