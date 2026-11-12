package com.example.recipeapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.recipeapp.MainActivity
import com.example.recipeapp.R
import com.example.recipeapp.data.auth.AuthManager
import com.example.recipeapp.databinding.ActivityAuthBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var authManager: AuthManager
    private lateinit var credentialManager: CredentialManager
    private var isSignUpMode = false

    companion object {
        private const val TAG = "AuthActivity"
        private const val WEB_CLIENT_ID = "735277592813-usvkhbu7bmd1khberdjd53ai1nhgtu3m.apps.googleusercontent.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.authScroll) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        authManager = AuthManager(this)
        credentialManager = CredentialManager.create(this)

        // If user already logged in, go directly to MainActivity
        if (authManager.isLoggedIn) {
            navigateToMain()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        binding.tvToggleMode.setOnClickListener {
            isSignUpMode = !isSignUpMode
            updateModeUI()
        }

        binding.btnSubmitAuth.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString()?.trim().orEmpty()

            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email"
                return@setOnClickListener
            } else {
                binding.tilEmail.error = null
            }

            if (password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            setLoading(true)
            binding.tvAuthError.visibility = View.GONE

            if (isSignUpMode) {
                authManager.signUp(email, password) { success, errorMsg ->
                    setLoading(false)
                    if (success) {
                        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                        navigateToMain()
                    } else {
                        showError(errorMsg ?: "Failed to sign up")
                    }
                }
            } else {
                authManager.signIn(email, password) { success, errorMsg ->
                    setLoading(false)
                    if (success) {
                        navigateToMain()
                    } else {
                        showError(errorMsg ?: "Invalid credentials")
                    }
                }
            }
        }

        // Google / Gmail Sign In
        binding.btnGoogleSignIn.setOnClickListener {
            val emailInput = binding.etEmail.text?.toString()?.trim().orEmpty()
            val googleEmail = if (emailInput.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                emailInput
            } else {
                "google.user@gmail.com"
            }
            binding.tvAuthError.visibility = View.GONE
            setLoading(true)
            authManager.signInFakeGoogle(googleEmail) { _, _ ->
                setLoading(false)
                Toast.makeText(this, "Signed in with Google ($googleEmail)", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
        }

        binding.btnGuestLogin.setOnClickListener {
            binding.tvAuthError.visibility = View.GONE
            setLoading(true)
            authManager.signInGuest { _, _ ->
                setLoading(false)
                navigateToMain()
            }
        }

        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Enter your email to reset password"
                return@setOnClickListener
            }
            binding.tilEmail.error = null
            setLoading(true)
            authManager.resetPassword(email) { success, errorMsg ->
                setLoading(false)
                if (success) {
                    Toast.makeText(this, getString(R.string.reset_password_sent), Toast.LENGTH_LONG).show()
                } else {
                    showError(errorMsg ?: "Failed to send reset email")
                }
            }
        }
    }

    private fun signInWithGoogle() {
        binding.tvAuthError.visibility = View.GONE
        setLoading(true)

        lifecycleScope.launch {
            // Step 1: Try with previously authorized accounts (fast path)
            val authorized = tryGetCredential(filterByAuthorized = true)
            if (authorized) return@launch

            // Step 2: Show full Google account picker
            tryGetCredential(filterByAuthorized = false)
        }
    }

    private suspend fun tryGetCredential(filterByAuthorized: Boolean): Boolean {
        return try {
            val credentialOption = if (filterByAuthorized) {
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(true)
                    .build()
            } else {
                GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID)
                    .build()
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(credentialOption)
                .build()

            val result = credentialManager.getCredential(this@AuthActivity, request)
            handleCredential(result.credential)
            true
        } catch (_: NoCredentialException) {
            Log.d(TAG, "No credential found (filterByAuthorized=$filterByAuthorized), trying picker")
            false
        } catch (e: GetCredentialCancellationException) {
            Log.d(TAG, "Sign-in cancelled by user: ${e.message}")
            setLoading(false)
            true
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException: ${e.message}")
            setLoading(false)
            showError("Google sign-in error: ${e.localizedMessage}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            setLoading(false)
            showError(e.localizedMessage ?: "Failed to get Google credentials")
            true
        }
    }

    private fun handleCredential(credential: androidx.credentials.Credential) {
        if ((credential is CustomCredential) &&
            (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                setLoading(false)
                showError("Could not parse Google ID token: ${e.message}")
            }
        } else {
            setLoading(false)
            showError("Unexpected credential type: ${credential.type}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        authManager.signInWithGoogleToken(idToken) { success, errorMsg ->
            setLoading(false)
            if (success) {
                Toast.makeText(this, "Signed in with Google!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            } else {
                showError(errorMsg ?: "Firebase authentication with Google failed")
            }
        }
    }

    private fun updateModeUI() {
        if (isSignUpMode) {
            binding.tvAuthTitle.setText(R.string.sign_up)
            binding.btnSubmitAuth.setText(R.string.sign_up)
            binding.tvToggleMode.setText(R.string.switch_to_login)
            binding.tvForgotPassword.visibility = View.GONE
        } else {
            binding.tvAuthTitle.setText(R.string.login_title)
            binding.btnSubmitAuth.setText(R.string.sign_in)
            binding.tvToggleMode.setText(R.string.switch_to_register)
            binding.tvForgotPassword.visibility = View.VISIBLE
        }
        binding.tvAuthError.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBarAuth.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSubmitAuth.isEnabled = !loading
        binding.btnGoogleSignIn.isEnabled = !loading
        binding.btnGuestLogin.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.tvAuthError.text = message
        binding.tvAuthError.visibility = View.VISIBLE
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
