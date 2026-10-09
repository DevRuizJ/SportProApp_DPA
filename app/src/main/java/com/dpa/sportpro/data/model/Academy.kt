package com.dpa.sportpro.data.model

data class AcademyCategory(
    val name: String,
    val coachName: String
)

data class Academy(
    val id: String,
    val name: String,
    val crestUri: String,
    val mainVenue: String,
    val description: String,
    val categories: List<AcademyCategory>
)
