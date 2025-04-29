package com.example.gestural_music_app.data

data class Track(
    val id: String,
    val title: String,
    val artist: String = "SoundHelix",
    val streamUrl: String,
    val coverUrl: String? = null
)
