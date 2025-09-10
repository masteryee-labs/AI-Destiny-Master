package com.aidestinymaster.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes

object GoogleAuthManager {
    private val requiredScopes = listOf(DriveScopes.DRIVE_APPDATA)

    fun getSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun signIn(context: Context): GoogleSignInAccount? =
        GoogleSignIn.getLastSignedInAccount(context)

    fun getAccessToken(context: Context): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return getAccessToken(account, context)
    }

    fun getAccessToken(account: GoogleSignInAccount, context: Context): String? {
        return try {
            val credential = GoogleAccountCredential.usingOAuth2(context, requiredScopes)
            credential.selectedAccount = account.account
            credential.token
        } catch (e: Exception) {
            null
        }
    }
}

