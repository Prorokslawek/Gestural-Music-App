package com.example.gestural_music_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.gestural_music_app.login.LoginScreen
import com.example.gestural_music_app.userinterface.MainScreen
import androidx.compose.ui.Alignment
import com.example.gestural_music_app.ui.theme.GesturalMusicAppTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sprawdź, czy plik modelu istnieje
        try {
            val assetManager = assets
            val files = assetManager.list("")
            val modelExists = files?.contains("gesture_recognizer.task") ?: false
            Log.d("MainActivity", "Model exists: $modelExists")
            if (!modelExists) {
                Log.e(
                    "MainActivity",
                    "Model gesture_recognizer.task nie został znaleziony w assets!"
                )
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Błąd podczas sprawdzania pliku modelu", e)
        }

        setContent {
            GesturalMusicAppTheme {
                var isLoggedIn by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
                var isAuthChecked by remember { mutableStateOf(false) }

                DisposableEffect(Unit) {
                    val auth = FirebaseAuth.getInstance()
                    val listener = FirebaseAuth.AuthStateListener {
                        isLoggedIn = it.currentUser != null
                        isAuthChecked = true
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        !isAuthChecked -> {
                            // Loader, bo nie wiemy jeszcze, czy użytkownik jest zalogowany
                            Box(modifier = Modifier.fillMaxSize()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }

                        }

                        isLoggedIn -> {
                            MainScreen(
                                onLogout = {
                                    FirebaseAuth.getInstance().signOut()
                                }
                            )
                        }

                        else -> {
                            LoginScreen(
                                onLoginSuccess = { isLoggedIn = true },
                                onNavigateToRegister = { /* ... */ }
                            )
                        }
                    }
                }
            }
        }
    }
}