package com.dpa.sportpro.ui.academy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.model.Academy
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.data.repository.AcademyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AcademyViewModel(
    private val repository: AcademyRepository = AcademyRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AcademyUiState())
    val uiState: StateFlow<AcademyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.academiesState.collect { academyList ->
                _uiState.update {
                    it.copy(
                        academies = academyList,
                        availableCoaches = repository.getAvailableCoaches()
                    )
                }
            }
        }
    }

    fun onAcademyNameChanged(name: String) {
        _uiState.update { it.copy(academyName = name, nameError = null) }
    }

    fun onShieldUrlChanged(url: String) {
        _uiState.update { it.copy(shieldUrl = url) }
    }

    fun onMainVenueChanged(venue: String) {
        _uiState.update { it.copy(mainVenue = venue, venueError = null) }
    }

    fun onDescriptionChanged(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun registerAcademy() {
        val state = _uiState.value
        var isValid = true

        val nameErr = if (state.academyName.isBlank()) {
            isValid = false
            "Ingrese el nombre de la academia."
        } else null

        val venueErr = if (state.mainVenue.isBlank()) {
            isValid = false
            "Ingrese la sede principal."
        } else null

        if (!isValid) {
            _uiState.update {
                it.copy(nameError = nameErr, venueError = venueErr)
            }
            return
        }

        val newAcademy = Academy(
            id = "acad_${System.currentTimeMillis()}",
            name = state.academyName,
            shieldUrl = state.shieldUrl,
            crestUri = state.shieldUrl,
            mainVenue = state.mainVenue,
            venue = state.mainVenue,
            description = state.description,
            categories = emptyList()
        )

        repository.addAcademy(newAcademy)

        _uiState.update {
            it.copy(
                academyName = "",
                shieldUrl = "",
                mainVenue = "",
                description = "",
                successMessage = "¡Academia ${newAcademy.name} registrada exitosamente!"
            )
        }
    }

    fun openAddCategoryDialog(academy: Academy) {
        _uiState.update {
            it.copy(
                selectedAcademyForCategory = academy,
                showAddCategoryDialog = true,
                newCategoryName = "",
                selectedCoach = null,
                categoryError = null
            )
        }
    }

    fun closeAddCategoryDialog() {
        _uiState.update { it.copy(showAddCategoryDialog = false) }
    }

    fun onNewCategoryNameChanged(name: String) {
        _uiState.update { it.copy(newCategoryName = name, categoryError = null) }
    }

    fun onCoachSelected(coach: UserProfile) {
        _uiState.update { it.copy(selectedCoach = coach) }
    }

    fun addCategoryToAcademy() {
        val state = _uiState.value
        val academy = state.selectedAcademyForCategory ?: return

        if (state.newCategoryName.isBlank()) {
            _uiState.update { it.copy(categoryError = "Ingrese el nombre de la categoría.") }
            return
        }

        repository.addCategory(
            academyId = academy.id,
            categoryName = state.newCategoryName,
            coachId = state.selectedCoach?.uid,
            coachName = if (state.selectedCoach != null) "${state.selectedCoach.names} ${state.selectedCoach.lastNames}" else null
        )

        _uiState.update {
            it.copy(
                showAddCategoryDialog = false,
                successMessage = "Categoría ${state.newCategoryName} asignada a ${academy.name}"
            )
        }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
