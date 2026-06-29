package com.example.auth

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserSession(
    val userId: String,
    val displayName: String,
    val email: String,
    val isSubUser: Boolean = false,
    val ownerId: String = ""
)

class AuthManager(private val context: Context) {
    private var firebaseAuth: FirebaseAuth? = null
    private val prefs = context.getSharedPreferences("tailor_shop_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser

    init {
        try {
            var initialized = false
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    initialized = true
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Error checking initialized apps", e)
            }

            if (!initialized) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    Log.e("AuthManager", "Failed to initialize FirebaseApp with context", e)
                }
            }
            firebaseAuth = FirebaseAuth.getInstance()
            if (prefs.getBoolean("sub_user_active", false)) {
                _currentUser.value = UserSession(
                    userId = prefs.getString("sub_user_id", "") ?: "",
                    displayName = prefs.getString("sub_user_name", "") ?: "",
                    email = prefs.getString("sub_user_email", "") ?: "",
                    isSubUser = true,
                    ownerId = prefs.getString("sub_user_owner_id", "") ?: ""
                )
                Log.d("AuthManager", "Recovered sub-user session: ${_currentUser.value}")
                
                // Keep sub-user logged into Firebase network anonymously for background Firestore read/write sync success
                if (firebaseAuth?.currentUser == null) {
                    firebaseAuth?.signInAnonymously()?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("AuthManager", "Sub-user auto-signed in anonymously on recovery: ${task.result?.user?.uid}")
                        }
                    }
                }
            } else {
                firebaseAuth?.currentUser?.let { user ->
                    _currentUser.value = UserSession(
                        userId = user.uid,
                        displayName = user.displayName ?: "Tailor Partner",
                        email = user.email ?: ""
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Firebase or local auth not initialized", e)
        }
    }

    val isLoggedIn: Boolean
        get() = _currentUser.value != null

    // Set or recover customized sub-user session from manual credentials
    fun saveSubUserSession(session: UserSession) {
        prefs.edit().apply {
            putBoolean("sub_user_active", true)
            putString("sub_user_id", session.userId)
            putString("sub_user_name", session.displayName)
            putString("sub_user_email", session.email)
            putString("sub_user_owner_id", session.ownerId)
            apply()
        }
        _currentUser.value = session
        
        // Log in to Firebase network anonymously to bypass Firestore unauthenticated permission rules
        try {
            if (firebaseAuth?.currentUser == null) {
                firebaseAuth?.signInAnonymously()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AuthManager", "Sub-user signed in anonymously: ${task.result?.user?.uid}")
                    } else {
                        Log.e("AuthManager", "Sub-user anonymous register failed", task.exception)
                    }
                }
            }
        } catch (authEx: Exception) {
            Log.e("AuthManager", "Anonymous auth failed silently", authEx)
        }
    }

    // Google Sign in using actual Firebase credentials
    fun signInWithGoogle(idToken: String, onComplete: (Boolean, String?) -> Unit) {
        val auth = firebaseAuth
        if (auth != null) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        task.result?.user?.let { user ->
                            // When logging in with Google, clear any sub-user state
                            prefs.edit().apply {
                                putBoolean("sub_user_active", false)
                                remove("sub_user_id")
                                remove("sub_user_name")
                                remove("sub_user_email")
                                remove("sub_user_owner_id")
                                apply()
                            }
                            _currentUser.value = UserSession(
                                userId = user.uid,
                                displayName = user.displayName ?: "Tailor Partner",
                                email = user.email ?: ""
                            )
                            onComplete(true, null)
                        } ?: run {
                            onComplete(false, "No authenticated user session returned from Firebase")
                        }
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Unknown Firebase integration error"
                        Log.e("AuthManager", "Firebase authentication failure: $errMsg", task.exception)
                        onComplete(false, errMsg)
                    }
                }
        } else {
            onComplete(false, "Error: Firebase Authentication client is not initialized.")
        }
    }

    fun signOut() {
        firebaseAuth?.signOut()
        prefs.edit().apply {
            putBoolean("sub_user_active", false)
            remove("sub_user_id")
            remove("sub_user_name")
            remove("sub_user_email")
            remove("sub_user_owner_id")
            apply()
        }
        _currentUser.value = null
    }

    fun deleteAccount(onComplete: (Boolean, String?) -> Unit) {
        val user = firebaseAuth?.currentUser
        if (user != null) {
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        signOut()
                        onComplete(true, null)
                    } else {
                        val errMsg = task.exception?.localizedMessage ?: "Failed to delete account. You might need to sign out and sign in again to perform this action."
                        onComplete(false, errMsg)
                    }
                }
        } else {
            signOut()
            onComplete(true, null)
        }
    }
}
