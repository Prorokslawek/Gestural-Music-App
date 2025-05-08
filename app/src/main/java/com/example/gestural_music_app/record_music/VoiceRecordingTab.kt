package com.example.gestural_music_app.record_music

import android.Manifest
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Email
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestural_music_app.database.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun VoiceRecordingTab(
    paddingValues: PaddingValues = PaddingValues(),
    recordingViewModel: RecordingViewModel = viewModel()
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder: MediaRecorder? by remember { mutableStateOf(null) }
    var outputFilePath by remember { mutableStateOf("") }
    val recordings by recordingViewModel.recordings.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dodaj zmienną do śledzenia numeru nagrania
    var recordingNumber by remember { mutableStateOf(recordings.size + 1) }

    // Aktualizuj numer nagrania, gdy lista nagrań się zmienia
    LaunchedEffect(recordings) {
        recordingNumber = recordings.size + 1
    }

    // Inicjalizacja bazy danych
    LaunchedEffect(Unit) {
        recordingViewModel.initDatabase(context)
    }

    // Definiujemy funkcję startRecording jako val przed jej użyciem
    val startRecording = { ctx: android.content.Context, onStarted: (String) -> Unit ->
        val fileName = "nagranie_${recordingNumber}.3gp"
        val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)

        // Sprawdź, czy plik już istnieje - jeśli tak, dodaj unikalny timestamp
        if (file.exists()) {
            val timestamp = System.currentTimeMillis()
            val fileName = "nagranie_${recordingNumber}_${timestamp}.3gp"
            val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName)
        }

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(ctx)
        } else {
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = recorder
        onStarted(file.absolutePath)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                startRecording(context) { path ->
                    outputFilePath = path
                    isRecording = true
                }
            } else {
                // Permission denied
            }
        }
    )

    // Funkcja do obliczania czasu trwania nagrania
    fun calculateDuration(filePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaRecorder = null
        isRecording = false

        // Zapisz nagranie do bazy danych
        if (outputFilePath.isNotEmpty()) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Oblicz czas trwania nagrania
                    val duration = calculateDuration(outputFilePath)
                    val fileName = File(outputFilePath).name

                    // Utwórz obiekt Recording
                    val recording = Recording(
                        filename = fileName,
                        filePath = outputFilePath,
                        duration = duration
                    )

                    // Zapisz do bazy danych
                    recordingViewModel.insertRecording(recording)

                    // Zwiększ licznik nagrań
                    recordingNumber++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Przycisk nagrywania
            Button(
                onClick = {
                    if (isRecording) {
                        stopRecording()
                    } else {
                        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        if (permissionStatus == PermissionChecker.PERMISSION_GRANTED) {
                            startRecording(context) { path ->
                                outputFilePath = path
                                isRecording = true
                            }
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRecording) "Zatrzymaj nagrywanie" else "Rozpocznij nagrywanie")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isRecording) {
                Text("Nagrywanie w toku...", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lista nagrań
            Text(
                "Twoje nagrania:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (recordings.isEmpty()) {
                Text(
                    "Brak nagrań",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(recordings) { recording ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recording.filename,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row {
                                        Text(
                                            text = formatDuration(recording.duration),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = formatDate(recording.timestamp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(onClick = {
                                    recordingViewModel.playRecording(context, recording.filePath)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Odtwarzam ${recording.filename}")
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Odtwórz",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(onClick = {
                                    recordingViewModel.sendRecordingByEmail(context, recording)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Wysyłam ${recording.filename} na email")
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Wyślij email",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        recordingViewModel.deleteRecording(recording)
                                        snackbarHostState.showSnackbar("Usunięto ${recording.filename}")


                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Usuń",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SnackbarHost na dole ekranu
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

// Funkcje pomocnicze
fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
            TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%02d:%02d", minutes, seconds)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
