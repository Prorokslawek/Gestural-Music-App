package com.example.gestural_music_app.login

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RegisterViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.IDLE)
    val registerState: StateFlow<RegisterState> = _registerState

    fun register(email: String, password: String) {
        _registerState.value = RegisterState.LOADING
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _registerState.value = if (task.isSuccessful) RegisterState.SUCCESS
                else RegisterState.ERROR(task.exception?.message ?: "Błąd rejestracji")
            }
    }
}

sealed class RegisterState {
    object IDLE : RegisterState()
    object LOADING : RegisterState()
    object SUCCESS : RegisterState()
    data class ERROR(val message: String) : RegisterState()
}
