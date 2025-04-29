@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gestural_music_app.userinterface

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestural_music_app.data.Track
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val tracks by viewModel.tracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scrollState = rememberScrollState()
    val musicStatusMessage by viewModel.musicStatusMessage.collectAsState()

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestural Music App") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
            musicStatusMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = message)
                }
            }
        },
        bottomBar = {
            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Wyloguj się")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Kamera
            if (cameraPermissionState.status.isGranted) {
                CameraPreview(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    onGestureDetected = { gestureType, confidence ->
                        viewModel.handleGesture(gestureType, confidence)
                    }
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
                TrackInfo(track, isPlaying, onPlayPauseClick = {
                    viewModel.togglePlayPause()
                })
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
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
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("✋ - Odtwórz/Pauza", fontSize = 12.sp)
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
                onTrackSelect = { viewModel.selectTrack(it) }
            )
        }
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
