package com.playgroundfinder.app.data.repository

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.google.firebase.firestore.FirebaseFirestore
import com.playgroundfinder.app.domain.model.BillingProducts
import com.playgroundfinder.app.domain.model.SubscriptionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "subscription_prefs")
private val SUBSCRIPTION_KEY = stringPreferencesKey("subscription_status")

@Singleton
class SubscriptionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _subscriptionStatus = MutableStateFlow(SubscriptionStatus.FREE)
    val subscriptionStatus = _subscriptionStatus.asStateFlow()

    /** Helyi gyorsítótár (DataStore) — az utoljára ismert állapot */
    val cachedStatus: Flow<SubscriptionStatus> = context.dataStore.data.map { prefs ->
        when (prefs[SUBSCRIPTION_KEY]) {
            "PREMIUM_MONTHLY" -> SubscriptionStatus.PREMIUM_MONTHLY
            "PREMIUM_YEARLY"  -> SubscriptionStatus.PREMIUM_YEARLY
            else              -> SubscriptionStatus.FREE
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    init {
        connectBilling()
    }

    private fun connectBilling() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { queryExistingPurchases() }
                }
            }
            override fun onBillingServiceDisconnected() {
                // Reconnect attempt is handled by the BillingClient internally
            }
        })
    }

    /** Aktív vásárlások lekérdezése (pl. alkalmazás újraindításakor) */
    suspend fun queryExistingPurchases() = withContext(Dispatchers.IO) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        handlePurchases(result.purchasesList)
    }

    /**
     * Előfizetés vásárlási folyamat indítása.
     * @param productId [BillingProducts.PREMIUM_MONTHLY] vagy [BillingProducts.PREMIUM_YEARLY]
     */
    suspend fun launchBillingFlow(activity: Activity, productId: String) = withContext(Dispatchers.IO) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        val detailsResult = billingClient.queryProductDetails(params)
        val productDetails: ProductDetails = detailsResult.productDetailsList?.firstOrNull() ?: return@withContext

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return@withContext
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        withContext(Dispatchers.Main) {
            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { handlePurchases(purchases) }
        }
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams)
                }
                val status = when {
                    purchase.products.contains(BillingProducts.PREMIUM_YEARLY)  -> SubscriptionStatus.PREMIUM_YEARLY
                    purchase.products.contains(BillingProducts.PREMIUM_MONTHLY) -> SubscriptionStatus.PREMIUM_MONTHLY
                    else -> SubscriptionStatus.FREE
                }
                updateSubscriptionStatus(status)
            }
        }
    }

    private suspend fun updateSubscriptionStatus(status: SubscriptionStatus) {
        _subscriptionStatus.value = status
        // Helyi gyorsítótár frissítése
        context.dataStore.edit { prefs -> prefs[SUBSCRIPTION_KEY] = status.name }
        // Firestore szinkronizáció (ha be van jelentkezve)
        authRepository.currentUserId?.let { uid ->
            firestore.collection("users").document(uid)
                .set(mapOf("subscriptionStatus" to status.name))
                .await()
        }
    }

    /** Firestore-ból betölti az előfizetési állapotot (bejelentkezés után) */
    suspend fun syncSubscriptionFromFirestore(userId: String) = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            val statusStr = doc.getString("subscriptionStatus") ?: "FREE"
            val status = SubscriptionStatus.valueOf(statusStr)
            _subscriptionStatus.value = status
            context.dataStore.edit { prefs -> prefs[SUBSCRIPTION_KEY] = status.name }
        } catch (_: Exception) { /* Offline esetén a helyi gyorsítótárat használjuk */ }
    }
}
