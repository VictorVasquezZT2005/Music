package com.example.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.music.data.PreferenceManager
import com.example.music.ui.components.MiniPlayer
import com.example.music.ui.components.NavBar
import com.example.music.ui.details.AlbumDetailScreen
import com.example.music.ui.details.ArtistDetailScreen
import com.example.music.ui.details.PlaylistDetailScreen
import com.example.music.ui.home.HomeScreen
import com.example.music.ui.library.LibraryScreen
import com.example.music.ui.navigation.Screen
import com.example.music.ui.permissions.PermissionScreen
import com.example.music.ui.player.PlayerScreen
import com.example.music.ui.search.SearchScreen
import com.example.music.ui.theme.MusicTheme
import com.example.music.ui.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Permite que el contenido se dibuje detrás de las barras del sistema (status bar y navigation bar)
        enableEdgeToEdge()

        val preferenceManager = PreferenceManager(this)
        val startDestination = if (preferenceManager.isFirstRun()) {
            Screen.Permission.route
        } else {
            Screen.Home.route
        }

        setContent {
            MusicTheme {
                val navController = rememberNavController()
                val musicViewModel: MusicPlayerViewModel = viewModel()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // CONFIGURACIÓN DE VISIBILIDAD DE BARRAS
                val showBars = currentRoute != Screen.Player.route &&
                        currentRoute != Screen.Permission.route &&
                        currentRoute?.startsWith(Screen.PlaylistDetail.route.split("/").first()) != true // Excluir rutas de detalle

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBars) {
                            Column {
                                MiniPlayer(viewModel = musicViewModel, navController = navController)
                                NavBar(navController = navController)
                            }
                        }
                    }
                ) { innerPadding ->

                    // Definimos un modificador para las pantallas que SÍ usan la NavBar (Home, Library, Search)
                    // Este modificador aplica el padding que compensa la barra inferior (innerPadding)
                    // y usa fillMaxSize() para que las pantallas llenen el espacio restante.
                    val defaultModifier = Modifier.padding(innerPadding).fillMaxSize()

                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        // El NavHost en sí no tiene padding. Las pantallas individuales lo manejan.
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(Screen.Permission.route) {
                            PermissionScreen(onPermissionGranted = {
                                preferenceManager.setFirstRunCompleted()
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Permission.route) { inclusive = true } }
                            })
                        }

                        // Las pantallas con NavBar visible usan el Box con defaultModifier
                        composable(Screen.Home.route) {
                            Box(modifier = defaultModifier) {
                                HomeScreen(navController = navController)
                            }
                        }

                        composable(Screen.Search.route) {
                            Box(modifier = defaultModifier) {
                                SearchScreen(viewModel = musicViewModel)
                            }
                        }

                        composable(Screen.Library.route) {
                            Box(modifier = defaultModifier) {
                                LibraryScreen(viewModel = musicViewModel, navController = navController)
                            }
                        }

                        // PlayerScreen NO recibe el innerPadding.
                        // Esto permite que el PlayerHeader maneje el padding de la barra de estado por sí mismo,
                        // eliminando el doble padding.
                        composable(Screen.Player.route) {
                            PlayerScreen(viewModel = musicViewModel, navController = navController)
                        }

                        composable(
                            route = Screen.ArtistDetail.route,
                            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            Box(modifier = defaultModifier) {
                                val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
                                ArtistDetailScreen(artistName = artistName, viewModel = musicViewModel, navController = navController)
                            }
                        }

                        composable(
                            route = Screen.AlbumDetail.route,
                            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            Box(modifier = defaultModifier) {
                                val albumId = backStackEntry.arguments?.getLong("albumId") ?: -1L
                                AlbumDetailScreen(albumId = albumId, viewModel = musicViewModel, navController = navController)
                            }
                        }

                        // Ruta de Playlist Detail (No usa Box con defaultModifier porque tiene su propio Scaffold)
                        composable(
                            route = Screen.PlaylistDetail.route,
                            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: -1L
                            PlaylistDetailScreen(playlistId = playlistId, viewModel = musicViewModel, navController = navController)
                        }
                    }
                }
            }
        }
    }
}