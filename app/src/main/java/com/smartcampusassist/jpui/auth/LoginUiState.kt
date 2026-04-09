package com.smartcampusassist.jpui.auth

sealed class LoginUiState {

    object Idle : LoginUiState()

    object Loading : LoginUiState()

    data class Info(val message: String) : LoginUiState()

    data class Success(val role: String) : LoginUiState()

    data class Error(val message: String) : LoginUiState()
}
