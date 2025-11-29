package com.example.music.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Importación corregida
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
    val imageCache = remember { ImageCache(context) }

    // Painter por defecto para la imagen del artista
    val defaultArtistPainter = rememberVectorPainter(Icons.Default.Person)

    // 1. Cargar canciones locales
    LaunchedEffect(Unit) {
        allSongs = getAudioFiles(context)
    }

    // 2. Lógica Inteligente de Imagen (Caché vs Internet)
    LaunchedEffect(artistName) {
        val cacheKey = "artist_$artistName"
        val cachedUrl = imageCache.getUrl(cacheKey)

        if (cachedUrl != null) {
            artistImageUrl = cachedUrl
        } else {
            try {
                val response = withContext(Dispatchers.IO) {
                    DeezerClient.service.searchArtist(artistName)
                }
                if (response.data.isNotEmpty()) {
                    val urlFound = response.data[0].picture_xl
                    if (urlFound != null) {
                        imageCache.saveUrl(cacheKey, urlFound)
                        artistImageUrl = urlFound
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            // NAVBAR CORREGIDA: Usamos TopAppBar para un control consistente
            TopAppBar(
                title = {
                    Text(
                        text = artistName,
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Uso de AutoMirrored
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
        }
    ) { innerPadding ->

        // CONTENIDO
        if (artistSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = 150.dp) // Offset para el MiniPlayer
            ) {
                // IMAGEN
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(artistImageUrl)
                                .crossfade(true)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "Imagen Artista",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().background(Color.LightGray),
                            // Usamos el painter por defecto si la carga falla
                            placeholder = defaultArtistPainter,
                            error = defaultArtistPainter
                        )

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

                // LISTA CORREGIDA: Se pasan los parámetros de Favoritos
                items(items = artistSongs, key = { it.id }) { song ->
                    AudioItem(
                        audioFile = song,
                        isFavorite = viewModel.isFavorite(song.id), // <-- CORRECCIÓN
                        onFavoriteToggle = { viewModel.toggleFavorite(song.id) }, // <-- CORRECCIÓN
                        onClick = { viewModel.playSong(song, artistSongs) }
                    )
                }
            }
        }
    }
}