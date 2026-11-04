package com.dpa.sportpro.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.dpa.sportpro.data.repository.LiveMatchRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.viewModelScope
import com.dpa.sportpro.data.repository.AuthRepositoryImpl
import com.dpa.sportpro.data.repository.AuthSession
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {
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
            profile.collectLatest { user ->
                if (user != null) {
                    val sender = LiveMatchRepository(application)
                    var delayMillis = 5_000L
                    while (true) {
                        try { sender.flushPending(user.uid) } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            // Commands remain in Room; errors are surfaced per operation.
                        }
                        delay(delayMillis)
                        delayMillis = (delayMillis * 2).coerceAtMost(60_000L)
                    }
                }
            }
        }
        viewModelScope.launch {
            try { repository.restoreSession() } finally { mutableRestoring.value = false }
        }
    }

    fun signOut() = repository.signOut()

    override fun onCleared() {
        auth.removeAuthStateListener(listener)
    }
}
