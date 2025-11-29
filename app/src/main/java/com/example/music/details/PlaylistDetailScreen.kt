package com.example.music.ui.details

import androidx.compose.material.icons.filled.List
import android.net.Uri // <--- ¡IMPORTACIÓN FALTANTE AÑADIDA!
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.music.ui.library.AudioFile
import com.example.music.ui.library.AudioItem
import com.example.music.ui.library.Playlist
import com.example.music.ui.viewmodel.MusicPlayerViewModel

private val BrandOrange = Color(0xFFFF5722)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MusicPlayerViewModel = viewModel(),
    navController: NavController
) {
    // --- LÓGICA TEMPORAL (Mocks) ---
    // Lista de Playlists de prueba de LibraryScreen
    val mockPlaylists = remember {
        listOf(
            Playlist(1, "Mis Favoritas", listOf(101, 102, 103)),
            Playlist(2, "Rock Clásico", listOf(104, 105)),
            Playlist(3, "Para Correr", listOf(106, 107, 108, 109))
        )
    }

    // Buscamos la playlist
    val playlist = mockPlaylists.find { it.id == playlistId }
    val playlistName = playlist?.name ?: "Lista Desconocida"
    val songIds = playlist?.songIds ?: emptyList()

    // Mock de AudioFiles
    val mockAudioFiles = remember {
        listOf(
            AudioFile(101, "Canción A", "Artista X", "Álbum 1", Uri.EMPTY, 180000, 1, ""),
            AudioFile(102, "Canción B", "Artista Y", "Álbum 2", Uri.EMPTY, 240000, 2, ""),
            AudioFile(103, "Canción C", "Artista X", "Álbum 1", Uri.EMPTY, 200000, 1, ""),
            AudioFile(104, "Canción D", "Artista Z", "Álbum 3", Uri.EMPTY, 300000, 3, ""),
            AudioFile(105, "Canción E", "Artista Y", "Álbum 2", Uri.EMPTY, 220000, 2, ""),
        )
    }

    val playlistSongs = remember(songIds) {
        mockAudioFiles.filter { it.id in songIds }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlistName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandOrange,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- HEADER DE LA PLAYLIST ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Icono grande de Playlist
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandOrange,
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = playlistName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${playlistSongs.size} canciones",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                }
            }

            // --- BOTÓN DE REPRODUCCIÓN ---
            Button(
                onClick = {
                    if (playlistSongs.isNotEmpty()) {
                        viewModel.playSong(playlistSongs.first(), playlistSongs)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir Lista")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reproducir todo")
            }

            // --- LISTA DE CANCIONES (CORREGIDA) ---
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(playlistSongs, key = { it.id }) { audio ->
                    // Reutilizamos el AudioItem, pasando los parámetros de favoritos
                    AudioItem(
                        audioFile = audio,
                        isFavorite = viewModel.isFavorite(audio.id), // <-- CORRECCIÓN
                        onFavoriteToggle = { viewModel.toggleFavorite(audio.id) }, // <-- CORRECCIÓN
                        onClick = {
                            viewModel.playSong(audio, playlistSongs)
                        }
                    )
                }
            }
        }
    }
}