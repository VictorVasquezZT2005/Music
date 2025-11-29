package com.example.music.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination // IMPORTANTE para el fix
import com.example.music.ui.navigation.Screen
import java.util.Calendar
import java.util.Locale

// Modelos estables
data class PlaylistUI(val id: Int, val name: String, val color: Color)
data class SongUI(val id: Int, val title: String, val artist: String)

val BrandOrange = Color(0xFFFF5722)

@Composable
fun HomeScreen(
    navController: NavController
) {
    // Datos simulados
    val playlists = remember {
        listOf(
            PlaylistUI(1, "Para Despertar", Color(0xFFFFF176)),
            PlaylistUI(2, "Música Nueva", Color(0xFFEF5350)),
            PlaylistUI(3, "Manejando", Color(0xFF4DB6AC)),
            PlaylistUI(4, "Relax Total", Color(0xFF4FC3F7))
        )
    }

    val mostPlayed = remember {
        List(10) { index ->
            SongUI(index, "Canción Top ${index + 1}", "Artista Famoso $index")
        }
    }

    val greetingText = remember { getDynamicGreeting() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo estático naranja superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(BrandOrange)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item(key = "header") {
                HeaderSection(greeting = greetingText)
            }

            // Playlists
            if (playlists.isNotEmpty()) {
                item(key = "playlists_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(playlists.size) { index ->
                            BigPlaylistCard(playlist = playlists[index])
                        }
                    }
                }
            }

            // Actividad Reciente con el botón VER MÁS arreglado
            item(key = "recent_section") {
                Column {
                    SectionTitleRow(
                        title = "Actividad reciente",
                        onSeeMoreClick = {
                            // CORRECCIÓN: Navegación limpia estilo BottomBar
                            navController.navigate(Screen.Library.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(mostPlayed.size) { index ->
                            RecentActivityItem(song = mostPlayed[index])
                        }
                    }
                }
            }
        }
    }
}

// --- Lógica de Saludo ---
fun getDynamicGreeting(): String {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val dayName = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale("es", "ES")) ?: "día"
    val timeOfDay = when (hour) {
        in 5..11 -> "por la mañana"
        in 12..19 -> "por la tarde"
        else -> "por la noche"
    }
    val capitalizedDay = dayName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    return "Es $capitalizedDay $timeOfDay"
}

// --- Componentes ---

@Composable
fun HeaderSection(greeting: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(text = "Escuchar ahora", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
        Text(text = greeting, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Normal)
        Text(text = "Música perfecta para...", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Normal)
    }
}

@Composable
fun SectionTitleRow(
    title: String,
    onSeeMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "VER MÁS",
            color = BrandOrange,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onSeeMoreClick() }
        )
    }
}

@Composable
fun BigPlaylistCard(playlist: PlaylistUI) {
    Surface(shape = RoundedCornerShape(4.dp), color = playlist.color, modifier = Modifier.width(160.dp).aspectRatio(0.8f).clickable { /* Abrir */ }) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            Text(text = playlist.name, color = Color.Black.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(16.dp).fillMaxWidth())
        }
    }
}

@Composable
fun RecentActivityItem(song: SongUI) {
    Column(modifier = Modifier.width(120.dp).clickable { /* Reproducir */ }) {
        Surface(shape = RoundedCornerShape(2.dp), color = Color.LightGray, modifier = Modifier.aspectRatio(1f)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = song.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
        Text(text = song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}