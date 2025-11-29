package com.example.music.ui.library

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite // <-- Importación para el corazón lleno
import androidx.compose.material.icons.outlined.FavoriteBorder // <-- Importación para el corazón vacío
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.music.api.DeezerClient
import com.example.music.data.PreferenceManager
import com.example.music.ui.navigation.Screen
import com.example.music.ui.viewmodel.MusicPlayerViewModel
import com.example.music.utils.ImageCache // Asegúrate de importar tu clase ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val BrandOrange = Color(0xFFFF5722)

// --- MODELO DE DATOS ---
data class AudioFile(
    val id: Long, val title: String, val artist: String, val albumName: String,
    val uri: Uri, val duration: Long, val albumId: Long, val path: String
) {
    val albumArtUri: Uri
        get() = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
}

// --- NUEVO: MODELO TEMPORAL PARA PLAYLISTS ---
data class Playlist(
    val id: Long,
    val name: String,
    val songIds: List<Long> // Lista de IDs de canciones
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicPlayerViewModel = viewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    var audioFiles by remember { mutableStateOf<List<AudioFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Instanciamos el caché una sola vez para toda la pantalla
    val imageCache = remember { ImageCache(context) }

    val categories = listOf("CANCIONES", "ARTISTAS", "ÁLBUMES", "LISTAS")
    val tabIndex = when (viewModel.selectedLibraryCategory) {
        "Canciones" -> 0
        "Artistas" -> 1
        "Álbumes" -> 2
        else -> 3
    }

    LaunchedEffect(Unit) {
        val prefs = PreferenceManager(context)
        val excludedFolders = prefs.getExcludedFolders()
        audioFiles = getAudioFiles(context, excludedFolders)
        isLoading = false
    }

    // Listas agrupadas
    val artistsList = remember(audioFiles) {
        audioFiles.groupBy { it.artist }.keys.toList().sorted()
    }

    // Agrupamos también por Artista para poder buscar la portada correctamente
    val albumsList = remember(audioFiles) {
        // La clave ahora es un Triple: (AlbumName, AlbumId, ArtistName)
        audioFiles.groupBy { Triple(it.albumName, it.albumId, it.artist) }
            .keys.toList()
            .sortedBy { it.first }
    }

    // --- NUEVO: LISTA TEMPORAL DE PLAYLISTS (MODIFICADA PARA INCLUIR FAVORITAS) ---
    val favoriteSongs = viewModel.favoriteSongIds.toList() // Obtenemos las favoritas del ViewModel

    val playlists = remember(favoriteSongs) {
        // ID 1 es ahora "Mis Favoritas", usando los IDs reales de canciones favoritas.
        mutableStateOf(
            listOf(
                Playlist(1, "Mis Favoritas", favoriteSongs),
                Playlist(2, "Rock Clásico", listOf(104, 105)),
                Playlist(3, "Para Correr", listOf(106, 107, 108, 109))
            ).filter { it.songIds.isNotEmpty() || it.id != 1L } // Ocultar si favoritas está vacío
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        // --- HEADER ---
        Column(modifier = Modifier.fillMaxWidth().background(BrandOrange)) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp)) {
                Text(
                    text = "Biblioteca",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            ScrollableTabRow(
                selectedTabIndex = tabIndex,
                containerColor = BrandOrange,
                contentColor = Color.White,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[tabIndex]), color = Color.White, height = 3.dp)
                },
                divider = {}
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = {
                            viewModel.selectedLibraryCategory = when(index) {
                                0 -> "Canciones"
                                1 -> "Artistas"
                                2 -> "Álbumes"
                                else -> "Listas"
                            }
                        },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                        selectedContentColor = Color.White,
                        unselectedContentColor = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // --- CONTENIDO ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandOrange)
            }
        } else if (audioFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No se encontró música", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().weight(1f), contentPadding = PaddingValues(bottom = 150.dp)) {
                when (viewModel.selectedLibraryCategory) {
                    "Canciones" -> {
                        items(audioFiles, key = { it.id }) { audio ->
                            AudioItem(
                                audioFile = audio,
                                isFavorite = viewModel.isFavorite(audio.id), // Estado favorito
                                onFavoriteToggle = { viewModel.toggleFavorite(audio.id) }, // Toggle action
                                onClick = { viewModel.playSong(audio, audioFiles) }
                            )
                        }
                    }
                    "Artistas" -> {
                        items(artistsList) { artistName ->
                            ArtistItem(
                                artistName = artistName,
                                imageCache = imageCache, // Pasamos el caché
                                onClick = { navController.navigate(Screen.ArtistDetail.createRoute(artistName)) }
                            )
                        }
                    }
                    "Álbumes" -> {
                        items(albumsList) { (albumName, albumId, artistName) ->
                            AlbumItem(
                                albumName = albumName,
                                albumId = albumId,
                                artistName = artistName,
                                imageCache = imageCache, // Pasamos el caché
                                onClick = { navController.navigate(Screen.AlbumDetail.createRoute(albumId)) }
                            )
                        }
                    }
                    // --- NUEVA SECCIÓN LISTAS ---
                    "Listas" -> {
                        items(playlists.value, key = { it.id }) { playlist ->
                            PlaylistItem(playlist = playlist, onClick = {
                                navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                            })
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES ACTUALIZADOS CON BOTÓN DE CORAZÓN ---

@Composable
fun AudioItem(
    audioFile: AudioFile,
    isFavorite: Boolean, // NUEVO
    onFavoriteToggle: () -> Unit, // NUEVO
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val defaultIcon = rememberVectorPainter(Icons.Default.MusicNote)
    val BrandOrange = Color(0xFFFF5722) // Necesario redefinir o importar si está fuera del alcance

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(4.dp), color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.size(48.dp)) {
            // Canciones usan la imagen local (rápida y correcta para archivos sueltos)
            AsyncImage(
                model = ImageRequest.Builder(context).data(audioFile.albumArtUri).crossfade(true).build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                error = defaultIcon, placeholder = defaultIcon
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(audioFile.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(audioFile.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // --- BOTÓN DE CORAZÓN ---
        IconButton(onClick = onFavoriteToggle) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Añadir a favoritas",
                tint = if (isFavorite) BrandOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ArtistItem(
    artistName: String,
    imageCache: ImageCache, // Recibimos el caché
    onClick: () -> Unit
) {
    val defaultIcon = rememberVectorPainter(Icons.Default.Person)
    var imageUrl by remember { mutableStateOf<String?>(null) }

    // Lógica inteligente de carga (Igual que en ArtistDetailScreen)
    LaunchedEffect(artistName) {
        val cacheKey = "artist_$artistName"
        val cachedUrl = imageCache.getUrl(cacheKey)

        if (cachedUrl != null) {
            imageUrl = cachedUrl
        } else {
            // Si no está en caché, buscamos en la API
            try {
                withContext(Dispatchers.IO) {
                    val response = DeezerClient.service.searchArtist(artistName)
                    val urlFound = response.data.firstOrNull()?.picture_xl
                    if (urlFound != null) {
                        imageCache.saveUrl(cacheKey, urlFound)
                        imageUrl = urlFound
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(50.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl) // Usamos la URL (Coil usa su propio caché de disco)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                error = defaultIcon, placeholder = defaultIcon
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = artistName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun AlbumItem(
    albumName: String,
    albumId: Long,
    artistName: String,
    imageCache: ImageCache, // Recibimos el caché
    onClick: () -> Unit
) {
    // 1. Imagen Local (Prioridad inicial)
    val localUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
    val defaultIcon = rememberVectorPainter(Icons.Default.Album)

    // 2. Imagen de Red (Mejora visual)
    var networkUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(albumName, artistName) {
        if (albumName != "Desconocido" && albumName != "Álbum") {
            val cacheKey = "album_${albumName}_$artistName"
            val cachedUrl = imageCache.getUrl(cacheKey)

            if (cachedUrl != null) {
                networkUrl = cachedUrl
            } else {
                try {
                    withContext(Dispatchers.IO) {
                        val response = DeezerClient.service.searchAlbum("$albumName $artistName")
                        val urlFound = response.data.firstOrNull()?.cover_xl
                        if (urlFound != null) {
                            imageCache.saveUrl(cacheKey, urlFound)
                            networkUrl = urlFound
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    // Decisión final: Si tenemos URL de red, la usamos. Si no, usamos la local.
    val finalImageModel = networkUrl ?: localUri

    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(50.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(finalImageModel)
                    .crossfade(true)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null, contentScale = ContentScale.Crop,
                error = defaultIcon, placeholder = defaultIcon
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = albumName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = artistName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// --- NUEVO COMPONENTE: PlaylistItem ---
@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    val defaultIcon = rememberVectorPainter(Icons.Default.List) // Usaremos un icono de lista
    val BrandOrange = Color(0xFFFF5722) // Necesario redefinir o importar si está fuera del alcance

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono de la lista de reproducción
        Surface(
            shape = RoundedCornerShape(8.dp), // Diferente forma que álbumes/canciones
            color = BrandOrange.copy(alpha = 0.8f), // Color distintivo
            modifier = Modifier.size(50.dp)
        ) {
            Icon(
                painter = defaultIcon,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(10.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songIds.size} canciones", // Mostrar el contador
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Puedes agregar un icono de flecha o menú aquí si es necesario
    }
}

// --- CARGA DE ARCHIVOS LOCALES ---
suspend fun getAudioFiles(context: Context, excludedFolders: Set<String> = emptySet()): List<AudioFile> = withContext(Dispatchers.IO) {
    val audioList = mutableListOf<AudioFile>()
    val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
    val projection = arrayOf(
        MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DATA
    )
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
    try {
        val cursor = context.contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} ASC")
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                val path = it.getString(dataCol)
                if (path != null) {
                    val parentFolder = File(path).parent
                    if (parentFolder != null && excludedFolders.contains(parentFolder)) continue
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Desconocido"
                    val artist = it.getString(artistCol) ?: "Desconocido"
                    val album = it.getString(albumCol) ?: "Desconocido"
                    val duration = it.getLong(durCol)
                    val albumId = it.getLong(albumIdCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    audioList.add(AudioFile(id, title, artist, album, uri, duration, albumId, path))
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    return@withContext audioList
}