package com.playgroundfinder.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.playgroundfinder.app.domain.model.User
import com.playgroundfinder.app.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    /** Aktuális bejelentkezett felhasználó valós idejű megfigyelése */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    val isLoggedIn: Boolean get() = firebaseAuth.currentUser != null

    val currentUserId: String? get() = firebaseAuth.currentUser?.uid

    /** Regisztráció e-mail + jelszó alapján */
    suspend fun register(name: String, email: String, password: String): Resource<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Resource.Error("Regisztráció sikertelen")
            // Név beállítása
            val profileUpdate = userProfileChangeRequest { displayName = name }
            firebaseUser.updateProfile(profileUpdate).await()
            Resource.Success(firebaseUser.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Regisztráció sikertelen")
        }
    }

    /** Bejelentkezés e-mail + jelszó alapján */
    suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Resource.Error("Bejelentkezés sikertelen")
            Resource.Success(firebaseUser.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Bejelentkezés sikertelen")
        }
    }

    /** Kijelentkezés */
    fun logout() = firebaseAuth.signOut()

    /** Jelszó-visszaállító e-mail küldése */
    suspend fun sendPasswordReset(email: String): Resource<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sikertelen")
        }
    }

    private fun FirebaseUser.toDomain() = User(
        uid = uid,
        email = email ?: "",
        displayName = displayName
    )
}
