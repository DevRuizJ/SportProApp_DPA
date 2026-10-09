package com.dpa.sportpro.data.model

data class FeePlayer(
    val id: String,
    val name: String,
    val category: String,
    val linkedAccountNames: List<String>
)

enum class FeeStatus(val label: String) {
    PENDING("Pendiente"),
    PAID("Pagado"),
    EXEMPT("Exonerado");

    companion object {
        fun fromLabel(label: String): FeeStatus =
            entries.firstOrNull { it.label == label } ?: PENDING
    }
}

data class MonthlyFee(
    val playerId: String,
    val year: Int,
    val month: Int,
    val amount: Double,
    val status: FeeStatus
)
