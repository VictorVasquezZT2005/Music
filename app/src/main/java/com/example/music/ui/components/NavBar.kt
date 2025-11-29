package com.example.music.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // <--- ESTA ERA LA QUE FALTABA
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.music.ui.navigation.Screen

// El naranja característico
private val BrandOrange = Color(0xFFFF5722)

@Composable
fun NavBar(navController: NavController) {
    val items = remember {
        listOf(
            Screen.Home,
            Screen.Search,
            Screen.Library
        )
    }

    NavigationBar(
        tonalElevation = 0.dp,
        // Usamos el color de 'Surface' del tema para que se adapte (Blanco en día, Oscuro en noche)
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        // Ahora el 'by' funcionará correctamente gracias al import
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            val isSelected = currentRoute == screen.route

            val labelText = when (screen) {
                Screen.Home -> "Inicio"
                Screen.Search -> "Buscar"
                Screen.Library -> "Biblioteca"
                else -> ""
            }

            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = labelText) },
                label = {
                    Text(
                        text = labelText,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = BrandOrange,
                    selectedIconColor = Color.White,
                    selectedTextColor = BrandOrange,
                    // Compatible con modo oscuro
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
        }
    }
}