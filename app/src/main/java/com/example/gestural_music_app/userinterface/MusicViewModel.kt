package com.example.gestural_music_app.userinterface

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestural_music_app.audio.AudioController
import com.example.gestural_music_app.data.SoundHelixRepository
import com.example.gestural_music_app.data.Track
import com.example.gestural_music_app.gesture.GestureRecognizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MusicViewModel"
    private val repository = SoundHelixRepository()
    private val audioController = AudioController(application)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _musicStatusMessage = MutableStateFlow<String?>(null)
    val musicStatusMessage: StateFlow<String?> = _musicStatusMessage.asStateFlow()

    private var currentPitch = 1.0f
    private val minPitch = 0.4f
    private val maxPitch = 2.0f

    private var repeatJob: Job? = null
    private var lastGesture: GestureRecognizer.GestureType = GestureRecognizer.GestureType.NONE

    init {
        loadTracks()
    }

    private fun loadTracks() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d(TAG, "Loading tracks from SoundHelix")
                val trackList = repository.getTracks()
                Log.d(TAG, "Loaded ${trackList.size} tracks")
                _tracks.value = trackList
                if (trackList.isNotEmpty()) {
                    selectTrack(trackList.first())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tracks: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectTrack(track: Track) {
        val currentlyPlaying = _isPlaying.value
        _currentTrack.value = track
        audioController.loadTrack(track)
        _isPlaying.value = currentlyPlaying
        _musicStatusMessage.value = "Wybrano utwór: ${track.title}"
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }

    }

    fun play() {
        audioController.play()
        _isPlaying.value = true
        _musicStatusMessage.value = "Odtwarzanie wznowione"

    }

    fun pause() {
        audioController.pause()
        _isPlaying.value = false
        _musicStatusMessage.value = "Odtwarzanie zatrzymane"

    }

    fun increaseVolume() {
        audioController.increaseVolume()
        _musicStatusMessage.value = "Głośność zwiększona"
    }

    fun decreaseVolume() {
        audioController.decreaseVolume()
        _musicStatusMessage.value = "Głośność zmniejszona"
    }

    fun increasePitch() {
        currentPitch = (currentPitch + 0.2f).coerceAtMost(maxPitch)
        audioController.adjustPitch(currentPitch)
        _musicStatusMessage.value = "Pitch: %.2f".format(currentPitch)
    }

    fun decreasePitch() {
        currentPitch = (currentPitch - 0.2f).coerceAtLeast(minPitch)
        audioController.adjustPitch(currentPitch)
        _musicStatusMessage.value = "Pitch: %.2f".format(currentPitch)
    }

    fun handleGesture(gestureType: GestureRecognizer.GestureType, confidence: Float) {
        if (gestureType != lastGesture || gestureType == GestureRecognizer.GestureType.NONE) {
            stopRepeating()
            lastGesture = gestureType
        }
        when (gestureType) {
            GestureRecognizer.GestureType.THUMB_UP -> startRepeating { increaseVolume() }
            GestureRecognizer.GestureType.THUMB_DOWN -> startRepeating { decreaseVolume() }
            GestureRecognizer.GestureType.POINTING_UP -> startRepeating { increasePitch() }
            GestureRecognizer.GestureType.VICTORY -> startRepeating { decreasePitch() }
            GestureRecognizer.GestureType.OPEN_PALM -> {
                stopRepeating()
                if (!_isPlaying.value) play()
            }
            GestureRecognizer.GestureType.CLOSED_FIST -> {
                stopRepeating()
                if (_isPlaying.value) pause()
            }
            GestureRecognizer.GestureType.NONE -> stopRepeating()

            GestureRecognizer.GestureType.ILOVEYOU -> {
                // Przełącz na następny utwór
                skipToNextTrack()
            }

        }
    }

    private fun startRepeating(action: () -> Unit) {
        if (repeatJob?.isActive == true) return
        repeatJob = viewModelScope.launch {
            while (isActive) {
                action()
                delay(300)
            }
        }
    }


    private fun stopRepeating() {
        repeatJob?.cancel()
        repeatJob = null
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
        stopRepeating()
    }
    fun skipToNextTrack() {
        viewModelScope.launch {
            val currentIndex = tracks.value.indexOfFirst { it.id == currentTrack.value?.id }
            if (currentIndex != -1 && currentIndex < tracks.value.size - 1) {
                // Wybierz następny utwór
                val nextTrack = tracks.value[currentIndex + 1]

                // Załaduj i odtwórz następny utwór
                audioController.loadTrack(nextTrack)
                _currentTrack.value = nextTrack

                // Zawsze odtwarzaj po przełączeniu, nawet jeśli poprzedni był zatrzymany
                audioController.play()
                _isPlaying.value = true

                // Wyświetl komunikat
                _musicStatusMessage.value = "Przełączono na: ${nextTrack.title}"

                // Wyczyść komunikat po 3 sekundach
                delay(3000)
                if (_musicStatusMessage.value == "Przełączono na: ${nextTrack.title}") {
                    _musicStatusMessage.value = null
                }
            } else if (currentIndex == tracks.value.size - 1) {
                // Jeśli to ostatni utwór, wyświetl komunikat
                _musicStatusMessage.value = "To ostatni utwór na liście"
                delay(3000)
                if (_musicStatusMessage.value == "To ostatni utwór na liście") {
                    _musicStatusMessage.value = null
                }
            }
        }
    }


    fun showMessage(message: String) {
        _musicStatusMessage.value = message
    }
}
