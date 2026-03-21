package com.forgecompose.app_wear.presentation

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object WearFirebaseGoogleAuth {
    private const val TAG = "WearFirebaseGoogleAuth"

    suspend fun ensureSignedIn(context: Context): String? = withContext(Dispatchers.IO) {
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
            Log.w(TAG, "Missing default_web_client_id on wear; cannot silent sign-in.")
            return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
        }

        val webClientId = context.getString(webClientIdRes).trim()
        if (webClientId.isBlank()) {
            Log.w(TAG, "Empty default_web_client_id on wear; cannot silent sign-in.")
            return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(context.applicationContext, gso)

        val account = runCatching { googleClient.silentSignIn().await() }
            .onFailure { error ->
                val apiException = error as? ApiException
                if (apiException?.statusCode == 4) {
                    Log.i(TAG, "Silent sign-in unavailable on wear (SIGN_IN_REQUIRED).")
                } else {
                    Log.w(TAG, "Google silentSignIn failed on wear", error)
                }
            }
            .getOrNull()
            ?: return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }

        val idToken = account.idToken
        if (idToken.isNullOrBlank()) {
            Log.w(TAG, "Google account returned no idToken on wear.")
            return@withContext auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = runCatching { auth.signInWithCredential(credential).await() }
            .onFailure { Log.w(TAG, "Firebase signInWithCredential failed on wear", it) }
            .getOrNull()
        authResult?.user?.uid ?: auth.currentUser?.uid?.takeIf { auth.currentUser?.isAnonymous == false }
    }
}
