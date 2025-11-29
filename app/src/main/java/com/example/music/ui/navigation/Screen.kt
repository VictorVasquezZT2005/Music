package com.example.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Library : Screen("library", "Library", Icons.Default.LibraryMusic)
    object Permission : Screen("permission", "Permisos", Icons.Default.Lock)
    object Player : Screen("player", "Now Playing", Icons.Default.PlayCircle)

    // --- NUEVAS RUTAS CON PARÁMETROS ---
    object ArtistDetail : Screen("artist_detail/{artistName}", "Artista", Icons.Default.Person) {
        fun createRoute(artistName: String) = "artist_detail/$artistName"
    }
    object AlbumDetail : Screen("album_detail/{albumId}", "Álbum", Icons.Default.Album) {
        fun createRoute(albumId: Long) = "album_detail/$albumId"
    }
}