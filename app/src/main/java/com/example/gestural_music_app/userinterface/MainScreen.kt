@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gestural_music_app.userinterface

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestural_music_app.data.Track
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.firebase.auth.FirebaseAuth
import android.webkit.WebStorage
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.gestural_music_app.gesture.GestureRecognizer
import com.example.gestural_music_app.login.LoginScreen
import com.example.gestural_music_app.register.RegisterScreen
import com.example.gestural_music_app.record_music.VoiceRecordingTab
import com.google.accompanist.permissions.PermissionState
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import com.example.gestural_music_app.R


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel = viewModel()
) {
    val tracks by viewModel.tracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scrollState = rememberScrollState()
    val musicStatusMessage by viewModel.musicStatusMessage.collectAsState()
    val context = LocalContext.current

    // Sprawdzanie stanu logowania
    var isUserLoggedIn by remember { mutableStateOf(false) }
    var showLoginScreen by rememberSaveable { mutableStateOf(false) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    // Dodajemy stan dla zakładek
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Muzyka", "Nagrywanie")
    var showRegisterScreen by rememberSaveable { mutableStateOf(false) }

    // Dodajemy SharedPreferences do śledzenia stanu wylogowania
    val sharedPrefs = remember { context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE) }

    // Ustaw tryb persystencji Firebase na SESSION zamiast LOCAL


    // Sprawdź czy użytkownik został wcześniej wylogowany
    LaunchedEffect(Unit) {
        val wasLoggedOut = sharedPrefs.getBoolean("was_logged_out", false)
        if (wasLoggedOut) {
            // Wymuś wylogowanie przy starcie
            FirebaseAuth.getInstance().signOut()
            WebStorage.getInstance().deleteAllData()
            isUserLoggedIn = false
        }

        // Opóźnij sprawdzenie stanu logowania, aby uniknąć migotania
        delay(300)
        isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null
        isCheckingAuth = false
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    // Obserwuj zmiany stanu zalogowania
    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener {
            isUserLoggedIn = it.currentUser != null
            // Jeśli użytkownik jest wylogowany, zapisz tę informację
            if (it.currentUser == null) {
                sharedPrefs.edit().putBoolean("was_logged_out", true).apply()
            } else {
                sharedPrefs.edit().putBoolean("was_logged_out", false).apply()
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Jeśli użytkownik próbuje przejść do zakładki Nagrywanie, ale nie jest zalogowany
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && !isCheckingAuth) {
            // Dodaj opóźnienie przed sprawdzeniem stanu logowania
            delay(1)

            // Sprawdź stan wylogowania z SharedPreferences
            val forceLogout = sharedPrefs.getBoolean("force_logout", false)

            // Sprawdź aktualny stan zalogowania
            val currentUser = FirebaseAuth.getInstance().currentUser
            isUserLoggedIn = currentUser != null && !forceLogout

            if (!isUserLoggedIn) {
                showLoginScreen = true
                // Zapisz, że użytkownik nie jest zalogowany
                sharedPrefs.edit().putBoolean("was_logged_out", true).apply()
            }
        }
    }

    // Funkcja do bezpiecznego wylogowania
    val performLogout = {
        // Najpierw przełącz na zakładkę Muzyka
        selectedTab = 0

        // Wyloguj z Firebase
        FirebaseAuth.getInstance().signOut()

        // Wyczyść dane sesji
        WebStorage.getInstance().deleteAllData()

        // Wyczyść cookies
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()

        // Zapisz stan wylogowania
        sharedPrefs.edit()
            .putBoolean("was_logged_out", true)
            .putBoolean("force_logout", true)
            .apply()

        // Aktualizuj stan zalogowania
        isUserLoggedIn = false

        // Wyświetl komunikat
        viewModel.showMessage("Pomyślnie wylogowano")
    }

    // Warunki wyświetlania ekranów
    if (showLoginScreen) {
        LoginScreen(
            onLoginSuccess = {
                isUserLoggedIn = true
                showLoginScreen = false
                // Zapisz, że użytkownik jest zalogowany
                sharedPrefs.edit()
                    .putBoolean("was_logged_out", false)
                    .putBoolean("force_logout", false)
                    .apply()
                // Przełącz na zakładkę nagrywania po zalogowaniu
                selectedTab = 1
            },
            onNavigateToRegister = {
                showRegisterScreen = true
                showLoginScreen = false
            },
            onCancel = {
                showLoginScreen = false
                selectedTab = 0  // Wróć do zakładki Muzyka
            }
        )
    } else if (showRegisterScreen) {
        RegisterScreen(
            onRegisterSuccess = {
                showRegisterScreen = false
                isUserLoggedIn = true
                // Zapisz, że użytkownik jest zalogowany
                sharedPrefs.edit()
                    .putBoolean("was_logged_out", false)
                    .putBoolean("force_logout", false)
                    .apply()
                selectedTab = 1
            },
            onNavigateToLogin = {
                showRegisterScreen = false
                showLoginScreen = true
            }
        )
    } else {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sprawdź szerokość ekranu
                                val configuration = LocalConfiguration.current
                                val screenWidth = configuration.screenWidthDp

                                // Dostosuj rozmiar tekstu w zależności od szerokości ekranu
                                val textStyle = if (screenWidth < 360) {
                                    MaterialTheme.typography.titleSmall
                                } else {
                                    MaterialTheme.typography.titleMedium
                                }

                                Text(
                                    "Gestural Music App",
                                    style = textStyle
                                )

                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        actions = {
                            // Przycisk wylogowania w AppBar, tylko jeśli użytkownik jest zalogowany
                            if (isUserLoggedIn) {
                                IconButton(onClick = { performLogout() }) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Wyloguj się"
                                    )
                                }
                            }
                        }
                    )

                    // Dodajemy TabRow do przełączania między zakładkami
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = {
                                    // Sprawdź, czy użytkownik może przejść do zakładki Nagrywanie
                                    if (index == 1) {
                                        // Sprawdź stan wylogowania z SharedPreferences
                                        val forceLogout = sharedPrefs.getBoolean("force_logout", false)

                                        // Sprawdź aktualny stan zalogowania
                                        val currentUser = FirebaseAuth.getInstance().currentUser
                                        isUserLoggedIn = currentUser != null && !forceLogout

                                        // Jeśli użytkownik został wylogowany lub wymuszono wylogowanie
                                        if (!isUserLoggedIn) {
                                            showLoginScreen = true
                                        } else {
                                            selectedTab = index
                                        }
                                    } else {
                                        selectedTab = index
                                    }
                                },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            // Wyświetlamy odpowiednią zawartość w zależności od wybranej zakładki
            when (selectedTab) {
                0 -> MusicContent(
                    tracks = tracks,
                    currentTrack = currentTrack,
                    isPlaying = isPlaying,
                    isLoading = isLoading,
                    cameraPermissionState = cameraPermissionState,
                    scrollState = scrollState,
                    paddingValues = paddingValues,
                    onGestureDetected = { gestureType, confidence ->
                        viewModel.handleGesture(gestureType, confidence)
                    },
                    onPlayPauseClick = {
                        viewModel.togglePlayPause()
                    },
                    onTrackSelect = { viewModel.selectTrack(it) },
                    musicStatusMessage = musicStatusMessage
                )
                1 -> VoiceRecordingTab(paddingValues = paddingValues)
            }
        }
    }
}




@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicContent(
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    isLoading: Boolean,
    cameraPermissionState: PermissionState,
    scrollState: ScrollState,
    paddingValues: PaddingValues,
    onGestureDetected: (GestureRecognizer.GestureType, Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onTrackSelect: (Track) -> Unit,
    musicStatusMessage: String? = null  // Dodaj ten parametr
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {


        // Następnie kamera
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onGestureDetected = onGestureDetected
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Wymagane uprawnienie do kamery")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text("Udziel uprawnień")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Informacje o utworze
        currentTrack?.let { track ->
            TrackInfo(track, isPlaying, onPlayPauseClick = onPlayPauseClick)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // komunikat muzyczny
        musicStatusMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(text = message)
            }
        }

        // Gesty sterujące
        Text(
            "Gesty sterujące:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("👍 - Zwiększ głośność", fontSize = 12.sp)
                Text("👎 - Zmniejsz głośność", fontSize = 12.sp)
                Text("❤️ - Następny utwór", fontSize = 12.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("✋ - Odtwórz", fontSize = 12.sp)
                Text("✊ - Pauza", fontSize = 12.sp)
                Text("☝️ - Podwyższ ton", fontSize = 12.sp)
                Text("✌️ - Obniż ton", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista utworów
        Text(
            "Dostępne utwory:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        TrackList(
            tracks = tracks,
            currentTrack = currentTrack,
            onTrackSelect = onTrackSelect
        )
    }
}

@Composable
fun TrackInfo(track: Track, isPlaying: Boolean, onPlayPauseClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isPlaying) "Pauza" else "Odtwórz")
            }
        }
    }
}

@Composable
fun TrackList(
    tracks: List<Track>,
    currentTrack: Track?,
    onTrackSelect: (Track) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        tracks.forEach { track ->
            val isSelected = track.id == currentTrack?.id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackSelect(track) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Aktualnie odtwarzany",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Divider()
        }
    }
}

