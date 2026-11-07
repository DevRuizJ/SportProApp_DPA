package com.dpa.sportpro.data.model

enum class AnnouncementPriority(val label: String) {
    INFORMATIONAL("Informativo"),
    URGENT("Urgente"),
}

data class OfficialAnnouncement(
    val id: String,
    val title: String,
    val body: String,
    val priority: AnnouncementPriority,
    val category: String,
    val senderId: String,
    val senderName: String,
    val senderRoleCode: String,
    val createdAtMillis: Long,
)
