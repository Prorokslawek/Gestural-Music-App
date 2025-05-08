package com.example.gestural_music_app.record_music

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestural_music_app.database.AppDatabase
import com.example.gestural_music_app.database.Recording
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class RecordingViewModel : ViewModel() {
    private var database: AppDatabase? = null
    private val _recordings = MutableStateFlow<List<Recording>>(emptyList())
    val recordings = _recordings.asStateFlow()
    private var mediaPlayer: MediaPlayer? = null

    fun initDatabase(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
            loadRecordings()
        }
    }

    private fun loadRecordings() {
        viewModelScope.launch(Dispatchers.IO) {
            database?.recordingDao()?.getAllRecordings()?.collect { recordingsList ->
                _recordings.value = recordingsList
            }
        }
    }

    fun insertRecording(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            database?.recordingDao()?.insertRecording(recording)
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch(Dispatchers.IO) {
            // Usuń plik z dysku
            try {
                val file = File(recording.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Usuń wpis z bazy danych
            database?.recordingDao()?.deleteRecording(recording)
        }
    }

    fun playRecording(context: Context, filePath: String) {
        // Zatrzymaj aktualnie odtwarzany dźwięk
        mediaPlayer?.release()

        // Odtwórz nowy dźwięk
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun sendRecordingByEmail(context: Context, recording: Recording) {
        viewModelScope.launch {
            try {
                // Pobierz email zalogowanego użytkownika
                val userEmail = FirebaseAuth.getInstance().currentUser?.email

                if (userEmail != null) {
                    // Utwórz intent do wysłania emaila
                    val emailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "audio/*"
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
                        putExtra(Intent.EXTRA_SUBJECT, "Nagranie: ${recording.filename}")
                        putExtra(Intent.EXTRA_TEXT, "Oto Twoje nagranie z aplikacji Gestural Music App.")

                        // Załącz plik nagrania
                        val file = File(recording.filePath)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        putExtra(Intent.EXTRA_STREAM, uri)

                        // Dodaj flagi, aby aplikacja email mogła odczytać plik
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    // Uruchom intent
                    context.startActivity(Intent.createChooser(emailIntent, "Wyślij nagranie przez:"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
