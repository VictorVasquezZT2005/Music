package com.example.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.music.ui.navigation.Screen
import com.example.music.ui.viewmodel.MusicPlayerViewModel

// COLORES DEL TEMA
private val BrandOrange = Color(0xFFFF5722)
// Un gris oscuro elegante (estilo Material Dark) para el fondo del reproductor
private val DarkPlayerBackground = Color(0xFF212121)

@Composable
fun MiniPlayer(
    viewModel: MusicPlayerViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val song = viewModel.currentSong ?: return
    val context = LocalContext.current
    val defaultIcon = rememberVectorPainter(Icons.Default.MusicNote)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable { navController.navigate(Screen.Player.route) },
        color = DarkPlayerBackground, // FONDO OSCURO
        contentColor = Color.White,   // Todo el contenido (texto/iconos) será blanco por defecto
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // CONTENIDO DEL REPRODUCTOR (Fila)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Carátula
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF333333), // Placeholder gris oscuro
                    modifier = Modifier.size(48.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(song.albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = defaultIcon,
                        placeholder = defaultIcon
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Texto
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White, // Título Blanco Brillante
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f), // Artista blanco suave
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Controles
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Anterior (Blanco)
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Prev",
                            tint = Color.White
                        )
                    }

                    // Botón Play/Pause (FONDO NARANJA - RESALTA EN OSCURO)
                    IconButton(
                        onClick = { viewModel.togglePlayPause() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = BrandOrange,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp).size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause"
                        )
                    }

                    // Botón Siguiente (Blanco)
                    IconButton(onClick = { viewModel.playNext() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                }
            }

            // BARRA DE PROGRESO (NARANJA)
            LinearProgressIndicator(
                progress = { viewModel.progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = BrandOrange,
                trackColor = Color.White.copy(alpha = 0.1f), // Pista muy sutil
            )
        }
    }
}