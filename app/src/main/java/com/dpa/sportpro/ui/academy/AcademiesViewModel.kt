package com.dpa.sportpro.ui.academy

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.model.Academy
import com.dpa.sportpro.data.model.AcademyCategory
import com.dpa.sportpro.data.repository.AcademyRepository
import com.dpa.sportpro.data.storage.SupabaseCrestStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AcademiesUiState(
    val academies: List<Academy> = emptyList(),
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

class AcademiesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AcademyRepository(application)
    private val storage = SupabaseCrestStorage(application.contentResolver)
    private val mutableState = MutableStateFlow(AcademiesUiState(academies = repository.academies))
    val state = mutableState.asStateFlow()

    fun clearResult() {
        if (!mutableState.value.saving) mutableState.update { it.copy(saved = false, error = null) }
    }

    fun save(name: String, crestUri: String, venue: String, description: String, categories: List<AcademyCategory>) {
        if (mutableState.value.saving) return
        if (name.isBlank() || crestUri.isBlank() || venue.isBlank() || description.isBlank() ||
            categories.isEmpty() || categories.any { it.name.isBlank() || it.coachName.isBlank() }) {
            mutableState.update { it.copy(error = "Completa los datos de la academia y sus categorías.") }
            return
        }
        mutableState.update { it.copy(saving = true, saved = false, error = null) }
        viewModelScope.launch {
            try {
                val publicUrl = storage.upload(Uri.parse(crestUri))
                repository.addAcademy(name, publicUrl, venue, description, categories)
                mutableState.value = AcademiesUiState(academies = repository.academies, saved = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val message = when (error) {
                    is IllegalArgumentException, is IllegalStateException ->
                        error.message ?: "No se pudo guardar la academia."
                    else -> "No se pudo subir el escudo. Revisa tu conexión y permisos e inténtalo de nuevo."
                }
                mutableState.update { it.copy(error = message) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }
}
