package com.smartcampusassist.jpui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val errorMessage: String? = null
)

class ProfileViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    private fun loadProfile() {

        viewModelScope.launch {
            _uiState.value = ProfileUiState(isLoading = true)

            try {
                val data = repository.getUserProfile()
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    profile = data
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Unable to load profile."
                )
            }
        }
    }
}
