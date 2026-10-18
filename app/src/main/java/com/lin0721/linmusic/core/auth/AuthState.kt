package com.lin0721.linmusic.core.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Clean data model representing an authenticated Firebase user.
 */
data class AuthUser(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)

/**
 * Unified authentication state backed exclusively by Firebase Auth.
 * Screens for Liked Songs, History, Stats, and Profile observe this state
 * to enforce user-scoped access without any dependency on NetEase sessions.
 */
open class AuthState(
    private val firebaseAuth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
) {
    private val _user = MutableStateFlow<AuthUser?>(
        runCatching { firebaseAuth?.currentUser?.toAuthUser() }.getOrNull()
    )
    open val user: StateFlow<AuthUser?> = _user.asStateFlow()

    open val currentUid: String?
        get() = _user.value?.uid

    open val currentUser: AuthUser?
        get() = _user.value

    open val isLoggedIn: Boolean
        get() = _user.value != null

    open val uidFlow: Flow<String?> = _user.map { it?.uid }

    init {
        firebaseAuth?.addAuthStateListener { auth ->
            _user.value = auth.currentUser?.toAuthUser()
        }
    }

    fun setTestUser(authUser: AuthUser?) {
        _user.value = authUser
    }

    open fun signOut() {
        firebaseAuth?.signOut()
        _user.value = null
    }

    companion object {
        fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
            uid = uid,
            email = email,
            displayName = displayName?.takeIf { it.isNotBlank() }
                ?: email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                ?: "Music Lover",
            photoUrl = photoUrl?.toString()?.takeIf { it.isNotBlank() }
        )
    }
}
