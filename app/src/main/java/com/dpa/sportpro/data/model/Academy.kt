package com.dpa.sportpro.data.model

data class AcademyCategory(
    val id: String = "",
    val name: String,
    val coachId: String? = null,
    val coachName: String = ""
)

data class Academy(
    val id: String = "",
    val name: String,
    val shieldUrl: String = "",
    val crestUri: String = "",
    val venue: String = "",
    val mainVenue: String = "",
    val description: String = "",
    val categories: List<AcademyCategory> = emptyList()
)
