package com.example.gestural_music_app.data

class SoundHelixRepository {

    fun getTracks(): List<Track> {
        val tracks = mutableListOf<Track>()

        // Dodaj 10 utworów z SoundHelix
        for (i in 1..10) {
            tracks.add(
                Track(
                    id = i.toString(),
                    title = "Song $i",
                    artist = "SoundHelix",
                    streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-$i.mp3"
                )
            )
        }

        return tracks
    }
}
