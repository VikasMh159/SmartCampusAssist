package com.smartcampusassist.jpui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcampusassist.jpui.auth.FirebaseAuthRepository
import com.smartcampusassist.jpui.main.MainScreenDestination
import com.smartcampusassist.jpui.session.SessionManager
import com.smartcampusassist.jpui.session.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel : ViewModel() {

    private val authRepository by lazy { FirebaseAuthRepository() }
    private val _mainDestination =
        MutableStateFlow<MainScreenDestination>(MainScreenDestination.Home)
    private var isRestoringSession = false

    val sessionState: StateFlow<SessionState> = SessionManager.sessionState
    val mainDestination: StateFlow<MainScreenDestination> = _mainDestination.asStateFlow()

    fun restoreSession(
        onLoggedOut: () -> Unit,
        onLoggedIn: () -> Unit
    ) {
        if (isRestoringSession) return

        viewModelScope.launch {
            isRestoringSession = true
            val user = authRepository.getCurrentUser()
            if (user == null) {
                SessionManager.clear()
                onLoggedOut()
                isRestoringSession = false
                return@launch
            }

            try {
                val session = authRepository.restoreAuthorizedSession()
                SessionManager.updateRole(session.role)
                onLoggedIn()
            } catch (_: Exception) {
                authRepository.logout()
                SessionManager.clear()
                onLoggedOut()
            } finally {
                isRestoringSession = false
            }
        }
    }

    fun updateRole(role: String) {
        SessionManager.updateRole(role)
    }

    fun setMainDestination(destination: MainScreenDestination) {
        _mainDestination.value = destination
    }

    fun logout() {
        authRepository.logout()
        SessionManager.clear()
        _mainDestination.value = MainScreenDestination.Home
    }
}
