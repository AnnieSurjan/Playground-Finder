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

private const val FIREBASE_NOT_CONFIGURED =
    "A Firebase még nincs beállítva. Kövesd a local.properties.example utasításait a google-services.json letöltéséhez."

const val ADMIN_EMAIL = "surjaneniko@gmail.com"

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth?
) {
    val isFirebaseAvailable: Boolean get() = firebaseAuth != null

    val isAdmin: Boolean get() = firebaseAuth?.currentUser?.email == ADMIN_EMAIL
    val currentUserEmail: String? get() = firebaseAuth?.currentUser?.email
    val currentUserDisplayName: String? get() = firebaseAuth?.currentUser?.displayName

    /** Aktuális bejelentkezett felhasználó valós idejű megfigyelése */
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val auth = firebaseAuth
        if (auth == null) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { a -> trySend(a.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val isLoggedIn: Boolean get() = firebaseAuth?.currentUser != null

    val currentUserId: String? get() = firebaseAuth?.currentUser?.uid

    /** Regisztráció e-mail + jelszó alapján */
    suspend fun register(name: String, email: String, password: String): Resource<User> {
        val auth = firebaseAuth ?: return Resource.Error(FIREBASE_NOT_CONFIGURED)
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Resource.Error("Regisztráció sikertelen")
            val profileUpdate = userProfileChangeRequest { displayName = name }
            firebaseUser.updateProfile(profileUpdate).await()
            Resource.Success(firebaseUser.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Regisztráció sikertelen")
        }
    }

    /** Bejelentkezés e-mail + jelszó alapján */
    suspend fun login(email: String, password: String): Resource<User> {
        val auth = firebaseAuth ?: return Resource.Error(FIREBASE_NOT_CONFIGURED)
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: return Resource.Error("Bejelentkezés sikertelen")
            Resource.Success(firebaseUser.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Bejelentkezés sikertelen")
        }
    }

    /** Kijelentkezés */
    fun logout() = firebaseAuth?.signOut()

    /** Jelszó-visszaállító e-mail küldése */
    suspend fun sendPasswordReset(email: String): Resource<Unit> {
        val auth = firebaseAuth ?: return Resource.Error(FIREBASE_NOT_CONFIGURED)
        return try {
            auth.sendPasswordResetEmail(email).await()
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
