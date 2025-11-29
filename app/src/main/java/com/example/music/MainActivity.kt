package com.example.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
                val showBars = currentRoute != Screen.Player.route && currentRoute != Screen.Permission.route

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
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Permission.route) {
                            PermissionScreen(onPermissionGranted = {
                                preferenceManager.setFirstRunCompleted()
                                navController.navigate(Screen.Home.route) { popUpTo(Screen.Permission.route) { inclusive = true } }
                            })
                        }

                        // --- AQUÍ ESTABA EL ERROR ---
                        // Ahora pasamos el navController a HomeScreen
                        composable(Screen.Home.route) {
                            HomeScreen(navController = navController)
                        }
                        // ----------------------------

                        composable(Screen.Search.route) { SearchScreen(viewModel = musicViewModel) }

                        composable(Screen.Library.route) {
                            LibraryScreen(viewModel = musicViewModel, navController = navController)
                        }

                        composable(Screen.Player.route) {
                            PlayerScreen(viewModel = musicViewModel, navController = navController)
                        }

                        composable(
                            route = "artist_detail/{artistName}",
                            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
                            ArtistDetailScreen(artistName = artistName, viewModel = musicViewModel, navController = navController)
                        }

                        composable(
                            route = "album_detail/{albumId}",
                            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val albumId = backStackEntry.arguments?.getLong("albumId") ?: -1L
                            AlbumDetailScreen(albumId = albumId, viewModel = musicViewModel, navController = navController)
                        }
                    }
                }
            }
        }
    }
}