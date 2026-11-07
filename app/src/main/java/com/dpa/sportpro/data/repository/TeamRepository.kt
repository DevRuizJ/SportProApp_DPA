package com.dpa.sportpro.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

data class SportsTeam(val id: String, val clubId: String, val name: String, val category: String, val coachUid: String, val coachName: String)
data class TeamCoach(val uid: String, val name: String)
data class RosterPlayer(val id: String, val name: String, val active: Boolean)

/** Club sports records are separate from Auth identities. Joining a roster never creates an account. */
class TeamRepository {
    private val db get() = SportProFirestore.instance
    private val uid get() = checkNotNull(FirebaseAuth.getInstance().currentUser).uid
    suspend fun teams(clubId: String, admin: Boolean): List<SportsTeam> {
        val q = db.collection("sportsTeams").whereEqualTo("clubId", clubId)
        return (if(admin) q else q.whereEqualTo("coachUid", uid)).limit(100).get().await().documents.map {
            SportsTeam(it.id, clubId, it.getString("name").orEmpty(), it.getString("category").orEmpty(),
                it.getString("coachUid").orEmpty(), it.getString("coachName").orEmpty())
        }
    }
    suspend fun coaches(clubId: String): List<TeamCoach> =
        db.collection("clubAccess").whereEqualTo("clubId", clubId).whereEqualTo("role", "DT")
            .whereEqualTo("status", "APPROVED").limit(100).get().await().documents.map {
                TeamCoach(it.getString("uid").orEmpty(), it.getString("name").orEmpty())
            }

    suspend fun create(clubId: String, name: String, category: String, coach: TeamCoach) {
        require(name.trim().length in 1..80 && category.trim().length in 1..80)
        val ref = db.collection("sportsTeams").document()
        db.runTransaction { tx ->
            tx.set(ref, mapOf("clubId" to clubId, "name" to name.trim(), "category" to category.trim(),
                "coachUid" to coach.uid, "coachName" to coach.name, "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
        }.await()
    }
    suspend fun assignCoach(team: SportsTeam, coach: TeamCoach) {
        db.collection("sportsTeams").document(team.id).update(mapOf(
            "coachUid" to coach.uid, "coachName" to coach.name, "updatedAt" to FieldValue.serverTimestamp())).await()
    }
    suspend fun roster(team: SportsTeam): List<RosterPlayer> =
        db.collection("sportsTeams").document(team.id).collection("roster").limit(100).get().await().documents.map {
            RosterPlayer(it.id, it.getString("name").orEmpty(), it.getBoolean("active") == true)
        }

    suspend fun setPlayer(team: SportsTeam, playerId: String, active: Boolean) {
        val ref = db.collection("sportsTeams").document(team.id).collection("roster").document(playerId)
        db.runTransaction { tx ->
            val existing = tx.get(ref)
            if(existing.exists()) {
                tx.update(ref, mapOf("active" to active, "updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp()))
            } else {
                val player = tx.get(db.collection("clubPlayers").document(playerId))
                check(player.getString("clubId") == team.clubId) { "El jugador no pertenece a este club." }
                tx.set(ref, mapOf("name" to player.getString("name"), "active" to active,
                    "createdAt" to FieldValue.serverTimestamp(), "updatedBy" to uid, "updatedAt" to FieldValue.serverTimestamp()))
            }
        }.await()
    }
}
