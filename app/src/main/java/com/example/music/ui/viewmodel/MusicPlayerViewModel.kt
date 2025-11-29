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
import com.example.music.service.MusicService
import com.example.music.ui.library.AudioFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    // --- ESTADOS UI ---
    var currentSong by mutableStateOf<AudioFile?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    // Estado para saber en qué pestaña de la librería estamos
    var selectedLibraryCategory by mutableStateOf("Canciones")

    // --- CONEXIÓN CON SERVICIO ---
    @SuppressLint("StaticFieldLeak")
    private var musicService: MusicService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            // Recuperar estado actual si el servicio ya estaba corriendo
            currentSong = musicService?.currentSong
            isPlaying = musicService?.isPlayerPlaying() == true

            // Escuchar cambios
            musicService?.onSongChanged = { song -> currentSong = song }
            musicService?.onIsPlayingChanged = { playing -> isPlaying = playing }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
        }
    }

    init {
        // CORRECCIÓN CRÍTICA:
        // Usamos solo bindService con BIND_AUTO_CREATE.
        // Esto crea el servicio sin exigir notificación inmediata (evita el crash de los 5 segundos).
        val intent = Intent(application, MusicService::class.java)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // Iniciamos el ciclo de actualización de la barra
        startProgressUpdate()
    }

    // --- CONTROLES ---

    fun playSong(song: AudioFile, songList: List<AudioFile>) {
        musicService?.playSong(song, songList)
        // Al dar play, el servicio ya se encarga de mostrar la notificación
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
                // Solo actualizamos si estamos conectados y reproduciendo
                if (isBound && isPlaying) {
                    progress = musicService?.getProgress() ?: 0f
                }
                delay(100) // 10 veces por segundo es suficiente
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Desconectamos el ViewModel del Servicio, pero NO matamos el servicio
        // para que la música siga sonando.
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}