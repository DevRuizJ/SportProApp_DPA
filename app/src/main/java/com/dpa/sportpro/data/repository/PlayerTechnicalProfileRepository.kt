package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.PhysicalMeasurement
import com.dpa.sportpro.data.model.PlayerTechnicalProfile
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

class PlayerTechnicalProfileRepository(context: Context) {
    private val preferences = LocalAccountScope.preferences(context, PREFERENCES_NAME)

    var profile by mutableStateOf(loadProfile())
        private set

    fun updateProfile(updated: PlayerTechnicalProfile) {
        val physicalDataChanged =
            updated.heightMeters != profile.heightMeters ||
                updated.weightKilograms != profile.weightKilograms
        profile =
            updated.copy(
                physicalHistory =
                    if (physicalDataChanged) {
                        updated.physicalHistory +
                            PhysicalMeasurement(
                                heightMeters = updated.heightMeters,
                                weightKilograms = updated.weightKilograms,
                                recordedAt = System.currentTimeMillis(),
                            )
                    } else {
                        updated.physicalHistory
                    }
            )
        persistProfile()
    }

    private fun loadProfile(): PlayerTechnicalProfile {
        val saved = preferences.getString(PROFILE_KEY, null) ?: return demoProfile()
        val data = JSONObject(saved)
        val historyArray = data.getJSONArray("physicalHistory")
        val history = buildList {
            for (index in 0 until historyArray.length()) {
                val measurement = historyArray.getJSONObject(index)
                add(
                    PhysicalMeasurement(
                        heightMeters = measurement.getDouble("heightMeters"),
                        weightKilograms = measurement.getDouble("weightKilograms"),
                        recordedAt = measurement.getLong("recordedAt"),
                    )
                )
            }
        }
        return PlayerTechnicalProfile(
            fullName = data.getString("fullName"),
            category = data.getString("category"),
            dateOfBirth = data.getString("dateOfBirth"),
            primaryPosition = data.getString("primaryPosition"),
            secondaryPosition = data.getString("secondaryPosition"),
            dominantFoot = data.getString("dominantFoot"),
            heightMeters = data.getDouble("heightMeters"),
            weightKilograms = data.getDouble("weightKilograms"),
            profilePhotoUri = data.getString("profilePhotoUri"),
            contactPhone = data.getString("contactPhone"),
            emergencyContactName = data.getString("emergencyContactName"),
            emergencyRelationship = data.getString("emergencyRelationship"),
            emergencyPhone = data.getString("emergencyPhone"),
            physicalHistory = history,
        )
    }

    private fun persistProfile() {
        val historyArray = JSONArray()
        profile.physicalHistory.forEach { measurement ->
            historyArray.put(
                JSONObject()
                    .put("heightMeters", measurement.heightMeters)
                    .put("weightKilograms", measurement.weightKilograms)
                    .put("recordedAt", measurement.recordedAt)
            )
        }
        preferences
            .edit()
            .putString(
                PROFILE_KEY,
                JSONObject()
                    .put("fullName", profile.fullName)
                    .put("category", profile.category)
                    .put("dateOfBirth", profile.dateOfBirth)
                    .put("primaryPosition", profile.primaryPosition)
                    .put("secondaryPosition", profile.secondaryPosition)
                    .put("dominantFoot", profile.dominantFoot)
                    .put("heightMeters", profile.heightMeters)
                    .put("weightKilograms", profile.weightKilograms)
                    .put("profilePhotoUri", profile.profilePhotoUri)
                    .put("contactPhone", profile.contactPhone)
                    .put("emergencyContactName", profile.emergencyContactName)
                    .put("emergencyRelationship", profile.emergencyRelationship)
                    .put("emergencyPhone", profile.emergencyPhone)
                    .put("physicalHistory", historyArray)
                    .toString(),
            )
            .apply()
    }

    private fun demoProfile(): PlayerTechnicalProfile {
        // Empty onboarding record; never show another player's sample contact details.
        val account = AuthSession.profile.value
        val isPlayer = account?.role == com.dpa.sportpro.model.UserRole.PLAYER
        return PlayerTechnicalProfile(
            fullName = if (isPlayer) listOfNotNull(account?.names, account?.lastNames).joinToString(" ") else "",
            category = "",
            dateOfBirth = if (isPlayer) account?.birthDateMillis?.let {
                java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()
            }.orEmpty() else "",
            primaryPosition = "", secondaryPosition = "", dominantFoot = "",
            heightMeters = 0.0, weightKilograms = 0.0, profilePhotoUri = "",
            contactPhone = "", emergencyContactName = "", emergencyRelationship = "",
            emergencyPhone = "", physicalHistory = emptyList(),
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "sportpro_player_profile"
        const val PROFILE_KEY = "player_technical_profile"
    }
}
