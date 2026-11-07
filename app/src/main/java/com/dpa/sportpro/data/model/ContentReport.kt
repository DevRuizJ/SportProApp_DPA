package com.dpa.sportpro.data.model

enum class ReportTargetType(val title: String) {
    POST("Publicación"),
    COMMENT("Comentario"),
    TRYOUT("Convocatoria"),
    OFFICIAL_ANNOUNCEMENT("Anuncio oficial"),
}

enum class ReportReason(val title: String) {
    ABUSIVE_CONDUCT("Conducta abusiva/ofensiva"),
    FALSE_TRYOUT("Convocatoria engañosa/falsa"),
    MINOR_PRIVACY("Vulneración de privacidad de menores"),
}

enum class ReportStatus {
    PENDING,
    DISMISSED,
    REMOVED,
}

data class ContentReport(
    val id: String,
    val targetType: ReportTargetType,
    val targetId: String,
    val targetParentId: String,
    val targetOwnerId: String,
    val targetTitle: String,
    val reporterId: String,
    val reporterName: String,
    val reason: ReportReason,
    val createdAtMillis: Long,
    val status: ReportStatus = ReportStatus.PENDING,
)
