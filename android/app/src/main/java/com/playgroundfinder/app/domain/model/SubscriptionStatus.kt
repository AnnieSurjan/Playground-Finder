package com.playgroundfinder.app.domain.model

enum class SubscriptionStatus {
    /** Ingyenes fiók – csak alapfunkciók */
    FREE,
    /** Prémium havi előfizetés (990 Ft/hó) */
    PREMIUM_MONTHLY,
    /** Prémium éves előfizetés (4990 Ft/év) */
    PREMIUM_YEARLY;

    val isPremium: Boolean get() = this != FREE
}

// Google Play product ID-k
object BillingProducts {
    const val PREMIUM_MONTHLY = "playground_finder_premium_monthly"
    const val PREMIUM_YEARLY  = "playground_finder_premium_yearly"
}
