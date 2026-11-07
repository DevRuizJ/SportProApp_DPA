package com.dpa.sportpro.data.repository

import android.content.Context
import com.dpa.sportpro.data.model.ReportTargetType
import com.dpa.sportpro.data.model.TryoutAnnouncement
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TryoutRepository private constructor(context: Context) {

    private val preferencesContext = context.applicationContext
    private val preferences =
        preferencesContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _announcements = MutableStateFlow(loadAnnouncements())

    val announcements: StateFlow<List<TryoutAnnouncement>> = _announcements.asStateFlow()

    fun publish(
        clubName: String,
        category: String,
        positions: List<String>,
        date: String,
        time: String,
        address: String,
        city: String,
        requirements: String,
        contactEmail: String,
        contactPhone: String,
        publisherId: String,
        publisherName: String,
        publisherRole: String,
    ) {
        require(publisherId.isNotBlank()) { "No se pudo identificar la cuenta que publica." }
        require(
            !ModerationRepository.getInstance(preferencesContext).isUserSuspended(publisherId)
        ) {
            "Esta cuenta está suspendida y no puede publicar convocatorias."
        }
        require(clubName.isNotBlank()) { "Ingresa el club organizador." }
        require(category.isNotBlank()) { "Ingresa la categoría o edad requerida." }
        val requestedPositions = positions.map(String::trim).filter(String::isNotEmpty).distinct()
        require(requestedPositions.isNotEmpty()) { "Indica al menos una posición buscada." }
        require(isFutureDate(date)) { "Selecciona una fecha futura válida." }
        require(TIME_PATTERN.matches(time)) { "Ingresa la hora en formato HH:mm." }
        require(address.isNotBlank()) { "Ingresa la dirección exacta de la prueba." }
        require(city.isNotBlank()) { "Ingresa la ciudad o distrito." }
        require(requirements.isNotBlank()) { "Describe los requisitos para postular." }
        require(contactEmail.isNotBlank() || contactPhone.isNotBlank()) {
            "Agrega un correo o teléfono de contacto."
        }
        if (contactEmail.isNotBlank()) {
            require(EMAIL_PATTERN.matches(contactEmail.trim())) {
                "Ingresa un correo de contacto válido."
            }
        }
        if (contactPhone.isNotBlank()) {
            require(contactPhone.count(Char::isDigit) >= 7) {
                "Ingresa un teléfono de contacto válido."
            }
        }
        require(publisherRole == "ADM" || publisherRole == "DT") {
            "Solo el Administrador o Director Técnico puede publicar convocatorias."
        }

        val announcement =
            TryoutAnnouncement(
                id = UUID.randomUUID().toString(),
                clubName = clubName.trim(),
                category = category.trim(),
                positions = requestedPositions,
                date = date,
                time = time.trim(),
                address = address.trim(),
                city = city.trim(),
                requirements = requirements.trim(),
                contactEmail = contactEmail.trim(),
                contactPhone = contactPhone.trim(),
                publisherId = publisherId,
                publisherName = publisherName.trim(),
                publisherRole = publisherRole,
                createdAtMillis = System.currentTimeMillis(),
            )
        persist(listOf(announcement) + _announcements.value)
    }

    fun deleteAnnouncement(announcementId: String, moderatorRole: String) {
        require(moderatorRole == "ADM") { "Solo un Administrador puede eliminar convocatorias." }
        require(_announcements.value.any { it.id == announcementId }) {
            "No se encontró la convocatoria."
        }
        persist(_announcements.value.filterNot { it.id == announcementId })
        ModerationRepository.getInstance(preferencesContext)
            .resolveTarget(ReportTargetType.TRYOUT, announcementId, moderatorRole)
    }

    fun search(
        category: String,
        position: String,
        date: String,
        city: String,
    ): List<TryoutAnnouncement> =
        _announcements.value.filter { announcement ->
            (category.isBlank() ||
                announcement.category.contains(category.trim(), ignoreCase = true)) &&
                (position.isBlank() ||
                    announcement.positions.any {
                        it.contains(position.trim(), ignoreCase = true)
                    }) &&
                (date.isBlank() || announcement.date == date.trim()) &&
                (city.isBlank() || announcement.city.contains(city.trim(), ignoreCase = true))
        }

    private fun persist(announcements: List<TryoutAnnouncement>) {
        val json = JSONArray()
        announcements.forEach { announcement -> json.put(announcement.toJson()) }
        preferences.edit().putString(ANNOUNCEMENTS_KEY, json.toString()).apply()
        _announcements.value = announcements
    }

    private fun loadAnnouncements(): List<TryoutAnnouncement> {
        val saved = preferences.getString(ANNOUNCEMENTS_KEY, null) ?: return emptyList()
        val json = JSONArray(saved)
        return List(json.length()) { index -> json.getJSONObject(index).toAnnouncement() }
    }

    private fun TryoutAnnouncement.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("clubName", clubName)
            .put("category", category)
            .put("positions", JSONArray(positions))
            .put("date", date)
            .put("time", time)
            .put("address", address)
            .put("city", city)
            .put("requirements", requirements)
            .put("contactEmail", contactEmail)
            .put("contactPhone", contactPhone)
            .put("publisherId", publisherId)
            .put("publisherName", publisherName)
            .put("publisherRole", publisherRole)
            .put("createdAtMillis", createdAtMillis)

    private fun JSONObject.toAnnouncement(): TryoutAnnouncement {
        val positionArray = getJSONArray("positions")
        return TryoutAnnouncement(
            id = getString("id"),
            clubName = getString("clubName"),
            category = getString("category"),
            positions = List(positionArray.length()) { positionArray.getString(it) },
            date = getString("date"),
            time = getString("time"),
            address = getString("address"),
            city = getString("city"),
            requirements = getString("requirements"),
            contactEmail = optString("contactEmail"),
            contactPhone = optString("contactPhone"),
            publisherId = optString("publisherId").ifBlank { "legacy:${getString("id")}" },
            publisherName = optString("publisherName"),
            publisherRole = optString("publisherRole"),
            createdAtMillis = getLong("createdAtMillis"),
        )
    }

    private fun isFutureDate(value: String): Boolean {
        val parser = SimpleDateFormat(DATE_PATTERN, Locale.ROOT).apply { isLenient = false }
        val date =
            try {
                parser.parse(value)
            } catch (_: ParseException) {
                null
            } ?: return false
        val today = SimpleDateFormat(DATE_PATTERN, Locale.ROOT).format(Date())
        return value > today && parser.format(date) == value
    }

    companion object {
        private const val PREFERENCES_NAME = "sportpro_tryouts"
        private const val ANNOUNCEMENTS_KEY = "announcements"
        private const val DATE_PATTERN = "yyyy-MM-dd"
        private val TIME_PATTERN = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        @Volatile private var instance: TryoutRepository? = null

        fun getInstance(context: Context): TryoutRepository =
            instance
                ?: synchronized(this) {
                    instance ?: TryoutRepository(context.applicationContext).also { instance = it }
                }
    }
}
