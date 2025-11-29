package com.example.music.ui.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.music.data.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Instancia del PreferenceManager para guardar las carpetas
    val preferenceManager = remember { PreferenceManager(context) }

    // Estados
    var hasPermission by remember { mutableStateOf(false) }
    var folders by remember { mutableStateOf<List<AudioFolder>>(emptyList()) }
    // Guardamos las carpetas EXCLUIDAS (las que el usuario desmarca)
    val excludedPaths = remember { mutableStateListOf<String>() }
    var isLoadingFolders by remember { mutableStateOf(false) }

    // Definir permisos según versión de Android
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Función para iniciar el escaneo (se usa en varios lugares)
    fun startFolderScan() {
        isLoadingFolders = true
        scope.launch {
            val scannedFolders = withContext(Dispatchers.IO) {
                scanAudioFolders(context)
            }
            folders = scannedFolders
            isLoadingFolders = false

            // Auto-detectar carpetas basura (ej. WhatsApp) y marcarlas para excluir por defecto
            scannedFolders.forEach { folder ->
                if (folder.name.contains("WhatsApp", ignoreCase = true) ||
                    folder.name.contains("Telegram", ignoreCase = true)) {
                    if (!excludedPaths.contains(folder.path)) {
                        excludedPaths.add(folder.path)
                    }
                }
            }
        }
    }

    // VERIFICACIÓN INICIAL: Al iniciar la pantalla, revisamos si YA tenemos permisos
    LaunchedEffect(Unit) {
        val isGranted = permissionsToRequest.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
        if (isGranted) {
            hasPermission = true
            startFolderScan() // Si ya tiene permisos, cargamos carpetas directo
        }
    }

    // Launcher para solicitar permisos si no los tiene
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Lógica para Android 13+ vs anteriores
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] ?:
        permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        if (audioGranted) {
            hasPermission = true
            startFolderScan()
        } else {
            Toast.makeText(context, "Es necesario el permiso para leer música", Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!hasPermission) {
            // --- VISTA 1: PEDIR PERMISOS ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text("Bienvenido", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Para empezar, necesitamos acceso a tus archivos de audio.",
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { launcher.launch(permissionsToRequest) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Conceder Permisos")
                }
            }
        } else {
            // --- VISTA 2: SELECCIONAR CARPETAS ---
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Configurar Biblioteca",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Desmarca las carpetas que NO quieres ver (ej. WhatsApp).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (isLoadingFolders) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Buscando carpetas...", modifier = Modifier.padding(top = 40.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp)
                    ) {
                        if (folders.isEmpty()) {
                            item {
                                Text(
                                    "No encontramos carpetas con música.",
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        items(folders) { folder ->
                            // Si la ruta NO está en la lista de excluidos, el switch está activado (True)
                            val isIncluded = !excludedPaths.contains(folder.path)

                            ListItem(
                                headlineContent = { Text(folder.name, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("${folder.songCount} canciones") },
                                leadingContent = {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingContent = {
                                    Switch(
                                        checked = isIncluded,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                // Si el usuario lo activa, lo quitamos de la lista de excluidos
                                                excludedPaths.remove(folder.path)
                                            } else {
                                                // Si el usuario lo desactiva, lo agregamos a excluidos
                                                excludedPaths.add(folder.path)
                                            }
                                        }
                                    )
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }

                // Botón Finalizar
                Button(
                    onClick = {
                        // Guardamos las exclusiones en preferencias
                        preferenceManager.saveExcludedFolders(excludedPaths.toSet())
                        // Avisamos al MainActivity que terminamos
                        onPermissionGranted()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = !isLoadingFolders
                ) {
                    Text("Terminar Configuración")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.Check, contentDescription = null)
                }
            }
        }
    }
}