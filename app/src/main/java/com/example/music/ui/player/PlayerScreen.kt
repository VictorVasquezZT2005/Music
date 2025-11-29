package com.example.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder // Importación del corazón vacío
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.music.api.DeezerClient
import com.example.music.ui.viewmodel.MusicPlayerViewModel
import com.example.music.utils.ImageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// --- COLORES ---
private val BrandOrange = Color(0xFFFF5722)
private val BarBackground = Color(0xFF121212)
private val TextWhite = Color(0xFFEEEEEE)
private val TextSubtle = Color(0xFF9E9E9E)
private val DarkerBackground = Color(0xFF0A0A0A)
// Colores para la barra de progreso estilizada
private val ActiveLineColor = BrandOrange // Naranja brillante para el progreso
private val InactiveLineColor = Color(0xFF444444) // Gris oscuro y delgado
private val ThumbDotColor = TextWhite // Punto blanco sutil, si se usa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MusicPlayerViewModel,
    navController: NavController
) {
    val song = viewModel.currentSong ?: run {
        navController.popBackStack()
        return
    }
    val context = LocalContext.current

    // NUEVO: Observar el estado de favoritos
    val isFavorite = viewModel.isFavorite(song.id)

    // 1. LÓGICA DE IMAGEN HD (SIN CAMBIOS)
    val imageCache = remember { ImageCache(context) }
    var artModel by remember { mutableStateOf<Any>(song.albumArtUri) }

    LaunchedEffect(song.albumName, song.artist) {
        val cacheKey = "album_${song.albumName}_${song.artist}"
        val cachedUrl = imageCache.getUrl(cacheKey)

        if (cachedUrl != null) {
            artModel = cachedUrl
        } else {
            if (song.albumName != "Unknown" && song.artist != "Unknown") {
                try {
                    withContext(Dispatchers.IO) {
                        val response = DeezerClient.service.searchAlbum("${song.albumName} ${song.artist}")
                        val urlFound = response.data.firstOrNull()?.cover_xl
                        if (urlFound != null) {
                            imageCache.saveUrl(cacheKey, urlFound)
                            artModel = urlFound
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. IMAGEN DE FONDO (HD)
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(artModel)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Capa oscura degradada
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // 2. BARRA SUPERIOR REFINADA (SIN BOTÓN DE MENÚ)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(DarkerBackground.copy(alpha = 0.98f))
                .statusBarsPadding()
                // Aumentamos el padding para mantener el estilo
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Miniatura más grande
            AsyncImage(
                model = ImageRequest.Builder(context).data(artModel).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Info Texto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSubtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Espacio al final para asegurar que la info de texto use el espacio
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 3. ZONA INFERIOR (CONTROLES)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            // Zona Flotante (Corazón de Favoritos)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                DarkerBackground.copy(alpha = 0.3f),
                                DarkerBackground.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(top = 24.dp)
            ) {
                // Modificado: Corazón de favoritos
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(song.id) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (isFavorite) BrandOrange else TextWhite, // Naranja si es favorito
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // PANEL DE CONTROLES
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BarBackground)
                    .padding(bottom = 40.dp, top = 16.dp)
            ) {
                // BARRA DE PROGRESO ESTILIZADA (SIN CAMBIOS)
                Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                    Slider(
                        value = viewModel.progress,
                        onValueChange = { viewModel.seekTo(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = ThumbDotColor,
                            activeTrackColor = ActiveLineColor,
                            inactiveTrackColor = InactiveLineColor
                        ),
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(3.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = ActiveLineColor,
                                    inactiveTrackColor = InactiveLineColor
                                ),
                                drawStopIndicator = null
                            )
                        },
                        thumb = {
                            SliderDefaults.Thumb(
                                interactionSource = remember { MutableInteractionSource() },
                                colors = SliderDefaults.colors(thumbColor = ThumbDotColor),
                                modifier = Modifier.size(10.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )

                    // Tiempos (SIN CAMBIOS)
                    Row(
                        modifier = Modifier.fillMaxWidth().offset(y = (-8).dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime((viewModel.progress * song.duration).toLong()), style = MaterialTheme.typography.labelMedium, color = TextSubtle)
                        Text(formatTime(song.duration), style = MaterialTheme.typography.labelMedium, color = TextSubtle)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botonera (Iconos grandes) - SIN CAMBIOS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Repeat (48dp)
                    IconButton(onClick = { /* Repeat */ }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Repeat, contentDescription = "Repetir", tint = TextSubtle, modifier = Modifier.size(28.dp))
                    }

                    // Skip Previous (64dp)
                    IconButton(onClick = { viewModel.playPrevious() }, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = TextWhite, modifier = Modifier.size(40.dp))
                    }

                    // Play/Pause (80dp)
                    IconButton(onClick = { viewModel.togglePlayPause() }, modifier = Modifier.size(80.dp)) {
                        Icon(
                            imageVector = if (viewModel.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Reproducir/Pausar",
                            tint = BrandOrange,
                            modifier = Modifier.size(80.dp)
                        )
                    }

                    // Skip Next (64dp)
                    IconButton(onClick = { viewModel.playNext() }, modifier = Modifier.size(64.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = TextWhite, modifier = Modifier.size(40.dp))
                    }

                    // Shuffle (48dp)
                    IconButton(onClick = { /* Shuffle */ }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = BrandOrange, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

fun formatTime(milliseconds: Long): String {
    if (milliseconds < 0) return "0:00"
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}