package com.example.music.ui.details

import androidx.compose.material.icons.filled.List
import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.music.ui.library.AudioFile
import com.example.music.ui.library.AudioItem
import com.example.music.ui.library.getAudioFiles
import com.example.music.ui.viewmodel.MusicPlayerViewModel
import com.example.music.data.models.Playlist // <--- ¡LA IMPORTACIÓN CRÍTICA QUE FALTABA!

private val BrandOrange = Color(0xFFFF5722)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MusicPlayerViewModel = viewModel(),
    navController: NavController
) {
    val context = LocalContext.current

    // Estado para almacenar TODAS las canciones reales
    var allSongs by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // 1. Cargar la lista completa de canciones locales al iniciar
    LaunchedEffect(Unit) {
        allSongs = getAudioFiles(context)
        isLoading = false
    }

    // --- MOCKS DE PLAYLISTS (Manteniendo la estructura temporal) ---
    // Usamos el ID 1 para "Mis Favoritas" y obtenemos los IDs reales del ViewModel
    val favoriteSongsIds = viewModel.favoriteSongIds.toList()

    val mockPlaylists = remember(favoriteSongsIds) {
        listOf(
            // La playlist "Mis Favoritas" usa los IDs reales cargados
            Playlist(1, "Mis Favoritas", favoriteSongsIds),
            // Otras playlists (para demostrar el filtro, si existen en tu música local)
            Playlist(2, "Rock Clásico", listOf(104, 105)),
            Playlist(3, "Para Correr", listOf(106, 107, 108, 109))
        )
    }

    // 2. Buscar la playlist y obtener sus IDs
    val playlist = mockPlaylists.find { it.id == playlistId }
    val playlistName = playlist?.name ?: "Lista Desconocida"
    val songIds = playlist?.songIds ?: emptyList()

    // 3. Filtrar las canciones REALES por los IDs de la playlist
    val playlistSongs = remember(allSongs, songIds) {
        // Mapeamos los IDs de la playlist a los objetos AudioFile reales
        // Usamos un mapa para una búsqueda más eficiente (O(1))
        val songMap = allSongs.associateBy { it.id }
        songIds.mapNotNull { songMap[it] }
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

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandOrange)
                }
            } else if (playlistSongs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Esta lista está vacía.", color = Color.Gray)
                }
            } else {
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

                // --- LISTA DE CANCIONES ---
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 150.dp)) {
                    items(playlistSongs, key = { it.id }) { audio ->
                        AudioItem(
                            audioFile = audio,
                            isFavorite = viewModel.isFavorite(audio.id),
                            onFavoriteToggle = { viewModel.toggleFavorite(audio.id) },
                            onClick = {
                                viewModel.playSong(audio, playlistSongs)
                            }
                        )
                    }
                }
            }
        }
    }
}