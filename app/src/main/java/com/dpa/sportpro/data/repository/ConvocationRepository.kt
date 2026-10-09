package com.dpa.sportpro.data.repository

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dpa.sportpro.data.model.ConvocationResponse
import com.dpa.sportpro.data.model.ConvokedPlayer
import com.dpa.sportpro.data.model.MatchConvocation
import com.dpa.sportpro.data.model.MatchType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class ConvocationRepository private constructor(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var convocations by mutableStateOf(loadConvocations())
        private set

    fun createAndPublish(
        category: String,
        competition: String,
        opponent: String,
        matchType: MatchType,
        date: String,
        callTime: String,
        venue: String,
        players: List<ConvokedPlayer>
    ): MatchConvocation {
        require(category.isNotBlank()) { "Selecciona una categoría." }
        require(opponent.isNotBlank()) { "Ingresa el rival." }
        require(date.isNotBlank() && callTime.isNotBlank()) { "Define la fecha y hora de citación." }
        require(venue.isNotBlank()) { "Ingresa el lugar del encuentro." }
        require(players.isNotEmpty()) { "Selecciona al menos un jugador." }
        val convocation = MatchConvocation(
            id = UUID.randomUUID().toString(),
            category = category,
            competition = competition.trim(),
            opponent = opponent.trim(),
            matchType = matchType,
            date = date,
            callTime = callTime,
            venue = venue.trim(),
            players = players.distinctBy { it.id }.map {
                it.copy(response = ConvocationResponse.PENDING, justification = "")
            },
            isPublished = true,
            publishedAt = System.currentTimeMillis()
        )
        convocations = listOf(convocation) + convocations
        persist()
        return convocation
    }

    fun respond(
        convocationId: String,
        playerId: String,
        response: ConvocationResponse,
        justification: String
    ) {
        require(response != ConvocationResponse.PENDING) { "Selecciona una respuesta." }
        val target = convocations.firstOrNull { it.id == convocationId }
            ?: error("No se encontró la convocatoria.")
        require(target.isPublished) { "La convocatoria todavía no fue publicada." }
        require(target.players.any { it.id == playerId }) { "El jugador no pertenece a la convocatoria." }

        convocations = convocations.map { convocation ->
            if (convocation.id != convocationId) return@map convocation
            convocation.copy(
                players = convocation.players.map { player ->
                    if (player.id == playerId) {
                        player.copy(
                            response = response,
                            justification = justification.trim()
                        )
                    } else {
                        player
                    }
                }
            )
        }
        persist()
    }

    private fun loadConvocations(): List<MatchConvocation> {
        val saved = preferences.getString(CONVOCATIONS_KEY, null) ?: return listOf(demoConvocation())
        val array = JSONArray(saved)
        val loaded = buildList {
            for (index in 0 until array.length()) {
                val value = array.getJSONObject(index)
                val players = buildList {
                    val playerArray = value.getJSONArray("players")
                    for (playerIndex in 0 until playerArray.length()) {
                        val player = playerArray.getJSONObject(playerIndex)
                        val accountArray = player.getJSONArray("linkedAccountNames")
                        add(
                            ConvokedPlayer(
                                id = player.getString("id"),
                                name = player.getString("name"),
                                position = player.getString("position"),
                                linkedAccountNames = buildList {
                                    for (accountIndex in 0 until accountArray.length()) {
                                        add(accountArray.getString(accountIndex))
                                    }
                                },
                                response = ConvocationResponse.valueOf(player.getString("response")),
                                justification = player.getString("justification")
                            )
                        )
                    }
                }
                add(
                    MatchConvocation(
                        id = value.getString("id"),
                        category = value.getString("category"),
                        competition = value.getString("competition"),
                        opponent = value.getString("opponent"),
                        matchType = MatchType.valueOf(value.getString("matchType")),
                        date = value.getString("date"),
                        callTime = value.getString("callTime"),
                        venue = value.getString("venue"),
                        players = players,
                        isPublished = value.getBoolean("isPublished"),
                        publishedAt = if (value.isNull("publishedAt")) null else value.getLong("publishedAt")
                    )
                )
            }
        }
        return loaded.map { convocation ->
            if (convocation.id != DEMO_CONVOCATION_ID || convocation.confirmedCount >= MIN_DEMO_CONFIRMED) {
                convocation
            } else {
                val existingIds = convocation.players.map { it.id }.toSet()
                val missingPlayers = demoExtraPlayers
                    .filter { it.id !in existingIds }
                    .take(MIN_DEMO_CONFIRMED - convocation.confirmedCount)
                convocation.copy(players = convocation.players + missingPlayers)
            }
        }
    }

    private fun persist() {
        val array = JSONArray()
        convocations.forEach { convocation ->
            val playerArray = JSONArray()
            convocation.players.forEach { player ->
                val accountArray = JSONArray()
                player.linkedAccountNames.forEach(accountArray::put)
                playerArray.put(
                    JSONObject()
                        .put("id", player.id)
                        .put("name", player.name)
                        .put("position", player.position)
                        .put("linkedAccountNames", accountArray)
                        .put("response", player.response.name)
                        .put("justification", player.justification)
                )
            }
            array.put(
                JSONObject()
                    .put("id", convocation.id)
                    .put("category", convocation.category)
                    .put("competition", convocation.competition)
                    .put("opponent", convocation.opponent)
                    .put("matchType", convocation.matchType.name)
                    .put("date", convocation.date)
                    .put("callTime", convocation.callTime)
                    .put("venue", convocation.venue)
                    .put("players", playerArray)
                    .put("isPublished", convocation.isPublished)
                    .put("publishedAt", convocation.publishedAt ?: JSONObject.NULL)
            )
        }
        preferences.edit().putString(CONVOCATIONS_KEY, array.toString()).apply()
    }

    private fun demoConvocation(): MatchConvocation {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return MatchConvocation(
            id = "demo-convocation-sub15",
            category = "Sub-15",
            competition = "Torneo de Clausura",
            opponent = "Academia Cantolao",
            matchType = MatchType.OFFICIAL,
            date = "%04d-10-24".format(year),
            callTime = "16:00",
            venue = "Sede Principal: Campo de Marte (Cancha 1)",
            players = listOf(
                ConvokedPlayer("player_mateo", "Mateo Silva", "Delantero", listOf("Mateo Silva", "Mateo Silva Rossi"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_thiago", "Thiago Rossi", "Mediocampista", listOf("Thiago Ruiz Flores", "Thiago Rossi"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_lucas", "Lucas Castro", "Defensa", listOf("Lucas Gomez S.", "Lucas Castro"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_benjamin", "Benjamín Díaz", "Portero", listOf("Benjamín Díaz"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_gabriel", "Gabriel Ruiz", "Defensa", listOf("Gabriel Mendez O."), ConvocationResponse.UNAVAILABLE, "Lesión previa."),
                ConvokedPlayer("player_santiago", "Santiago Paz", "Mediocampista", listOf("Santiago Paz"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_esteban", "Esteban Rojas", "Defensa", listOf("Esteban Rojas"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_diego", "Diego López", "Delantero", listOf("Diego López"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_tomas", "Tomás Ortega", "Mediocampista", listOf("Tomás Ortega"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_raul", "Raúl Paredes", "Defensa", listOf("Raúl Paredes"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_andres", "Andrés Vega", "Mediocampista", listOf("Andrés Vega"), ConvocationResponse.CONFIRMED),
                ConvokedPlayer("player_carlos", "Carlos Medina", "Delantero", listOf("Carlos Medina"), ConvocationResponse.CONFIRMED)
            ),
            isPublished = true,
            publishedAt = System.currentTimeMillis()
        )
    }

    companion object {
        private const val PREFERENCES_NAME = "sportpro_match_convocations"
        private const val CONVOCATIONS_KEY = "convocations"
        private const val DEMO_CONVOCATION_ID = "demo-convocation-sub15"
        private const val MIN_DEMO_CONFIRMED = 11
        @Volatile
        private var instance: ConvocationRepository? = null

        private val demoExtraPlayers = listOf(
            ConvokedPlayer("player_jorge", "Jorge Salas", "Defensa", listOf("Jorge Salas"), ConvocationResponse.CONFIRMED),
            ConvokedPlayer("player_felipe", "Felipe Arias", "Mediocampista", listOf("Felipe Arias"), ConvocationResponse.CONFIRMED),
            ConvokedPlayer("player_omar", "Omar Campos", "Delantero", listOf("Omar Campos"), ConvocationResponse.CONFIRMED),
            ConvokedPlayer("player_pedro", "Pedro León", "Defensa", listOf("Pedro León"), ConvocationResponse.CONFIRMED),
            ConvokedPlayer("player_miguel", "Miguel Soto", "Mediocampista", listOf("Miguel Soto"), ConvocationResponse.CONFIRMED),
            ConvokedPlayer("player_alonso", "Alonso Cruz", "Delantero", listOf("Alonso Cruz"), ConvocationResponse.CONFIRMED),
            ConvokedPlayer("player_daniel", "Daniel Rivas", "Defensa", listOf("Daniel Rivas"), ConvocationResponse.CONFIRMED),
            ConvokedPlayer("player_pablo", "Pablo Reyes", "Mediocampista", listOf("Pablo Reyes"), ConvocationResponse.CONFIRMED),
            ConvokedPlayer("player_ivan", "Iván Luna", "Delantero", listOf("Iván Luna"), ConvocationResponse.CONFIRMED)
        )

        fun getInstance(context: Context): ConvocationRepository =
            instance ?: synchronized(this) {
                instance ?: ConvocationRepository(context.applicationContext).also { instance = it }
            }
    }
}
