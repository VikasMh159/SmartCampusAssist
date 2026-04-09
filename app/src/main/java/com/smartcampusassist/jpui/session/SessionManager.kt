package com.smartcampusassist.jpui.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionState(
    val role: String? = null
)

object SessionManager {

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun updateRole(role: String) {
        _sessionState.value = SessionState(role = role)
    }

    fun clear() {
        _sessionState.value = SessionState()
    }
}
