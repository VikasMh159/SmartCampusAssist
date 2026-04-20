package com.smartcampusassist.jpui.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SessionState(
    val userId: String? = null,
    val fullName: String? = null,
    val role: String? = null,
    val instituteId: String? = null,
    val instituteName: String? = null
)

object SessionManager {

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun updateSession(
        userId: String,
        fullName: String,
        role: String,
        instituteId: String,
        instituteName: String
    ) {
        _sessionState.value = SessionState(
            userId = userId,
            fullName = fullName,
            role = role,
            instituteId = instituteId,
            instituteName = instituteName
        )
    }

    fun clear() {
        _sessionState.value = SessionState()
    }
}
