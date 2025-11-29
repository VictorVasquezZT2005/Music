package com.example.music.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 1. Modelos de Datos (Lo que responde Deezer)
data class DeezerResponse<T>(val data: List<T>)
data class ArtistResult(val id: Long, val name: String, val picture_xl: String?)
data class AlbumResult(val id: Long, val title: String, val cover_xl: String?)

// 2. Interfaz de la API
interface DeezerApiService {
    @GET("search/artist")
    suspend fun searchArtist(@Query("q") query: String): DeezerResponse<ArtistResult>

    @GET("search/album")
    suspend fun searchAlbum(@Query("q") query: String): DeezerResponse<AlbumResult>
}

// 3. Objeto Singleton para usarlo fácilmente
object DeezerClient {
    private const val BASE_URL = "https://api.deezer.com/"

    val service: DeezerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApiService::class.java)
    }
}