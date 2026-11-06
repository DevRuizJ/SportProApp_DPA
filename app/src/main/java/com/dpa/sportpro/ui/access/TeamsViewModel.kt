package com.dpa.sportpro.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeamsState(val busy: Boolean = false, val error: String? = null,
    val teams: List<SportsTeam> = emptyList(), val coaches: List<TeamCoach> = emptyList(),
    val players: List<ClubPlayer> = emptyList(), val selected: SportsTeam? = null,
    val roster: List<RosterPlayer> = emptyList())

class TeamsViewModel : ViewModel() {
    private val repo = TeamRepository()
    private val mutable = MutableStateFlow(TeamsState())
    val state = mutable.asStateFlow()
    private var club = ""
    private var admin = false
    fun load(clubId: String, isAdmin: Boolean) {
        club = clubId; admin = isAdmin
        action {
            val teams = repo.teams(club, admin)
            val coaches = if(admin) repo.coaches(club) else emptyList()
            val players = if(admin) ClubAccessRepository().players(club).items else emptyList()
            mutable.update { it.copy(teams = teams, coaches = coaches, players = players) }
        }
    }
    private fun action(block: suspend () -> Unit) {
        if(state.value.busy) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { block() } catch(e: Exception) {
                if(e is CancellationException) throw e
                mutable.update { it.copy(error = "No se completó la operación. Verifica conexión y permisos del club.") }
            } finally { mutable.update { it.copy(busy = false) } }
        }
    }
    fun create(name: String, category: String, coach: TeamCoach) = action {
        repo.create(club, name, category, coach)
        val teams = repo.teams(club, admin)
        mutable.update { it.copy(teams = teams) }
    }
    fun select(team: SportsTeam) = action {
        val roster = repo.roster(team)
        mutable.update { it.copy(selected = team, roster = roster) }
    }
    fun back() { mutable.update { it.copy(selected = null, roster = emptyList()) } }
    fun assign(coach: TeamCoach) = action {
        val team = checkNotNull(state.value.selected)
        repo.assignCoach(team, coach)
        val teams = repo.teams(club, admin)
        mutable.update { it.copy(selected = team.copy(coachUid = coach.uid, coachName = coach.name), teams = teams) }
    }
    fun player(id: String, active: Boolean) = action {
        val team = checkNotNull(state.value.selected)
        repo.setPlayer(team, id, active)
        val roster = repo.roster(team)
        mutable.update { it.copy(roster = roster) }
    }
}
