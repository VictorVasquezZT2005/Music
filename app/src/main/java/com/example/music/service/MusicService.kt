package com.example.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.example.music.MainActivity
import com.example.music.R
import com.example.music.ui.library.AudioFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MusicService : Service() {

    private val binder = MusicBinder()
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var playlist: List<AudioFile> = emptyList()
    var currentIndex: Int = -1
    var currentSong: AudioFile? = null

    var onSongChanged: ((AudioFile?) -> Unit)? = null
    var onIsPlayingChanged: ((Boolean) -> Unit)? = null

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val ACTION_PREVIOUS = "action_previous"
        const val ACTION_PLAY_PAUSE = "action_play_pause"
        const val ACTION_NEXT = "action_next"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Configuración avanzada de la sesión para soportar Seekbar (Barra de progreso)
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { togglePlayPause() }
                override fun onPause() { togglePlayPause() }
                override fun onSkipToNext() { playNext() }
                override fun onSkipToPrevious() { playPrevious() }
                // ESTO PERMITE ADELANTAR/ATRASAR DESDE LA NOTIFICACIÓN
                override fun onSeekTo(pos: Long) {
                    seekTo(pos.toFloat() / (mediaPlayer?.duration ?: 1).toFloat())
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PREVIOUS -> playPrevious()
            ACTION_PLAY_PAUSE -> togglePlayPause()
            ACTION_NEXT -> playNext()
        }
        return START_NOT_STICKY
    }

    // --- LÓGICA DE AUDIO ---

    fun playSong(song: AudioFile, songList: List<AudioFile>) {
        playlist = songList
        currentIndex = songList.indexOfFirst { it.id == song.id }
        playCurrentIndex()
    }

    private fun playCurrentIndex() {
        if (currentIndex == -1 || playlist.isEmpty()) return
        val song = playlist[currentIndex]
        currentSong = song
        onSongChanged?.invoke(song)

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setDataSource(applicationContext, song.uri)
            prepare()
            start()
            setOnCompletionListener { playNext() }
        }

        onIsPlayingChanged?.invoke(true)
        updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING)
        updateMediaSessionMetadata(song) // Actualizamos datos para la barra
        showNotification(song, true)
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                onIsPlayingChanged?.invoke(false)
                updateMediaSessionState(PlaybackStateCompat.STATE_PAUSED)
                // Al pausar, permitimos que la notificación se pueda borrar (Swipe)
                stopForeground(false)
                showNotification(currentSong, false)
            } else {
                player.start()
                onIsPlayingChanged?.invoke(true)
                updateMediaSessionState(PlaybackStateCompat.STATE_PLAYING)
                showNotification(currentSong, true)
            }
        }
    }

    fun playNext() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex < playlist.size - 1) currentIndex + 1 else 0
        playCurrentIndex()
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        currentIndex = if (currentIndex > 0) currentIndex - 1 else playlist.size - 1
        playCurrentIndex()
    }

    fun seekTo(position: Float) {
        mediaPlayer?.let {
            if (it.duration > 0) {
                val newPos = (position * it.duration).toLong()
                it.seekTo(newPos.toInt())
                // Actualizamos el estado para que la barra de la notificación se mueva
                updateMediaSessionState(if (it.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
            }
        }
    }

    fun getProgress(): Float {
        mediaPlayer?.let {
            if (it.isPlaying && it.duration > 0) {
                return it.currentPosition.toFloat() / it.duration.toFloat()
            }
        }
        return 0f
    }

    fun isPlayerPlaying(): Boolean = mediaPlayer?.isPlaying == true

    // --- ACTUALIZACIÓN DE METADATOS Y ESTADO (CLAVE PARA SEEKBAR) ---

    private fun updateMediaSessionState(state: Int) {
        val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO // IMPORTANTE: Habilita la barra
            )
            .setState(state, position, 1f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun updateMediaSessionMetadata(song: AudioFile?) {
        if (song == null) return

        // Cargamos la carátula en segundo plano para pasarla a la metadata
        serviceScope.launch {
            var artBitmap: Bitmap? = null
            try {
                val imageLoader = ImageLoader(applicationContext)
                val request = ImageRequest.Builder(applicationContext)
                    .data(song.albumArtUri)
                    .size(512, 512)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result.drawable != null) {
                    artBitmap = drawableToBitmap(result.drawable!!)
                }
            } catch (e: Exception) { e.printStackTrace() }

            if (artBitmap == null) {
                artBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            }

            // Enviamos duración y carátula al sistema
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration) // CLAVE PARA BARRA
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, artBitmap)
                .build()

            mediaSession.setMetadata(metadata)
            // Refrescamos notificación si es necesario
            if (currentSong?.id == song.id) {
                showNotification(song, isPlayerPlaying())
            }
        }
    }

    // --- NOTIFICACIÓN ---

    private fun showNotification(song: AudioFile?, isPlaying: Boolean) {
        if (song == null) return

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(this, 0, Intent(this, MusicService::class.java).apply { action = ACTION_PREVIOUS }, PendingIntent.FLAG_IMMUTABLE)
        val playPauseIntent = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE }, PendingIntent.FLAG_IMMUTABLE)
        val nextIntent = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).apply { action = ACTION_NEXT }, PendingIntent.FLAG_IMMUTABLE)

        serviceScope.launch {
            // Obtenemos la imagen para el fondo de la notificación (HD)
            // (La metadata ya se actualizó en updateMediaSessionMetadata, esto es solo visual)
            var largeIcon: Bitmap? = null
            try {
                val imageLoader = ImageLoader(applicationContext)
                val request = ImageRequest.Builder(applicationContext)
                    .data(song.albumArtUri)
                    .size(1024, 1024)
                    .scale(Scale.FILL)
                    .precision(Precision.EXACT)
                    .allowHardware(false)
                    .build()
                val result = imageLoader.execute(request)
                if (result.drawable != null) {
                    largeIcon = getHighQualityBitmap(result.drawable!!)
                }
            } catch (e: Exception) { e.printStackTrace() }

            if (largeIcon == null) largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)

            val notification = NotificationCompat.Builder(this@MusicService, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(largeIcon)
                .setContentTitle(song.title)
                .setContentText(song.artist)
                .setContentIntent(contentPendingIntent)
                // Si está pausada, permitimos borrarla (ongoing = false). Si suena, es fija (true).
                .setOngoing(isPlaying)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setStyle(MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2))
                .addAction(R.drawable.ic_previous, "Previous", prevIntent)
                .addAction(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play, "Play/Pause", playPauseIntent)
                .addAction(R.drawable.ic_next, "Next", nextIntent)
                .build()

            // Si está reproduciendo, forzamos Foreground Service (no se muere).
            // Si está pausada, notificamos sin forzar foreground (se puede deslizar).
            if (isPlaying) {
                startForeground(1, notification)
            } else {
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(1, notification)
                // stopForeground(false) se llamó en togglePlayPause, eso permite el swipe
            }
        }
    }

    // --- UTILS ---

    private fun getHighQualityBitmap(drawable: Drawable): Bitmap {
        val originalBitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 512
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 512
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
        return Bitmap.createScaledBitmap(originalBitmap, 1024, 1024, true)
    }

    // Helper para la carga de metadata
    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        return getHighQualityBitmap(drawable)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Controls for music playback"
            channel.setSound(null, null)
            channel.enableVibration(false)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaSession.release()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}