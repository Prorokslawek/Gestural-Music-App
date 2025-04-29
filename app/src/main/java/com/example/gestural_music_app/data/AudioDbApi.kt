package com.example.gestural_music_app.data

import retrofit2.http.GET
import retrofit2.http.Query

interface AudioDbApi {
    @GET("search.php")
    suspend fun searchArtist(@Query("s") artistName: String): ArtistResponse

    @GET("album.php")
    suspend fun getArtistAlbums(@Query("i") artistId: String): AlbumResponse

    @GET("track.php")
    suspend fun getAlbumTracks(@Query("m") albumId: String): TrackResponse

    @GET("track-top10.php")
    suspend fun getArtistTopTracks(@Query("s") artistName: String): TrackResponse

    @GET("mostloved.php")
    suspend fun getMostLovedTracks(@Query("format") format: String = "track"): TrackResponse
}
