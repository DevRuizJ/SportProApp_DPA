package com.dpa.sportpro.data.model

data class PhysicalMeasurement(
    val heightMeters: Double,
    val weightKilograms: Double,
    val recordedAt: Long,
)

data class PlayerTechnicalProfile(
    val fullName: String,
    val category: String,
    val dateOfBirth: String,
    val primaryPosition: String,
    val secondaryPosition: String,
    val dominantFoot: String,
    val heightMeters: Double,
    val weightKilograms: Double,
    val profilePhotoUri: String,
    val contactPhone: String,
    val emergencyContactName: String,
    val emergencyRelationship: String,
    val emergencyPhone: String,
    val physicalHistory: List<PhysicalMeasurement>,
)
