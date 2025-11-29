package com.example.music.data.models

// Necesario importar Long si se usa esta clase sola
// Aunque en Kotlin/Compose suele inferirse bien si se usa en data class.

data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long> // Lista de IDs de canciones
)