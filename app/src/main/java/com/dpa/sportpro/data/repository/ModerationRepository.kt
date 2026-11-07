package com.dpa.sportpro.data.repository

import android.content.Context
import com.dpa.sportpro.data.model.ContentReport
import com.dpa.sportpro.data.model.ReportReason
import com.dpa.sportpro.data.model.ReportStatus
import com.dpa.sportpro.data.model.ReportTargetType
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class ModerationRepository private constructor(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val _reports = MutableStateFlow(loadReports())
    private val _suspendedUserIds = MutableStateFlow(loadSuspendedUsers())

    val reports: StateFlow<List<ContentReport>> = _reports.asStateFlow()
    val suspendedUserIds: StateFlow<Set<String>> = _suspendedUserIds.asStateFlow()

    fun report(
        targetType: ReportTargetType,
        targetId: String,
        targetParentId: String = "",
        targetOwnerId: String,
        targetTitle: String,
        reporterId: String,
        reporterName: String,
        reason: ReportReason,
    ) {
        require(reporterId.isNotBlank()) { "Inicia sesión para reportar contenido." }
        require(!isUserSuspended(reporterId)) {
            "La cuenta está suspendida y no puede reportar contenido."
        }
        require(targetId.isNotBlank() && targetOwnerId.isNotBlank()) {
            "No se pudo identificar el contenido reportado."
        }
        require(reporterId != targetOwnerId) { "No puedes reportar tu propio contenido." }
        require(
            _reports.value.none {
                it.targetType == targetType &&
                    it.targetId == targetId &&
                    it.reporterId == reporterId
            }
        ) {
            "Ya reportaste este contenido."
        }

        updateReports(
            _reports.value +
                ContentReport(
                    id = UUID.randomUUID().toString(),
                    targetType = targetType,
                    targetId = targetId,
                    targetParentId = targetParentId,
                    targetOwnerId = targetOwnerId,
                    targetTitle = targetTitle,
                    reporterId = reporterId,
                    reporterName = reporterName,
                    reason = reason,
                    createdAtMillis = System.currentTimeMillis(),
                )
        )
    }

    fun dismissReport(reportId: String, moderatorRole: String) {
        requireModerator(moderatorRole)
        require(_reports.value.any { it.id == reportId && it.status == ReportStatus.PENDING }) {
            "El reporte ya fue atendido."
        }
        updateReports(
            _reports.value.map {
                if (it.id == reportId) it.copy(status = ReportStatus.DISMISSED) else it
            }
        )
    }

    fun resolveTarget(targetType: ReportTargetType, targetId: String, moderatorRole: String) {
        requireModerator(moderatorRole)
        updateReports(
            _reports.value.map {
                if (
                    it.targetType == targetType &&
                        it.targetId == targetId &&
                        it.status == ReportStatus.PENDING
                ) {
                    it.copy(status = ReportStatus.REMOVED)
                } else {
                    it
                }
            }
        )
    }

    fun suspendUser(userId: String, moderatorRole: String) {
        requireModerator(moderatorRole)
        require(userId.isNotBlank()) { "No se pudo identificar la cuenta." }
        val updated = _suspendedUserIds.value + userId
        preferences.edit().putStringSet(SUSPENDED_USERS_KEY, updated).apply()
        _suspendedUserIds.value = updated
    }

    fun isUserSuspended(userId: String): Boolean = userId in _suspendedUserIds.value

    fun pendingReportCount(targetType: ReportTargetType, targetId: String): Int =
        _reports.value.count {
            it.targetType == targetType &&
                it.targetId == targetId &&
                it.status == ReportStatus.PENDING
        }

    fun isPreventivelyHidden(targetType: ReportTargetType, targetId: String): Boolean =
        pendingReportCount(targetType, targetId) >= HIDE_REPORT_THRESHOLD

    private fun requireModerator(role: String) {
        require(role == ADMIN_ROLE) { "Solo un Administrador puede moderar reportes." }
    }

    private fun updateReports(reports: List<ContentReport>) {
        val json = JSONArray()
        reports.forEach { report -> json.put(report.toJson()) }
        preferences.edit().putString(REPORTS_KEY, json.toString()).apply()
        _reports.value = reports
    }

    private fun loadReports(): List<ContentReport> {
        val saved = preferences.getString(REPORTS_KEY, null) ?: return emptyList()
        val json = JSONArray(saved)
        return List(json.length()) { index -> json.getJSONObject(index).toReport() }
    }

    private fun loadSuspendedUsers(): Set<String> =
        preferences.getStringSet(SUSPENDED_USERS_KEY, emptySet())?.toSet() ?: emptySet()

    private fun ContentReport.toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("targetType", targetType.name)
            .put("targetId", targetId)
            .put("targetParentId", targetParentId)
            .put("targetOwnerId", targetOwnerId)
            .put("targetTitle", targetTitle)
            .put("reporterId", reporterId)
            .put("reporterName", reporterName)
            .put("reason", reason.name)
            .put("createdAtMillis", createdAtMillis)
            .put("status", status.name)

    private fun JSONObject.toReport(): ContentReport =
        ContentReport(
            id = getString("id"),
            targetType = ReportTargetType.valueOf(getString("targetType")),
            targetId = getString("targetId"),
            targetParentId = optString("targetParentId"),
            targetOwnerId = getString("targetOwnerId"),
            targetTitle = getString("targetTitle"),
            reporterId = getString("reporterId"),
            reporterName = getString("reporterName"),
            reason = ReportReason.valueOf(getString("reason")),
            createdAtMillis = getLong("createdAtMillis"),
            status = ReportStatus.valueOf(getString("status")),
        )

    companion object {
        private const val PREFERENCES_NAME = "sportpro_moderation"
        private const val REPORTS_KEY = "content_reports"
        private const val SUSPENDED_USERS_KEY = "suspended_users"
        private const val ADMIN_ROLE = "ADM"
        const val HIDE_REPORT_THRESHOLD = 3

        @Volatile private var instance: ModerationRepository? = null

        fun getInstance(context: Context): ModerationRepository =
            instance
                ?: synchronized(this) {
                    instance
                        ?: ModerationRepository(context.applicationContext).also { instance = it }
                }
    }
}
