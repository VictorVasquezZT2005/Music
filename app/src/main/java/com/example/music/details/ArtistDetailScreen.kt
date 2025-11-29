package com.example.music.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.music.utils.ImageCache // Asegúrate de importar tu nueva clase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val BrandOrange = Color(0xFFFF5722)

@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: MusicPlayerViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var allSongs by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    val artistSongs = remember(allSongs, artistName) {
        allSongs.filter { it.artist == artistName }
    }

    // Estado de la imagen
    var artistImageUrl by remember { mutableStateOf<String?>(null) }

    // Instancia del Caché
    val imageCache = remember { ImageCache(context) }

    // 1. Cargar canciones locales
    LaunchedEffect(Unit) {
        allSongs = getAudioFiles(context)
    }

    // 2. Lógica Inteligente de Imagen (Caché vs Internet)
    LaunchedEffect(artistName) {
        // PASO A: Verificar si ya lo tenemos guardado
        val cachedUrl = imageCache.getUrl("artist_$artistName")

        if (cachedUrl != null) {
            // Si existe, lo usamos directamente (0 gasto de datos de API)
            artistImageUrl = cachedUrl
        } else {
            // PASO B: Si no existe, buscamos en Deezer
            try {
                val response = withContext(Dispatchers.IO) {
                    DeezerClient.service.searchArtist(artistName)
                }
                if (response.data.isNotEmpty()) {
                    val urlFound = response.data[0].picture_xl
                    // Guardamos para la próxima vez
                    if (urlFound != null) {
                        imageCache.saveUrl("artist_$artistName", urlFound)
                        artistImageUrl = urlFound
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // NAVBAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrandOrange)
                .padding(horizontal = 8.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = artistName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // CONTENIDO
        if (artistSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 150.dp)
            ) {
                // IMAGEN
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        if (artistImageUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(artistImageUrl)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.ENABLED) // Forzar guardado en disco
                                    .build(),
                                contentDescription = "Imagen Artista",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray)) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.align(Alignment.Center).size(80.dp)
                                )
                            }
                        }
                        FloatingActionButton(
                            onClick = { if (artistSongs.isNotEmpty()) viewModel.playSong(artistSongs.first(), artistSongs) },
                            containerColor = BrandOrange,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 24.dp).size(64.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(32.dp))
                        }
                    }
                }

                // NOMBRE
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // HEADER LISTA
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Canciones principales", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Surface(color = BrandOrange, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "${artistSongs.size} CANCIONES",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // LISTA
                items(items = artistSongs, key = { it.id }) { song ->
                    AudioItem(audioFile = song, onClick = { viewModel.playSong(song, artistSongs) })
                }
            }
        }
    }
}