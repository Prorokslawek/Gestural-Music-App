package com.example.gestural_music_app.login

import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LoginViewModel : ViewModel() {
    private val _loginState = MutableStateFlow<LoginState>(LoginState.IDLE)
    val loginState = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _loginState.value = LoginState.ERROR("Email i hasło nie mogą być puste")
            return
        }

        _loginState.value = LoginState.LOADING

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _loginState.value = LoginState.SUCCESS
            }
            .addOnFailureListener { exception ->
                // Mapuj różne błędy Firebase na przyjazne komunikaty
                val errorMessage = when (exception) {
                    is FirebaseAuthInvalidUserException -> "Użytkownik nie istnieje"
                    is FirebaseAuthInvalidCredentialsException -> "Nieprawidłowy email lub hasło"
                    is FirebaseAuthUserCollisionException -> "Konto już istnieje"
                    is FirebaseNetworkException -> "Problem z połączeniem internetowym"
                    else -> "Błąd logowania: ${exception.message}"
                }
                _loginState.value = LoginState.ERROR(errorMessage)
            }
    }

    fun resetState() {
        _loginState.value = LoginState.IDLE
    }
}

sealed class LoginState {
    object IDLE : LoginState()
    object LOADING : LoginState()
    object SUCCESS : LoginState()
    data class ERROR(val message: String) : LoginState()
}

