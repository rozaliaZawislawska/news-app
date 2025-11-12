package com.basic.newsapp.viewModel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class AuthViewModel : ViewModel() {
    val isLoggedIn = mutableStateOf(Firebase.auth.currentUser != null)
    val isLoginMode = mutableStateOf(true)
    var email = Firebase.auth.currentUser?.email ?: "Unknown"

    fun changeAuthMode() {
        isLoginMode.value = !isLoginMode.value
    }

    fun signIn(email: String, password: String) {
        this.email = email
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isLoggedIn.value = true
                } else {
                    isLoggedIn.value = false
                }
            }
    }

    fun signUp(email: String, password: String) {
        this.email = email
        try {
            Firebase.auth.createUserWithEmailAndPassword(email, password)
            isLoggedIn.value = true
        } catch (e: Exception) {
            isLoggedIn.value = false
        }
    }

    fun signOut() {
        Firebase.auth.signOut()
        email = "Unkonwn"
        isLoggedIn.value = false
    }
}