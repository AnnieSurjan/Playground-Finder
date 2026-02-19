package com.playgroundfinder.app.ui.subscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playgroundfinder.app.data.repository.SubscriptionRepository
import com.playgroundfinder.app.domain.model.BillingProducts
import com.playgroundfinder.app.domain.model.SubscriptionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val subscriptionStatus = subscriptionRepository.subscriptionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionStatus.FREE)

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState = _uiState.asStateFlow()

    fun purchaseMonthly(activity: Activity) = purchase(activity, BillingProducts.PREMIUM_MONTHLY)

    fun purchaseYearly(activity: Activity) = purchase(activity, BillingProducts.PREMIUM_YEARLY)

    private fun purchase(activity: Activity, productId: String) {
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState(isLoading = true)
            try {
                subscriptionRepository.launchBillingFlow(activity, productId)
                _uiState.value = SubscriptionUiState()
            } catch (e: Exception) {
                _uiState.value = SubscriptionUiState(
                    error = "Vásárlás indítása sikertelen: ${e.localizedMessage}"
                )
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
