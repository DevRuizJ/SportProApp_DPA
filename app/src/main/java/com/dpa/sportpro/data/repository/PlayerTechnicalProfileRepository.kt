package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.PhysicalMeasurement
import com.dpa.sportpro.data.model.PlayerTechnicalProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class PlayerTechnicalProfileRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var profile by mutableStateOf(loadProfile())
        private set

    fun updateProfile(updated: PlayerTechnicalProfile) {
        val physicalDataChanged = updated.heightMeters != profile.heightMeters ||
            updated.weightKilograms != profile.weightKilograms
        profile = updated.copy(
            physicalHistory = if (physicalDataChanged) {
                updated.physicalHistory + PhysicalMeasurement(
                    heightMeters = updated.heightMeters,
                    weightKilograms = updated.weightKilograms,
                    recordedAt = System.currentTimeMillis()
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
                        recordedAt = measurement.getLong("recordedAt")
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
            physicalHistory = history
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
        preferences.edit().putString(
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
                .toString()
        ).apply()
    }

    private fun demoProfile(): PlayerTechnicalProfile {
        val measuredAt = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return PlayerTechnicalProfile(
            fullName = "Mateo Silva Rossi",
            category = "Sub-15",
            dateOfBirth = "2010-04-12",
            primaryPosition = "Mediocampista",
            secondaryPosition = "Extremo",
            dominantFoot = "Derecha (Diestro)",
            heightMeters = 1.72,
            weightKilograms = 65.0,
            profilePhotoUri = "",
            contactPhone = "+51 987 654 321",
            emergencyContactName = "Juan Silva Rossi",
            emergencyRelationship = "Padre",
            emergencyPhone = "+51 912 345 678",
            physicalHistory = listOf(
                PhysicalMeasurement(
                    heightMeters = 1.72,
                    weightKilograms = 65.0,
                    recordedAt = measuredAt
                )
            )
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "sportpro_player_profile"
        const val PROFILE_KEY = "player_technical_profile"
    }
}
