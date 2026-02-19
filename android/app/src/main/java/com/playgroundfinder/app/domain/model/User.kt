package com.playgroundfinder.app.domain.model

data class User(
    val uid: String,
    val email: String,
    val displayName: String?,
    val subscription: SubscriptionStatus = SubscriptionStatus.FREE
)
