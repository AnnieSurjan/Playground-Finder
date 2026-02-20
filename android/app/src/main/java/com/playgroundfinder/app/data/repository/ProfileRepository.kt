package com.playgroundfinder.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.playgroundfinder.app.domain.model.Invite
import com.playgroundfinder.app.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore?,
    private val authRepository: AuthRepository
) {
    private val invitesRef get() = firestore?.collection("invites")

    /** Meghívó küldése e-mail cím alapján */
    suspend fun sendInvite(toEmail: String): Resource<Unit> {
        val fs = firestore ?: return Resource.Error("Firebase nem elérhető")
        val uid = authRepository.currentUserId ?: return Resource.Error("Nincs bejelentkezve")
        val fromEmail = authRepository.currentUserEmail ?: return Resource.Error("Ismeretlen e-mail")
        val fromName = authRepository.currentUserDisplayName ?: fromEmail

        if (toEmail.isBlank()) return Resource.Error("Add meg az e-mail címet")
        if (toEmail.equals(fromEmail, ignoreCase = true)) return Resource.Error("Saját magadat nem hívhatod meg")

        return try {
            // Duplikált meghívó ellenőrzése
            val existing = fs.collection("invites")
                .whereEqualTo("fromUid", uid)
                .whereEqualTo("toEmail", toEmail.lowercase())
                .whereEqualTo("status", "pending")
                .get().await()

            if (!existing.isEmpty) return Resource.Error("Már küldtél meghívót erre a címre")

            val invite = hashMapOf(
                "fromUid"   to uid,
                "fromEmail" to fromEmail,
                "fromName"  to fromName,
                "toEmail"   to toEmail.lowercase(),
                "status"    to "pending",
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            fs.collection("invites").add(invite).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Meghívó küldése sikertelen")
        }
    }

    /** Beérkezett meghívók (ahol a toEmail = saját e-mail) */
    fun getReceivedInvites(): Flow<List<Invite>> = callbackFlow {
        val email = authRepository.currentUserEmail?.lowercase()
        if (firestore == null || email == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val listener = firestore.collection("invites")
            .whereEqualTo("toEmail", email)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toInvite()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Elküldött meghívók (ahol fromUid = saját uid) */
    fun getSentInvites(): Flow<List<Invite>> = callbackFlow {
        val uid = authRepository.currentUserId
        if (firestore == null || uid == null) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }
        val listener = firestore.collection("invites")
            .whereEqualTo("fromUid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.toInvite()
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    /** Meghívóra válasz (elfogadás / elutasítás) */
    suspend fun respondToInvite(inviteId: String, accept: Boolean): Resource<Unit> {
        val fs = firestore ?: return Resource.Error("Firebase nem elérhető")
        return try {
            fs.collection("invites").document(inviteId)
                .update("status", if (accept) "accepted" else "rejected")
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sikertelen")
        }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toInvite(): Invite? {
        return try {
            Invite(
                id        = id,
                fromUid   = getString("fromUid") ?: return null,
                fromEmail = getString("fromEmail") ?: return null,
                fromName  = getString("fromName") ?: getString("fromEmail") ?: return null,
                toEmail   = getString("toEmail") ?: return null,
                status    = getString("status") ?: "pending"
            )
        } catch (_: Exception) { null }
    }
}
