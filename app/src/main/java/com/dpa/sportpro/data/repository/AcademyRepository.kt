package com.dpa.sportpro.data.repository

import android.content.Context
import com.dpa.sportpro.data.model.Academy
import com.dpa.sportpro.data.model.AcademyCategory
import com.dpa.sportpro.data.model.UserProfile
import com.dpa.sportpro.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AcademyRepository(context: Context? = null) {

    private val availableCoachesList = listOf(
        UserProfile("coach_1", "carlos.gomez@sportpro.com", "Carlos", "Gómez", UserRole.COACH),
        UserProfile("coach_2", "roberto.perez@sportpro.com", "Roberto", "Pérez", UserRole.COACH),
        UserProfile("coach_3", "luis.martinez@sportpro.com", "Luis", "Martínez", UserRole.COACH)
    )

    private val _academiesState = MutableStateFlow(
        listOf(
            Academy(
                id = "acad_1",
                name = "SportPro FC",
                shieldUrl = "https://example.com/shield.png",
                crestUri = "https://example.com/shield.png",
                mainVenue = "Sede Central Lima Norte",
                venue = "Sede Central Lima Norte",
                description = "Academia de fútbol alto rendimiento para jóvenes talentos.",
                categories = listOf(
                    AcademyCategory("cat_1", "Sub-10", "coach_1", "Carlos Gómez"),
                    AcademyCategory("cat_2", "Sub-15", "coach_2", "Roberto Pérez"),
                    AcademyCategory("cat_3", "Primera División", "coach_3", "Luis Martínez")
                )
            )
        )
    )

    val academiesState: StateFlow<List<Academy>> = _academiesState.asStateFlow()

    val academies: List<Academy>
        get() = _academiesState.value

    fun addAcademy(
        name: String,
        crestUri: String,
        venue: String,
        description: String,
        categoryDrafts: List<Any> = emptyList()
    ) {
        val convertedCategories = categoryDrafts.mapIndexed { index, draft ->
            val catName = try {
                draft.javaClass.getMethod("getName").invoke(draft) as? String ?: "Categoría ${index + 1}"
            } catch (e: Exception) {
                "Categoría ${index + 1}"
            }
            val coachName = try {
                draft.javaClass.getMethod("getCoachName").invoke(draft) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
            AcademyCategory(
                id = "cat_${System.currentTimeMillis()}_$index",
                name = catName,
                coachName = coachName
            )
        }

        val newAcademy = Academy(
            id = "acad_${System.currentTimeMillis()}",
            name = name,
            shieldUrl = crestUri,
            crestUri = crestUri,
            mainVenue = venue,
            venue = venue,
            description = description,
            categories = convertedCategories
        )

        _academiesState.update { current ->
            current + newAcademy
        }
    }

    fun addAcademy(academy: Academy) {
        _academiesState.update { current ->
            current + academy
        }
    }

    fun addCategory(
        academyId: String,
        categoryName: String,
        coachId: String?,
        coachName: String?
    ) {
        _academiesState.update { currentList ->
            currentList.map { academy ->
                if (academy.id == academyId) {
                    val newCategory = AcademyCategory(
                        id = "cat_${System.currentTimeMillis()}",
                        name = categoryName,
                        coachId = coachId,
                        coachName = coachName ?: ""
                    )
                    academy.copy(categories = academy.categories + newCategory)
                } else {
                    academy
                }
            }
        }
    }

    fun getAvailableCoaches(): List<UserProfile> = availableCoachesList

    companion object {
        @Volatile
        private var instance: AcademyRepository? = null

        fun getInstance(context: Context? = null): AcademyRepository {
            return instance ?: synchronized(this) {
                instance ?: AcademyRepository(context).also { instance = it }
            }
        }
    }
}
