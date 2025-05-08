package com.example.gestural_music_app.audio

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.example.gestural_music_app.data.Track
import android.os.Handler
import android.os.Looper


class AudioController(private val context: Context) {
    private val TAG = "AudioController"
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var isPlaying = false


    init {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .build(),
            true
        )
    }

    fun loadTrack(track: Track) {
        Log.d(TAG, "Loading track: ${track.title}, URL: ${track.streamUrl}")
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri(track.streamUrl))
        player.prepare()
        // Nie auto-startuj, decyzja należy do ViewModel
    }

    fun play() {
        Handler(Looper.getMainLooper()).post {
            if (!player.isPlaying) {
                player.play()
                Log.d(TAG, "Started playback")
            }
        }
    }

    fun pause() {
        Handler(Looper.getMainLooper()).post {
            if (player.isPlaying) {
                player.pause()
                Log.d(TAG, "Paused playback")
            }
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) pause() else play()
    }

    fun increaseVolume() {
        // ExoPlayer nie steruje głośnością systemową, więc użyj AudioManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.adjustStreamVolume(
            android.media.AudioManager.STREAM_MUSIC,
            android.media.AudioManager.ADJUST_RAISE,
            0
        )
        Log.d(TAG, "Volume increased")
    }

    fun decreaseVolume() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        audioManager.adjustStreamVolume(
            android.media.AudioManager.STREAM_MUSIC,
            android.media.AudioManager.ADJUST_LOWER,
            0
        )
        Log.d(TAG, "Volume decreased")
    }

    fun adjustPitch(value: Float) {
        Handler(Looper.getMainLooper()).post {
            player.setPlaybackParameters(PlaybackParameters(1f, value))
            Log.d(TAG, "Pitch adjusted to $value")
        }
    }

    // Nowa metoda do sprawdzania czy odtwarzacz jest aktywny
    fun isPlayerActive(): Boolean {
        return player.isPlaying || player.isLoading || player.playbackState != androidx.media3.common.Player.STATE_IDLE
    }

    fun release() {
        player.release()
        Log.d(TAG, "ExoPlayer released")
    }
}
