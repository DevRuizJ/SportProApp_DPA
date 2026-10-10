package com.dpa.sportpro.data.model

data class MatchEventType(
    val id: String,
    val name: String,
    val description: String,
    val requiredFields: List<String>,
    val optionalFields: List<String>,
)

data class CategoryEventSettings(val category: String, val enabledEventIds: Set<String>)
