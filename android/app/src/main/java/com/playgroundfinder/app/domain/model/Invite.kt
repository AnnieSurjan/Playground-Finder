package com.playgroundfinder.app.domain.model

data class Invite(
    val id: String,
    val fromUid: String,
    val fromEmail: String,
    val fromName: String,
    val toEmail: String,
    val status: String   // "pending" | "accepted" | "rejected"
) {
    val isPending:  Boolean get() = status == "pending"
    val isAccepted: Boolean get() = status == "accepted"
}
