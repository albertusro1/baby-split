package com.babysplit.app.core.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Returns the currently signed-in Firebase user, or null if not signed in.
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Emits the current Firebase user whenever auth state changes.
     */
    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Initiates Google Sign-In via Credential Manager.
     * Uses GetSignInWithGoogleOption for explicit button clicks with GetGoogleIdOption fallback
     * to eliminate first-attempt NoCredentialException.
     *
     * @param activityContext Must be an Activity Context to display the Credential Manager UI
     * @param webClientId The Web Client ID from Firebase Console (Authentication > Google > Web SDK config)
     */
    suspend fun signInWithGoogle(activityContext: Context, webClientId: String): Result<FirebaseUser> {
        val credentialManager = CredentialManager.create(activityContext)

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
            .build()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val primaryRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .addCredentialOption(googleIdOption)
            .build()

        val result: GetCredentialResponse = try {
            credentialManager.getCredential(
                context = activityContext,
                request = primaryRequest
            )
        } catch (_: NoCredentialException) {
            // Fallback: If no credential found on initial attempt, retry explicitly with GetSignInWithGoogleOption
            try {
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()
                credentialManager.getCredential(
                    context = activityContext,
                    request = fallbackRequest
                )
            } catch (e: Exception) {
                return Result.failure(e)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return try {
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                    ?: return Result.failure(Exception("Firebase user is null after sign-in"))

                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Signs out from Firebase Auth and clears Credential Manager state.
     */
    suspend fun signOut(activityContext: Context? = null) {
        auth.signOut()
        try {
            val manager = CredentialManager.create(activityContext ?: context)
            manager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Ignore errors when clearing credential state
        }
    }
}

