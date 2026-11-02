package com.dpa.sportpro.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.repository.AuthRepositoryImpl
import com.dpa.sportpro.data.repository.AuthSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel : ViewModel() {
    private val repository = AuthRepositoryImpl()
    private val mutableRestoring = MutableStateFlow(true)
    val restoring = mutableRestoring.asStateFlow()
    val profile = AuthSession.profile

    private val auth = FirebaseAuth.getInstance()
    private val listener = FirebaseAuth.AuthStateListener {
        if (it.currentUser == null) AuthSession.set(null)
    }

    init {
        auth.addAuthStateListener(listener)
        viewModelScope.launch {
            try { repository.restoreSession() } finally { mutableRestoring.value = false }
        }
    }

    fun signOut() = repository.signOut()

    override fun onCleared() {
        auth.removeAuthStateListener(listener)
    }
}
