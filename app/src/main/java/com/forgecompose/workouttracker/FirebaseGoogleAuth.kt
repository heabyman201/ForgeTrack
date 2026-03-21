package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseGoogleAuth {
    private const val TAG = "FirebaseGoogleAuth"

    suspend fun ensureSignedIn(context: Context): String? = withContext(Dispatchers.Main) {
        val auth = Firebase.auth
        auth.currentUser?.let { user ->
            if (!user.isAnonymous) return@withContext user.uid
        }

        val webClientIdRes = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (webClientIdRes == 0) {
            Log.w(TAG, "Missing default_web_client_id; cannot perform Google silent sign-in.")
            return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
        }

        val webClientId = context.getString(webClientIdRes).trim()
        if (webClientId.isBlank()) {
            Log.w(TAG, "Empty default_web_client_id; cannot perform Google silent sign-in.")
            return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
        }

        val credentialManager = CredentialManager.create(context.applicationContext)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setAutoSelectEnabled(true)
            .setServerClientId(webClientId)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialResult = runCatching { credentialManager.getCredential(context, request) }
            .onFailure { Log.i(TAG, "No authorized Google credential available for silent sign-in.") }
            .getOrNull()
            ?: return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }

        val rawCredential = credentialResult.credential
        if (rawCredential !is CustomCredential ||
            rawCredential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Log.w(TAG, "Unexpected credential type returned for Google sign-in.")
            return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
        }

        val idToken = runCatching {
            GoogleIdTokenCredential.createFrom(rawCredential.data).idToken
        }.getOrNull()

        if (idToken.isNullOrBlank()) {
            Log.w(TAG, "Google credential returned no idToken.")
            return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = runCatching { auth.signInWithCredential(credential).await() }
            .onFailure { Log.w(TAG, "Firebase signInWithCredential failed", it) }
            .getOrNull()
        authResult?.user?.uid ?: auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
    }
}
