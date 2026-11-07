package com.dpa.sportpro.data.repository

import android.content.Context
import com.dpa.sportpro.data.model.AnnouncementPriority
import com.dpa.sportpro.data.model.OfficialAnnouncement
import com.dpa.sportpro.data.model.ReportTargetType
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AnnouncementRepository private constructor(context: Context) {

    private val preferencesContext = context.applicationContext
    private val preferences =
        preferencesContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _announcements = MutableStateFlow(loadAnnouncements())

    val announcements: StateFlow<List<OfficialAnnouncement>> = _announcements.asStateFlow()

    fun subscriptionsFor(userKey: String, availableCategories: List<String>): Set<String> {
        val saved = preferences.getStringSet(subscriptionKey(userKey), null)
        if (saved != null) return saved.toSet()

        val defaults = availableCategories.toSet()
        saveSubscriptions(userKey, defaults)
        return defaults
    }

    fun saveSubscriptions(userKey: String, categories: Set<String>) {
        preferences.edit().putStringSet(subscriptionKey(userKey), categories.toSet()).apply()
    }

    fun unreadIdsFor(userKey: String, subscriptions: Set<String>): Set<String> {
        val readIds = preferences.getStringSet(readKey(userKey), emptySet()).orEmpty()
        return _announcements.value
            .filter { it.category in subscriptions && it.id !in readIds }
            .map { it.id }
            .toSet()
    }

    fun announcementsFor(subscriptions: Set<String>): List<OfficialAnnouncement> {
        return _announcements.value.filter { it.category in subscriptions }
    }

    fun markAsRead(userKey: String, announcementId: String) {
        val readIds =
            preferences.getStringSet(readKey(userKey), emptySet()).orEmpty().toMutableSet()
        readIds += announcementId
        preferences.edit().putStringSet(readKey(userKey), readIds).apply()
    }

    fun publish(
        title: String,
        body: String,
        priority: AnnouncementPriority,
        category: String,
        senderId: String,
        senderName: String,
        senderRoleCode: String,
    ) {
        require(title.isNotBlank()) { "El título es obligatorio." }
        require(body.isNotBlank()) { "El mensaje es obligatorio." }
        require(category.isNotBlank()) { "Selecciona una categoría destino." }
        require(senderId.isNotBlank()) { "No se pudo identificar la cuenta que publica." }
        require(!ModerationRepository.getInstance(preferencesContext).isUserSuspended(senderId)) {
            "Esta cuenta está suspendida y no puede publicar anuncios."
        }

        val announcement =
            OfficialAnnouncement(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                body = body.trim(),
                priority = priority,
                category = category,
                senderId = senderId,
                senderName = senderName,
                senderRoleCode = senderRoleCode,
                createdAtMillis = System.currentTimeMillis(),
            )
        val updated = listOf(announcement) + _announcements.value
        _announcements.value = updated
        persistAnnouncements(updated)
    }

    fun deleteAnnouncement(announcementId: String, moderatorRole: String) {
        require(moderatorRole == "ADM") { "Solo un Administrador puede eliminar anuncios." }
        require(_announcements.value.any { it.id == announcementId }) {
            "No se encontró el anuncio."
        }
        val updated = _announcements.value.filterNot { it.id == announcementId }
        _announcements.value = updated
        persistAnnouncements(updated)
        ModerationRepository.getInstance(preferencesContext)
            .resolveTarget(ReportTargetType.OFFICIAL_ANNOUNCEMENT, announcementId, moderatorRole)
    }

    private fun loadAnnouncements(): List<OfficialAnnouncement> {
        val saved = preferences.getString(ANNOUNCEMENTS_KEY, null) ?: return emptyList()
        val values = JSONArray(saved)
        return List(values.length()) { index ->
            val value = values.getJSONObject(index)
            OfficialAnnouncement(
                id = value.getString("id"),
                title = value.getString("title"),
                body = value.getString("body"),
                priority = AnnouncementPriority.valueOf(value.getString("priority")),
                category = value.getString("category"),
                senderId =
                    value.optString("senderId").ifBlank {
                        "legacy-announcement:${value.getString("id")}"
                    },
                senderName = value.getString("senderName"),
                senderRoleCode = value.getString("senderRoleCode"),
                createdAtMillis = value.getLong("createdAtMillis"),
            )
        }
    }

    private fun persistAnnouncements(announcements: List<OfficialAnnouncement>) {
        val values = JSONArray()
        announcements.forEach { announcement ->
            values.put(
                JSONObject()
                    .put("id", announcement.id)
                    .put("title", announcement.title)
                    .put("body", announcement.body)
                    .put("priority", announcement.priority.name)
                    .put("category", announcement.category)
                    .put("senderId", announcement.senderId)
                    .put("senderName", announcement.senderName)
                    .put("senderRoleCode", announcement.senderRoleCode)
                    .put("createdAtMillis", announcement.createdAtMillis)
            )
        }
        preferences.edit().putString(ANNOUNCEMENTS_KEY, values.toString()).apply()
    }

    private fun subscriptionKey(userKey: String) = "subscriptions_$userKey"

    private fun readKey(userKey: String) = "read_$userKey"

    companion object {
        private const val PREFERENCES_NAME = "sportpro_announcements"
        private const val ANNOUNCEMENTS_KEY = "announcements"

        @Volatile private var instance: AnnouncementRepository? = null

        fun getInstance(context: Context): AnnouncementRepository =
            instance
                ?: synchronized(this) {
                    instance
                        ?: AnnouncementRepository(context.applicationContext).also { instance = it }
                }
    }
}
