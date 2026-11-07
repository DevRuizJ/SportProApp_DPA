package com.dpa.sportpro.data.model

data class TryoutAnnouncement(
    val id: String,
    val clubName: String,
    val category: String,
    val positions: List<String>,
    val date: String,
    val time: String,
    val address: String,
    val city: String,
    val requirements: String,
    val contactEmail: String,
    val contactPhone: String,
    val publisherId: String,
    val publisherName: String,
    val publisherRole: String,
    val createdAtMillis: Long,
)
