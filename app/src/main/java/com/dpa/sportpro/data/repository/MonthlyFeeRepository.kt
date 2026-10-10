package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.FeePlayer
import com.dpa.sportpro.data.model.FeeStatus
import com.dpa.sportpro.data.model.MonthlyFee
import com.dpa.sportpro.model.UserRole
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

class MonthlyFeeRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    val players: List<FeePlayer> = demoPlayers

    var fees by mutableStateOf(loadFees())
        private set

    fun visiblePlayers(viewerRole: UserRole, viewerName: String): List<FeePlayer> =
        when (viewerRole) {
            UserRole.ADMIN -> players
            UserRole.PLAYER,
            UserRole.PARENT ->
                players.filter { player ->
                    player.linkedAccountNames.any {
                        it.equals(viewerName.trim(), ignoreCase = true)
                    }
                }
            else -> emptyList()
        }

    fun feeFor(playerId: String, year: Int, month: Int): MonthlyFee =
        fees.firstOrNull { it.playerId == playerId && it.year == year && it.month == month }
            ?: MonthlyFee(
                playerId = playerId,
                year = year,
                month = month,
                amount = DEFAULT_AMOUNT,
                status = FeeStatus.PENDING,
            )

    fun updateFee(playerId: String, year: Int, month: Int, amount: Double, status: FeeStatus) {
        require(players.any { it.id == playerId }) { "Jugador no encontrado." }
        require(year in MIN_YEAR..MAX_YEAR) { "Periodo lectivo no válido." }
        require(month in Calendar.JANUARY..Calendar.DECEMBER) { "Mes no válido." }
        require(amount >= 0.0) { "El monto no puede ser negativo." }

        val updatedFee = MonthlyFee(playerId, year, month, amount, status)
        fees =
            fees.filterNot { it.playerId == playerId && it.year == year && it.month == month } +
                updatedFee
        persistFees()
    }

    private fun loadFees(): List<MonthlyFee> {
        val saved = preferences.getString(FEES_KEY, null) ?: return demoFees()
        val array = JSONArray(saved)
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                add(
                    MonthlyFee(
                        playerId = value.getString("playerId"),
                        year = value.getInt("year"),
                        month = value.getInt("month"),
                        amount = value.getDouble("amount"),
                        status = FeeStatus.fromLabel(value.getString("status")),
                    )
                )
            }
        }
    }

    private fun persistFees() {
        val array = JSONArray()
        fees.forEach { fee ->
            array.put(
                JSONObject()
                    .put("playerId", fee.playerId)
                    .put("year", fee.year)
                    .put("month", fee.month)
                    .put("amount", fee.amount)
                    .put("status", fee.status.label)
            )
        }
        preferences.edit().putString(FEES_KEY, array.toString()).apply()
    }

    private fun demoFees(): List<MonthlyFee> {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return (Calendar.JANUARY..Calendar.DECEMBER).map { month ->
            val status =
                when (month) {
                    in Calendar.JANUARY..Calendar.MAY -> FeeStatus.PAID
                    Calendar.JUNE -> FeeStatus.EXEMPT
                    else -> FeeStatus.PENDING
                }
            MonthlyFee(
                playerId = DEMO_PLAYER_ID,
                year = year,
                month = month,
                amount = DEFAULT_AMOUNT,
                status = status,
            )
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "sportpro_monthly_fees"
        const val FEES_KEY = "monthly_fees"
        const val DEMO_PLAYER_ID = "player_mateo"
        const val DEFAULT_AMOUNT = 150.0
        const val MIN_YEAR = 2020
        const val MAX_YEAR = 2100

        val demoPlayers =
            listOf(
                FeePlayer(
                    id = DEMO_PLAYER_ID,
                    name = "Mateo Silva",
                    category = "Sub-15",
                    linkedAccountNames = listOf("Mateo Silva", "Juan Silva"),
                )
            )
    }
}
