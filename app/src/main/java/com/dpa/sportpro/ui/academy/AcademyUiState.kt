package com.dpa.sportpro.ui.academy

import com.dpa.sportpro.data.model.Academy
import com.dpa.sportpro.data.model.UserProfile

data class AcademyUiState(
    val academyName: String = "",
    val shieldUrl: String = "",
    val mainVenue: String = "",
    val description: String = "",
    val newCategoryName: String = "",
    val selectedCoach: UserProfile? = null,

    val nameError: String? = null,
    val venueError: String? = null,
    val categoryError: String? = null,

    val academies: List<Academy> = emptyList(),
    val availableCoaches: List<UserProfile> = emptyList(),
    val selectedAcademyForCategory: Academy? = null,
    val showAddCategoryDialog: Boolean = false,
    val successMessage: String? = null
)
