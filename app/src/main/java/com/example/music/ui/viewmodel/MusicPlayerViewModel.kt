package com.example.music.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.music.data.PreferenceManager
import com.example.music.service.MusicService
import com.example.music.ui.library.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    // --- ESTADOS UI ---
    var currentSong by mutableStateOf<AudioFile?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var selectedLibraryCategory by mutableStateOf("Canciones")

    // --- NUEVO: ESTADO DE FAVORITOS ---
    var favoriteSongIds by mutableStateOf(emptySet<Long>())
        private set

    // Instancia del PreferenceManager
    private val prefs = PreferenceManager(application)

    // --- CONEXIÓN CON SERVICIO ---
    // ... (código existente de ServiceConnection) ...
    @SuppressLint("StaticFieldLeak")
    private var musicService: MusicService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            currentSong = musicService?.currentSong
            isPlaying = musicService?.isPlayerPlaying() == true

            musicService?.onSongChanged = { song -> currentSong = song }
            musicService?.onIsPlayingChanged = { playing -> isPlaying = playing }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
        }
    }

    init {
        val intent = Intent(application, MusicService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        startProgressUpdate()

        // --- INICIALIZACIÓN DE FAVORITOS ---
        loadFavorites()
    }

    // --- LÓGICA DE FAVORITOS ---

    private fun loadFavorites() {
        // Cargar los IDs guardados y convertirlos a Longs.
        favoriteSongIds = prefs.getFavoriteSongIds().mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun isFavorite(songId: Long): Boolean {
        return songId in favoriteSongIds
    }

    fun toggleFavorite(songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentIds = prefs.getFavoriteSongIds().toMutableSet()
            val songIdStr = songId.toString()

            if (currentIds.contains(songIdStr)) {
                currentIds.remove(songIdStr)
            } else {
                currentIds.add(songIdStr)
            }

            prefs.saveFavoriteSongIds(currentIds)

            // Actualizar el estado reactivo en el hilo principal
            withContext(Dispatchers.Main) {
                favoriteSongIds = currentIds.mapNotNull { it.toLongOrNull() }.toSet()
            }
        }
    }

    // ... (rest of the controls and startProgressUpdate) ...
    // --- CONTROLES ---
    fun playSong(song: AudioFile, songList: List<AudioFile>) {
        musicService?.playSong(song, songList)
    }

    fun togglePlayPause() {
        musicService?.togglePlayPause()
    }

    fun playNext() {
        musicService?.playNext()
    }

    fun playPrevious() {
        musicService?.playPrevious()
    }

    fun seekTo(value: Float) {
        musicService?.seekTo(value)
        progress = value
    }

    // --- LOOP DE PROGRESO ---
    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (isActive) {
                if (isBound && isPlaying) {
                    progress = musicService?.getProgress() ?: 0f
                }
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}