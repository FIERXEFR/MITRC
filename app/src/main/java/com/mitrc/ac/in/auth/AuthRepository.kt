package com.mitrc.ac.`in`.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object AuthRepository {

    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return suspendCancellableCoroutine { continuation ->
            try {
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { response ->
                        val user = response.user
                        if (user == null) {
                            continuation.resume(Result.failure(IllegalStateException("Authentication failed: UID null")))
                        } else {
                            continuation.resume(Result.success(user))
                        }
                    }
                    .addOnFailureListener { error ->
                        continuation.resume(Result.failure(error))
                    }
            } catch (error: Throwable) {
                continuation.resume(Result.failure(error))
            }
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            try {
                auth.sendPasswordResetEmail(email.trim())
                    .addOnSuccessListener { continuation.resume(Result.success(Unit)) }
                    .addOnFailureListener { continuation.resume(Result.failure(it)) }
            } catch (error: Throwable) {
                continuation.resume(Result.failure(error))
            }
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    fun friendlyMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                message.contains("wrong password", ignoreCase = true) ||
                message.contains("badly formatted", ignoreCase = true) ||
                message.contains("invalid email", ignoreCase = true) -> "Invalid email or password"

            message.contains("user not found", ignoreCase = true) -> "No account found for this email"
            message.contains("network", ignoreCase = true) ||
                message.contains("Unable to resolve host", ignoreCase = true) ->
                "Network error - check your internet connection"

            message.contains("too many requests", ignoreCase = true) ->
                "Too many attempts. Please try again later"

            message.contains("user disabled", ignoreCase = true) ->
                "This account has been disabled. Contact the admin"

            else -> error.localizedMessage ?: "Sign-in failed. Please try again"
        }
    }
}
