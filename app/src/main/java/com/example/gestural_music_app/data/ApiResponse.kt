package com.example.gestural_music_app.data

data class ArtistResponse(
    val artists: List<Artist>? = null
)

data class AlbumResponse(
    val album: List<Album>? = null
)

data class TrackResponse(
    val track: List<ApiTrack>? = null
)

data class Artist(
    val idArtist: String,
    val strArtist: String,
    val strGenre: String? = null,
    val strBiographyEN: String? = null,
    val strArtistThumb: String? = null
)

data class Album(
    val idAlbum: String,
    val strAlbum: String,
    val strArtist: String,
    val intYearReleased: String? = null,
    val strAlbumThumb: String? = null
)

data class ApiTrack(
    val idTrack: String,
    val strTrack: String,
    val strArtist: String,
    val strAlbum: String? = null,
    val intDuration: String? = null,
    val strMusicVid: String? = null,
    val strTrackThumb: String? = null
) {
    // Metoda do konwersji na model używany w aplikacji
    fun toAppTrack(): com.example.gestural_music_app.data.Track {
        return com.example.gestural_music_app.data.Track(
            id = idTrack,
            title = strTrack,
            artist = strArtist,
            streamUrl = strMusicVid ?: "", // Uwaga: API nie zawsze zwraca URL do streamingu
            coverUrl = strTrackThumb
        )
    }
}
