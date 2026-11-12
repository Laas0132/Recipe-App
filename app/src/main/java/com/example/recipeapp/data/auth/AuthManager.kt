package com.example.recipeapp.data.auth

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class AuthManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val auth: FirebaseAuth by lazy {
        ensureFirebaseInitialized(context)
        FirebaseAuth.getInstance()
    }

    val currentUser: FirebaseUser?
        get() {
            return try {
                auth.currentUser
            } catch (_: Exception) {
                null
            }
        }

    val isGuestMode: Boolean
        get() = prefs.getBoolean("is_guest_mode", false)

    val googleUserEmail: String?
        get() = prefs.getString("google_user_email", null)

    val isLoggedIn: Boolean
        get() = (currentUser != null) || isGuestMode || (googleUserEmail != null)

    fun signIn(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        prefs.edit { 
            putBoolean("is_guest_mode", false)
            remove("google_user_email")
        }
        try {
            auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Authentication failed")
                    }
                }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Error during sign in")
        }
    }

    fun signUp(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        prefs.edit { 
            putBoolean("is_guest_mode", false)
            remove("google_user_email")
        }
        try {
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Registration failed")
                    }
                }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Error during sign up")
        }
    }

    fun signInGuest(onResult: (Boolean, String?) -> Unit) {
        prefs.edit { 
            putBoolean("is_guest_mode", true)
            remove("google_user_email")
        }
        try {
            auth.signInAnonymously()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        // Allow guest access directly even if Firebase anonymous auth fails/unconfigured
                        onResult(true, null)
                    }
                }
        } catch (_: Exception) {
            onResult(true, null)
        }
    }

    fun signInFakeGoogle(email: String = "google.user@gmail.com", onResult: (Boolean, String?) -> Unit) {
        prefs.edit { 
            putBoolean("is_guest_mode", false)
            putString("google_user_email", email)
        }
        try {
            auth.signInAnonymously()
                .addOnCompleteListener { _ ->
                    onResult(true, null)
                }
        } catch (_: Exception) {
            onResult(true, null)
        }
    }

    fun signInWithGoogleToken(idToken: String, onResult: (Boolean, String?) -> Unit) {
        prefs.edit { putBoolean("is_guest_mode", false) }
        try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Google sign-in failed")
                    }
                }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Error during Google sign-in")
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        try {
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onResult(true, null)
                    } else {
                        onResult(false, task.exception?.localizedMessage ?: "Failed to send reset email")
                    }
                }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Error sending reset email")
        }
    }

    fun signOut() {
        prefs.edit { 
            putBoolean("is_guest_mode", false)
            remove("google_user_email")
        }
        try {
            auth.signOut()
        } catch (_: Exception) {
        }
    }

    companion object {
        fun ensureFirebaseInitialized(context: Context) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:735277592813:android:a2097d7dbb3c09e8a39ce4")
                    .setApiKey("AIzaSyAJYhWF00VZfgefY7v9X6Vqf1THB2dHlho")
                    .setProjectId("fir-authtester-a4030")
                    .setStorageBucket("fir-authtester-a4030.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }
        }
    }
}
