package com.example.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.music.ui.library.AudioFile
import com.example.music.ui.viewmodel.MusicPlayerViewModel

// --- COLORES BÁSICOS PARA TEMA OSCURO PLANO ---
private val DarkBackground = Color(0xFF121212) // Fondo muy oscuro
private val DarkSurface = Color(0xFF1E1E1E)    // Superficie un poco más clara
private val DarkPrimary = Color(0xFFBB86FC)    // Morado brillante (Para contraste)
private val DarkOnBackground = Color.White     // Texto e iconos claros
private val DarkOnPrimary = Color.Black        // Texto en color primario
private val DarkSecondaryIcon = Color(0xFFB3B3B3) // Iconos grises suaves

@Composable
fun PlayerScreen(
    viewModel: MusicPlayerViewModel,
    navController: NavController
) {
    val song = viewModel.currentSong ?: run {
        navController.popBackStack()
        return
    }

    // Colores para el tema oscuro
    val primaryColor = DarkPrimary
    val onBackgroundColor = DarkOnBackground

    // --- CONTENEDOR PRINCIPAL OPTIMIZADO (DARK MODE) ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground) // Fondo oscuro
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerHeader(
                onClose = { navController.popBackStack() },
                onBackgroundColor = onBackgroundColor
            )

            Spacer(modifier = Modifier.weight(1f))

            AlbumArtDisplay(song = song)

            Spacer(modifier = Modifier.height(30.dp))

            TrackInfo(song = song, onBackgroundColor = onBackgroundColor)

            Spacer(modifier = Modifier.height(24.dp))

            PlayerProgressBar(
                progress = viewModel.progress,
                durationMs = song.duration,
                onSeek = { viewModel.seekTo(it) },
                primaryColor = primaryColor,
                onBackgroundColor = onBackgroundColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            MainControls(
                isPlaying = viewModel.isPlaying,
                onPrevious = { viewModel.playPrevious() },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                primaryColor = primaryColor,
                onPrimaryColor = DarkOnPrimary,
                onBackgroundColor = onBackgroundColor
            )

            Spacer(modifier = Modifier.weight(0.5f))

            SecondaryControls(onBackgroundColor = onBackgroundColor)
        }
    }
}

// ================= COMPONENTES INDIVIDUALES SIMPLIFICADOS (ADAPTADOS) =================

@Composable
fun PlayerHeader(onClose: () -> Unit, onBackgroundColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Cerrar",
                tint = onBackgroundColor
            )
        }
    }
}

@Composable
fun AlbumArtDisplay(song: AudioFile) {
    val context = LocalContext.current
    val defaultIcon = rememberVectorPainter(Icons.Default.MusicNote)

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .crossfade(true)
            .build(),
        contentDescription = "Portada del Álbum",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(DarkSurface), // Fondo Surface Oscuro
        error = defaultIcon,
        placeholder = defaultIcon
    )
}

@Composable
fun TrackInfo(song: AudioFile, onBackgroundColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = onBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = onBackgroundColor.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProgressBar(
    progress: Float,
    durationMs: Long,
    onSeek: (Float) -> Unit,
    primaryColor: Color,
    onBackgroundColor: Color
) {
    val currentTimeMs = (progress * durationMs).toLong()

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progress,
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(16.dp),
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = DarkSurface // Pista inactiva con color de superficie oscuro
            ),
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = remember { MutableInteractionSource() },
                    colors = SliderDefaults.colors(thumbColor = primaryColor),
                    modifier = Modifier.size(12.dp)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    colors = SliderDefaults.colors(
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = DarkSurface
                    ),
                    sliderState = sliderState,
                    modifier = Modifier.height(2.dp)
                )
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentTimeMs),
                style = MaterialTheme.typography.labelSmall,
                color = onBackgroundColor.copy(alpha = 0.6f)
            )
            Text(
                text = formatTime(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = onBackgroundColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun MainControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    primaryColor: Color,
    onPrimaryColor: Color,
    onBackgroundColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Anterior
        IconButton(onClick = onPrevious, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Anterior",
                modifier = Modifier.size(32.dp),
                tint = onBackgroundColor
            )
        }

        // Play/Pause (Botón central plano con color Primario oscuro)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(primaryColor)
                .clickable(onClick = onPlayPause)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                modifier = Modifier.size(40.dp),
                tint = onPrimaryColor // Texto/Icono oscuro sobre el botón primario
            )
        }

        // Siguiente
        IconButton(onClick = onNext, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Siguiente",
                modifier = Modifier.size(32.dp),
                tint = onBackgroundColor
            )
        }
    }
}

@Composable
fun SecondaryControls(onBackgroundColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Íconos más neutros con color suave
        PlayerIconButton(icon = Icons.Default.Shuffle, contentDescription = "Aleatorio", onBackgroundColor = onBackgroundColor)
        PlayerIconButton(icon = Icons.Default.Repeat, contentDescription = "Repetir", onBackgroundColor = onBackgroundColor)
        PlayerIconButton(icon = Icons.Outlined.FavoriteBorder, contentDescription = "Favorito", onBackgroundColor = onBackgroundColor)
        PlayerIconButton(icon = Icons.Default.PlaylistPlay, contentDescription = "Playlist", onBackgroundColor = onBackgroundColor)
    }
}

@Composable
fun PlayerIconButton(icon: ImageVector, contentDescription: String, onBackgroundColor: Color, size: Dp = 24.dp) {
    IconButton(onClick = { /* TODO: Implementar acciones */ }) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = DarkSecondaryIcon, // Color gris suave para iconos secundarios
            modifier = Modifier.size(size)
        )
    }
}

// Función auxiliar para formatear tiempo (sin cambios)
fun formatTime(milliseconds: Long): String {
    if (milliseconds < 0) return "00:00"
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}