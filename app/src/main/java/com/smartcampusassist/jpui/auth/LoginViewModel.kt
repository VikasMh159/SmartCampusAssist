package com.smartcampusassist.jpui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = FirebaseAuthRepository()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    private var isLoggingIn = false

    /* ---------------- EMAIL LOGIN ---------------- */

    fun loginWithEmail(email: String, password: String) {

        if (isLoggingIn) return

        viewModelScope.launch {
            try {
                isLoggingIn = true
                _uiState.value = LoginUiState.Loading

                // Step 1: Firebase Auth
                val session = repository.loginWithEmail(email, password)

                // Step 2: Fetch role from Firestore
                val role = session.role

                // Step 3: Send role to UI
                _uiState.value = LoginUiState.Success(role)

            } catch (e: Exception) {
                _uiState.value =
                    LoginUiState.Error(
                        mapLoginError(
                            error = e
                        )
                    )

            } finally {
                isLoggingIn = false
            }
        }
    }

    /* ---------------- GOOGLE LOGIN ---------------- */

    fun loginWithGoogle(idToken: String) {

        if (isLoggingIn) return

        viewModelScope.launch {
            try {
                isLoggingIn = true
                _uiState.value = LoginUiState.Loading

                // Step 1: Firebase Auth
                val session = repository.loginWithGoogle(idToken)

                // Step 2: Fetch role from Firestore
                val role = session.role

                // Step 3: Send role to UI
                _uiState.value = LoginUiState.Success(role)

            } catch (e: Exception) {

                _uiState.value =
                    LoginUiState.Error(
                        mapLoginError(e)
                    )

            } finally {
                isLoggingIn = false
            }
        }
    }

    /* ---------------- RESET STATE ---------------- */

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }

    private fun mapLoginError(error: Exception): String {
        return when (error) {
            is UnauthorizedAccountException -> {
                "Login failed. Use a valid account."
            }

            is FirebaseAuthInvalidUserException -> {
                "No Firebase Authentication user was found for this email."
            }

            is FirebaseAuthInvalidCredentialsException -> {
                "Email/password sign-in failed. Check that the password is correct and that this Firebase Auth user has a password credential linked."
            }

            else -> error.localizedMessage ?: "Login failed"
        }
    }

    fun sendPasswordReset(email: String) {
        if (isLoggingIn) return

        viewModelScope.launch {
            try {
                isLoggingIn = true
                _uiState.value = LoginUiState.Loading

                repository.sendPasswordResetEmail(email)
                _uiState.value = LoginUiState.Info(
                    "Password reset email sent. Check your inbox."
                )
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(
                    e.localizedMessage ?: "Unable to send password reset email"
                )
            } finally {
                isLoggingIn = false
            }
        }
    }
}
