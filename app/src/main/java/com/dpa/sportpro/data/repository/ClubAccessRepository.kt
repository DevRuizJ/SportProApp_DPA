package com.dpa.sportpro.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

data class ClubRecord(val id: String, val name: String, val description: String)
data class AccessRequest(
    val id: String, val uid: String, val clubId: String, val clubName: String,
    val name: String, val email: String, val role: String, val status: String,
    val createdAt: Date?, val reason: String,
)
data class ClubPlayer(val id: String, val clubId: String, val name: String, val category: String)
data class FamilyLink(
    val id: String, val parentUid: String, val clubId: String, val playerId: String,
    val playerName: String, val clubName: String, val category: String,
    val parentName: String, val status: String, val reason: String,
)
data class FamilyInvitation(
    val code: String, val clubId: String, val playerId: String, val playerName: String,
    val clubName: String, val category: String,
)
data class AccessPage<T>(val items: List<T>, val cursor: DocumentSnapshot?, val hasMore: Boolean)

/** Firestore rules are the authority. No selected role or cached UI value authorizes writes. */
class ClubAccessRepository {
    private val db get() = SportProFirestore.instance
    private val uid get() = checkNotNull(FirebaseAuth.getInstance().currentUser).uid
    private val requests get() = db.collection("clubAccess")
    private val links get() = db.collection("familyLinks")
    private fun now() = FieldValue.serverTimestamp()
    private fun profile() = checkNotNull(AuthSession.profile.value)
    private fun fullName() = profile().let { "${it.names} ${it.lastNames}".trim() }

    suspend fun platformAuthority(): Boolean {
        val claims = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()?.claims.orEmpty()
        if (claims["platform_admin"] == true) return true
        // This document is provisioned once by the Firebase project owner; Android cannot write it.
        return db.collection("platformAuthorities").document(uid)
            .get(com.google.firebase.firestore.Source.SERVER).await().getBoolean("active") == true
    }

    fun ownAccess() = callbackFlow {
        val registration = requests.whereEqualTo("uid", uid).limit(100)
            .addSnapshotListener { data, error ->
                if (error != null) close(error)
                else if (data != null) trySend(data.documents.map(::access))
            }
        awaitClose { registration.remove() }
    }

    fun myChildren() = callbackFlow {
        val registration = links.whereEqualTo("parentUid", uid).limit(100)
            .addSnapshotListener { data, error ->
                if (error != null) close(error)
                else if (data != null) trySend(data.documents.map(::family))
            }
        awaitClose { registration.remove() }
    }

    private suspend fun <T> page(query: Query, cursor: DocumentSnapshot?, map: (DocumentSnapshot) -> T): AccessPage<T> {
        val ordered = query.orderBy(FieldPath.documentId())
        val result = (if (cursor == null) ordered else ordered.startAfter(cursor)).limit(50).get().await()
        return AccessPage(result.documents.map(map), result.documents.lastOrNull(), result.size() == 50)
    }

    suspend fun clubs(cursor: DocumentSnapshot? = null) =
        page(db.collection("clubs"), cursor) { ClubRecord(it.id, it.getString("name").orEmpty(), it.getString("description").orEmpty()) }

    suspend fun reviewRequests(club: String, cursor: DocumentSnapshot? = null) =
        page(requests.whereEqualTo("clubId", club), cursor, ::access)

    suspend fun reviewFamilies(club: String, cursor: DocumentSnapshot? = null) =
        page(links.whereEqualTo("clubId", club), cursor, ::family)

    suspend fun players(club: String, cursor: DocumentSnapshot? = null) =
        page(db.collection("clubPlayers").whereEqualTo("clubId", club), cursor) {
            ClubPlayer(it.id, club, it.getString("name").orEmpty(), it.getString("category").orEmpty())
        }

    suspend fun createClub(name: String, description: String) {
        require(name.trim().length in 1..80 && description.length <= 500)
        val ref = db.collection("clubs").document()
        db.runTransaction { tx ->
            tx.set(ref, mapOf("name" to name.trim(), "description" to description.trim(),
                "createdBy" to uid, "createdAt" to now()))
        }.await()
    }

    suspend fun requestRole(club: ClubRecord, role: String) {
        require(role in listOf("ADM", "DT", "JUG", "PAD"))
        val ref = requests.document("${club.id}_${uid}")
        val user = profile()
        db.runTransaction { tx ->
            check(!tx.get(ref).exists()) { "Ya existe una solicitud para este club. Consulta su estado." }
            tx.set(ref, mapOf("uid" to uid, "clubId" to club.id, "clubName" to club.name,
                "name" to fullName(), "email" to user.email, "role" to role,
                "status" to "PENDING", "createdAt" to now(), "decidedAt" to null,
                "decidedBy" to "", "reason" to ""))
        }.await()
    }

    suspend fun decideAccess(item: AccessRequest, status: String, reason: String) {
        require(status in listOf("APPROVED", "REJECTED", "REVOKED"))
        require(uid != item.uid) { "No puedes aprobar o modificar tus propios permisos." }
        require(reason.trim().length in 1..500)
        val ref = requests.document(item.id)
        db.runTransaction { tx ->
            check(tx.get(ref).getString("status") == item.status) { "La solicitud cambió. Actualiza la lista." }
            tx.update(ref, mapOf("status" to status, "decidedAt" to now(),
                "decidedBy" to uid, "reason" to reason.trim()))
        }.await()
    }

    suspend fun addPlayer(club: ClubRecord, name: String, category: String) {
        require(name.trim().length in 1..100 && category.trim().length in 1..80)
        // A sports record is not an Authentication account and grants no login permission.
        val ref = db.collection("clubPlayers").document()
        db.runTransaction { tx ->
            tx.set(ref, mapOf("clubId" to club.id, "name" to name.trim(),
                "category" to category.trim(), "createdBy" to uid, "createdAt" to now()))
        }.await()
    }

    suspend fun inviteFamily(club: ClubRecord, player: ClubPlayer): String {
        val code = UUID.randomUUID().toString().replace("-", "")
        val ref = db.collection("familyInvitations").document(code)
        db.runTransaction { tx ->
            tx.set(ref, mapOf("clubId" to club.id, "clubName" to club.name,
                "playerId" to player.id, "playerName" to player.name, "category" to player.category,
                "createdBy" to uid, "createdAt" to now(),
                "expiresAt" to Timestamp(Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000)),
                "consumedBy" to ""))
        }.await()
        return code
    }

    suspend fun inspectInvitation(raw: String): FamilyInvitation {
        val code = raw.trim().lowercase()
        require(code.matches(Regex("[a-f0-9]{32}"))) { "Código de invitación inválido." }
        val doc = db.collection("familyInvitations").document(code).get().await()
        check(doc.exists() && doc.getString("consumedBy") == "") { "Invitación no disponible." }
        check((doc.getTimestamp("expiresAt")?.toDate()?.time ?: 0) > System.currentTimeMillis()) {
            "La invitación caducó. Solicita una nueva al club."
        }
        return FamilyInvitation(code, doc.getString("clubId").orEmpty(),
            doc.getString("playerId").orEmpty(), doc.getString("playerName").orEmpty(),
            doc.getString("clubName").orEmpty(), doc.getString("category").orEmpty())
    }

    suspend fun claimInvitation(invite: FamilyInvitation) {
        val invitation = db.collection("familyInvitations").document(invite.code)
        val link = links.document("${invite.playerId}_${uid}")
        db.runTransaction { tx ->
            val current = tx.get(invitation)
            val previous = tx.get(link)
            check(current.getString("consumedBy") == "" && !previous.exists()) {
                "Invitación utilizada o vínculo ya solicitado."
            }
            tx.update(invitation, "consumedBy", uid)
            tx.set(link, mapOf("parentUid" to uid, "parentName" to fullName(),
                "clubId" to invite.clubId, "clubName" to invite.clubName,
                "playerId" to invite.playerId, "playerName" to invite.playerName,
                "category" to invite.category, "invitationId" to invite.code,
                "status" to "PENDING", "createdAt" to now(), "decidedAt" to null,
                "decidedBy" to "", "reason" to ""))
        }.await()
    }

    suspend fun decideFamily(item: FamilyLink, status: String, reason: String) {
        require(status in listOf("APPROVED", "REJECTED", "REVOKED"))
        require(reason.trim().length in 1..500)
        val ref = links.document(item.id)
        db.runTransaction { tx ->
            check(tx.get(ref).getString("status") == item.status) { "El vínculo cambió. Actualiza la lista." }
            tx.update(ref, mapOf("status" to status, "decidedAt" to now(),
                "decidedBy" to uid, "reason" to reason.trim()))
        }.await()
    }

    suspend fun childProfile(item: FamilyLink): ClubPlayer {
        val doc = db.collection("clubPlayers").document(item.playerId).get(com.google.firebase.firestore.Source.SERVER).await()
        return ClubPlayer(doc.id, doc.getString("clubId").orEmpty(), doc.getString("name").orEmpty(),
            doc.getString("category").orEmpty())
    }

    private fun access(d: DocumentSnapshot) = AccessRequest(d.id, d.getString("uid").orEmpty(),
        d.getString("clubId").orEmpty(), d.getString("clubName").orEmpty(), d.getString("name").orEmpty(),
        d.getString("email").orEmpty(), d.getString("role").orEmpty(), d.getString("status").orEmpty(),
        d.getTimestamp("createdAt")?.toDate(), d.getString("reason").orEmpty())
    private fun family(d: DocumentSnapshot) = FamilyLink(d.id, d.getString("parentUid").orEmpty(),
        d.getString("clubId").orEmpty(), d.getString("playerId").orEmpty(), d.getString("playerName").orEmpty(),
        d.getString("clubName").orEmpty(), d.getString("category").orEmpty(), d.getString("parentName").orEmpty(),
        d.getString("status").orEmpty(), d.getString("reason").orEmpty())
}
