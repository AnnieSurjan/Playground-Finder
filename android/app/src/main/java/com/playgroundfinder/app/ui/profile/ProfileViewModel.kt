package com.playgroundfinder.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playgroundfinder.app.data.repository.AuthRepository
import com.playgroundfinder.app.data.repository.ProfileRepository
import com.playgroundfinder.app.domain.model.Invite
import com.playgroundfinder.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val displayName: String = "",
    val email: String = "",
    val isAdmin: Boolean = false,
    val inviteEmail: String = "",
    val isSending: Boolean = false,
    val sentInvites: List<Invite> = emptyList(),
    val receivedInvites: List<Invite> = emptyList(),
    val message: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        _state.update { it.copy(
            displayName = authRepository.currentUserDisplayName ?: "",
            email       = authRepository.currentUserEmail ?: "",
            isAdmin     = authRepository.isAdmin
        ) }
        observeInvites()
    }

    private fun observeInvites() {
        viewModelScope.launch {
            profileRepository.getReceivedInvites().collect { list ->
                _state.update { it.copy(receivedInvites = list) }
            }
        }
        viewModelScope.launch {
            profileRepository.getSentInvites().collect { list ->
                _state.update { it.copy(sentInvites = list) }
            }
        }
    }

    fun onInviteEmailChange(email: String) {
        _state.update { it.copy(inviteEmail = email) }
    }

    fun sendInvite() {
        val email = _state.value.inviteEmail.trim()
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            when (val result = profileRepository.sendInvite(email)) {
                is Resource.Success -> _state.update { it.copy(
                    isSending = false,
                    inviteEmail = "",
                    message = "Meghívó elküldve: $email"
                ) }
                is Resource.Error -> _state.update { it.copy(
                    isSending = false,
                    message = result.message
                ) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun respondToInvite(inviteId: String, accept: Boolean) {
        viewModelScope.launch {
            profileRepository.respondToInvite(inviteId, accept)
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
