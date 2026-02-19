package com.playgroundfinder.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playgroundfinder.app.data.repository.AuthRepository
import com.playgroundfinder.app.data.repository.SubscriptionRepository
import com.playgroundfinder.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    val isLoggedIn: Boolean get() = authRepository.isLoggedIn

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.register(name, email, password)) {
                is Resource.Success -> {
                    result.data?.uid?.let { uid ->
                        subscriptionRepository.syncSubscriptionFromFirestore(uid)
                    }
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is Resource.Error -> _uiState.value = AuthUiState(error = result.message)
                is Resource.Loading -> Unit
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.login(email, password)) {
                is Resource.Success -> {
                    result.data?.uid?.let { uid ->
                        subscriptionRepository.syncSubscriptionFromFirestore(uid)
                    }
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is Resource.Error -> _uiState.value = AuthUiState(error = result.message)
                is Resource.Loading -> Unit
            }
        }
    }

    fun logout() = authRepository.logout()

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
