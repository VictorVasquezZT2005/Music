package com.example.music.ui.details

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter // Aseguramos la importación del painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.music.api.DeezerClient
import com.example.music.ui.library.AudioFile
import com.example.music.ui.library.AudioItem
import com.example.music.ui.library.getAudioFiles
import com.example.music.ui.viewmodel.MusicPlayerViewModel
import com.example.music.utils.ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BrandOrange = Color(0xFFFF5722)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MusicPlayerViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var allSongs by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    val albumSongs = remember(allSongs, albumId) { allSongs.filter { it.albumId == albumId } }

    val firstSong = albumSongs.firstOrNull()
    val albumName = firstSong?.albumName ?: "Álbum"
    val artistName = firstSong?.artist ?: "Artista"

    var albumCoverUrl by remember { mutableStateOf<String?>(null) }
    val imageCache = remember { ImageCache(context) }

    // Definimos el Painter por defecto para el placeholder/error
    val defaultAlbumPainter = rememberVectorPainter(Icons.Default.Album)


    LaunchedEffect(Unit) {
        allSongs = getAudioFiles(context)
    }

    // Lógica Caché para Álbum
    LaunchedEffect(albumName) {
        if (albumName != "Álbum") {
            val cacheKey = "album_${albumName}_${artistName}"

            // 1. Buscar en caché
            val cachedUrl = imageCache.getUrl(cacheKey)

            if (cachedUrl != null) {
                albumCoverUrl = cachedUrl
            } else {
                // 2. Buscar en API
                try {
                    val response = withContext(Dispatchers.IO) {
                        DeezerClient.service.searchAlbum("$albumName $artistName")
                    }
                    if (response.data.isNotEmpty()) {
                        val urlFound = response.data[0].cover_xl
                        if (urlFound != null) {
                            imageCache.saveUrl(cacheKey, urlFound)
                            albumCoverUrl = urlFound
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Decisión final sobre el modelo de imagen a cargar
    val finalImageModel = albumCoverUrl ?: firstSong?.albumArtUri

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // NAVBAR (TopAppBar)
        TopAppBar(
            title = {
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BrandOrange,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        // CONTENIDO
        if (albumSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 150.dp)) {
                // IMAGEN DEL ÁLBUM (Bloque Corregido)
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(finalImageModel)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "Portada",
                            contentScale = ContentScale.Crop,
                            // Usamos placeholder y error con el Painter
                            placeholder = defaultAlbumPainter,
                            error = defaultAlbumPainter,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.LightGray) // Fondo gris para el placeholder/error
                        )

                        // Botón de Play
                        FloatingActionButton(
                            onClick = { viewModel.playSong(albumSongs.first(), albumSongs) },
                            containerColor = BrandOrange,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp).size(64.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp))
                        }
                    }
                }

                // INFO
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = albumName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                        Text(text = artistName, style = MaterialTheme.typography.titleMedium, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }

                // HEADER
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Lista de canciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Surface(color = BrandOrange, shape = RoundedCornerShape(4.dp)) {
                            Text(text = "${albumSongs.size} PISTAS", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }

                // LISTA CORREGIDA
                items(items = albumSongs, key = { it.id }) { song ->
                    AudioItem(
                        audioFile = song,
                        isFavorite = viewModel.isFavorite(song.id),
                        onFavoriteToggle = { viewModel.toggleFavorite(song.id) },
                        onClick = { viewModel.playSong(song, albumSongs) }
                    )
                }
            }
        }
    }
}