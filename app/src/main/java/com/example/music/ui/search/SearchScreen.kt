package com.example.music.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.music.ui.home.BrandOrange // Usamos el mismo color que en Home
import com.example.music.ui.library.AudioFile
import com.example.music.ui.library.AudioItem
import com.example.music.ui.library.getAudioFiles
import com.example.music.ui.viewmodel.MusicPlayerViewModel

@Composable
fun SearchScreen(
    viewModel: MusicPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    // Usamos remember para cachear la lista completa y no recargarla innecesariamente
    var allSongs by remember { mutableStateOf<List<AudioFile>>(emptyList()) }

    LaunchedEffect(Unit) {
        // Cargar todas las canciones la primera vez
        allSongs = getAudioFiles(context)
    }

    // Filtrado optimizado
    val filteredSongs = remember(searchQuery, allSongs) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. FONDO NARANJA (HEADER)
        // Coincide con el estilo del Home
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp) // Altura suficiente para título y barra
                .background(BrandOrange)
        )

        Column(modifier = Modifier.fillMaxSize()) {

            // --- ZONA DE BÚSQUEDA ---
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 24.dp, bottom = 16.dp)
            ) {
                // Título
                Text(
                    text = "Buscar",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Barra de Búsqueda estilo "Tarjeta"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    shadowElevation = 4.dp, // Sombra suave para que flote
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar canciones o artistas...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Borrar", tint = Color.Gray)
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent, // Sin linea abajo
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = BrandOrange
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                }
            }

            // --- ZONA DE RESULTADOS ---
            // Usamos una Surface con esquinas redondeadas arriba para crear efecto de "Hoja"
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (searchQuery.isNotEmpty() && filteredSongs.isEmpty()) {
                        // Mensaje Sin Resultados
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No se encontraron resultados", color = Color.Gray)
                        }
                    } else if (searchQuery.isEmpty()) {
                        // Mensaje Inicial
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Explora tu colección musical", color = Color.Gray)
                        }
                    } else {
                        // Lista de Resultados
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 150.dp), // Espacio para player
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = filteredSongs,
                                key = { it.id } // Optimización clave
                            ) { audio ->
                                AudioItem(
                                    audioFile = audio,
                                    // === CORRECCIÓN AÑADIDA ===
                                    isFavorite = viewModel.isFavorite(audio.id),
                                    onFavoriteToggle = { viewModel.toggleFavorite(audio.id) },
                                    // ==========================
                                    onClick = {
                                        // Reproducir la canción usando la lista filtrada como contexto
                                        viewModel.playSong(audio, filteredSongs)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}