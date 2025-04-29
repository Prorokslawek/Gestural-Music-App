package com.example.gestural_music_app.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _loginState = MutableStateFlow<LoginState>(LoginState.IDLE)
    val loginState: StateFlow<LoginState> = _loginState

    fun login(email: String, password: String) {
        _loginState.value = LoginState.LOADING
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _loginState.value = if (task.isSuccessful) LoginState.SUCCESS
                else LoginState.ERROR(task.exception?.message ?: "Błąd logowania")
            }
    }
}

sealed class LoginState {
    object IDLE : LoginState()
    object LOADING : LoginState()
    object SUCCESS : LoginState()
    data class ERROR(val message: String) : LoginState()
}
