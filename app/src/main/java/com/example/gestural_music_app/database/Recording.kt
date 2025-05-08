package com.example.gestural_music_app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val filename: String,
    val filePath: String,
    val duration: Long,
    val timestamp: Long = System.currentTimeMillis()
)