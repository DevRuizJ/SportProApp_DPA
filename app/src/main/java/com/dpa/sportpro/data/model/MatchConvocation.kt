package com.dpa.sportpro.data.model

data class ConvokedPlayer(
    val id: String,
    val name: String,
    val position: String,
    val linkedAccountNames: List<String>,
    val response: ConvocationResponse = ConvocationResponse.PENDING,
    val justification: String = "",
)

enum class MatchType(val label: String) {
    FRIENDLY("Amistoso"),
    OFFICIAL("Oficial"),
}

enum class ConvocationResponse(val label: String) {
    PENDING("Pendiente"),
    CONFIRMED("Confirmo"),
    UNAVAILABLE("No disponible"),
}

data class MatchConvocation(
    val id: String,
    val category: String,
    val competition: String,
    val opponent: String,
    val matchType: MatchType,
    val date: String,
    val callTime: String,
    val venue: String,
    val players: List<ConvokedPlayer>,
    val isPublished: Boolean,
    val publishedAt: Long?,
) {
    val confirmedCount: Int
        get() = players.count { it.response == ConvocationResponse.CONFIRMED }

    val unavailableCount: Int
        get() = players.count { it.response == ConvocationResponse.UNAVAILABLE }

    val pendingCount: Int
        get() = players.count { it.response == ConvocationResponse.PENDING }
}
