package com.dpa.sportpro.ui.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.repository.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ClubAccessState(
    val busy: Boolean = false, val error: String? = null, val notice: String? = null,
    val platform: Boolean = false, val clubs: List<ClubRecord> = emptyList(),
    val moreClubs: Boolean = false, val selected: ClubRecord? = null,
    val own: List<AccessRequest> = emptyList(), val children: List<FamilyLink> = emptyList(),
    val requests: List<AccessRequest> = emptyList(), val families: List<FamilyLink> = emptyList(),
    val players: List<ClubPlayer> = emptyList(), val moreRequests: Boolean = false,
    val moreFamilies: Boolean = false, val morePlayers: Boolean = false,
    val invitation: FamilyInvitation? = null, val generatedCode: String? = null,
    val child: ClubPlayer? = null,
)

class ClubAccessViewModel : ViewModel() {
    private val repository = ClubAccessRepository()
    val user = checkNotNull(AuthSession.profile.value)
    private val mutable = MutableStateFlow(ClubAccessState())
    val state = mutable.asStateFlow()
    private var clubCursor: DocumentSnapshot? = null
    private var requestCursor: DocumentSnapshot? = null
    private var familyCursor: DocumentSnapshot? = null
    private var playerCursor: DocumentSnapshot? = null
    fun isAdmin(s: ClubAccessState = state.value) = s.platform || s.own.any {
        it.clubId == s.selected?.id && it.status == "APPROVED" && it.role == "ADM"
    }
    fun mayDecide(item: AccessRequest) = item.uid != user.uid &&
        (if (item.role == "ADM") state.value.platform else isAdmin())
    init {
        viewModelScope.launch {
            repository.ownAccess().catch { showError(it) }.collect { rows ->
                mutable.update { it.copy(own = rows) }
                if (!isAdmin()) mutable.update { it.copy(requests = emptyList(), families = emptyList(), players = emptyList()) }
            }
        }
        viewModelScope.launch {
            repository.myChildren().catch { showError(it) }.collect { rows ->
                mutable.update { it.copy(children = rows, child = null) }
            }
        }
        action {
            mutable.update { it.copy(platform = repository.platformAuthority()) }
            loadClubs(false)
        }
    }

    private fun showError(e: Throwable) {
        if (e is CancellationException) throw e
        val message = if (e is FirebaseFirestoreException) when(e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Acceso no autorizado o configuración de permisos pendiente. No se completó la operación."
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Sin conexión. Esta operación necesita confirmación del servidor."
            else -> "No se pudo completar la operación (${e.code})."
        } else e.message ?: "No se pudo completar la operación."
        mutable.update { it.copy(error = message) }
    }
    private fun action(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try { block() } catch(e: Exception) { showError(e) }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    private suspend fun loadClubs(more: Boolean) {
        val page = repository.clubs(if(more) clubCursor else null)
        clubCursor = page.cursor
        mutable.update { it.copy(clubs = (if(more) it.clubs else emptyList()) + page.items, moreClubs = page.hasMore) }
    }
    fun moreClubs() = action { loadClubs(true) }
    fun createClub(name: String, description: String) = action {
        repository.createClub(name, description)
        loadClubs(false)
        mutable.update { it.copy(notice = "Club creado. Los administradores deben solicitar acceso.") }
    }
    fun select(club: ClubRecord) = action {
        mutable.update { it.copy(selected = club, requests = emptyList(), families = emptyList(),
            players = emptyList(), generatedCode = null) }
        if (isAdmin()) loadManagement()
    }
    fun back() { mutable.update { it.copy(selected = null, generatedCode = null, child = null, invitation = null) } }
    private suspend fun loadManagement() {
        val club = checkNotNull(state.value.selected).id
        val req = repository.reviewRequests(club)
        val fam = repository.reviewFamilies(club)
        val players = repository.players(club)
        requestCursor = req.cursor; familyCursor = fam.cursor; playerCursor = players.cursor
        mutable.update { it.copy(requests = req.items, families = fam.items, players = players.items,
            moreRequests = req.hasMore, moreFamilies = fam.hasMore, morePlayers = players.hasMore) }
    }
    fun refresh() = action {
        mutable.update { it.copy(platform = repository.platformAuthority()) }
        if (isAdmin() && state.value.selected != null) loadManagement() else loadClubs(false)
    }
    fun requestRole(role: String) = action {
        repository.requestRole(checkNotNull(state.value.selected), role)
        mutable.update { it.copy(notice = "Solicitud enviada. Su estado se actualizará aquí automáticamente.") }
    }
    fun decide(item: AccessRequest, status: String, reason: String) = action {
        repository.decideAccess(item, status, reason); loadManagement()
        mutable.update { it.copy(notice = "Decisión guardada y permisos actualizados para este club.") }
    }
    fun decideFamily(item: FamilyLink, status: String, reason: String) = action {
        repository.decideFamily(item, status, reason); loadManagement()
        mutable.update { it.copy(notice = "Estado del vínculo actualizado.") }
    }
    fun addPlayer(name: String, category: String) = action {
        repository.addPlayer(checkNotNull(state.value.selected), name, category); loadManagement()
        mutable.update { it.copy(notice = "Ficha deportiva creada. No se ha creado una cuenta de acceso.") }
    }
    fun invite(player: ClubPlayer) = action {
        val code = repository.inviteFamily(checkNotNull(state.value.selected), player)
        mutable.update { it.copy(generatedCode = code) }
    }
    fun inspect(code: String) = action {
        val invitation = repository.inspectInvitation(code)
        mutable.update { it.copy(invitation = invitation) }
    }
    fun confirmInvitation() = action {
        repository.claimInvitation(checkNotNull(state.value.invitation))
        mutable.update { it.copy(invitation = null, notice = "Vínculo solicitado. El club verificará la relación familiar.") }
    }
    fun dismissInvitation() { mutable.update { it.copy(invitation = null) } }
    fun child(item: FamilyLink) = action {
        val player = repository.childProfile(item)
        mutable.update { it.copy(child = player) }
    }
    fun dismissChild() { mutable.update { it.copy(child = null) } }
    fun moreRequests() = action {
        val page = repository.reviewRequests(checkNotNull(state.value.selected).id, requestCursor)
        requestCursor = page.cursor
        mutable.update { it.copy(requests = it.requests + page.items, moreRequests = page.hasMore) }
    }
    fun moreFamilies() = action {
        val page = repository.reviewFamilies(checkNotNull(state.value.selected).id, familyCursor)
        familyCursor = page.cursor
        mutable.update { it.copy(families = it.families + page.items, moreFamilies = page.hasMore) }
    }
    fun morePlayers() = action {
        val page = repository.players(checkNotNull(state.value.selected).id, playerCursor)
        playerCursor = page.cursor
        mutable.update { it.copy(players = it.players + page.items, morePlayers = page.hasMore) }
    }
}
