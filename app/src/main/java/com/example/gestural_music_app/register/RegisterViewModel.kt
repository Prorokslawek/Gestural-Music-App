package com.example.gestural_music_app.register

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterViewModel : ViewModel() {
    fun registerUser(
        email: String,
        password: String,
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                // Zapisz dodatkowe dane użytkownika
                val user = FirebaseAuth.getInstance().currentUser
                val userProfile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user?.updateProfile(userProfile)
                    ?.addOnSuccessListener {
                        onSuccess()
                    }
                    ?.addOnFailureListener { e ->
                        onError(e.message ?: "Błąd podczas aktualizacji profilu")
                    }
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Błąd podczas rejestracji")
            }
    }
}
