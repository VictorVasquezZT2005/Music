package com.example.music.utils // O el paquete que prefieras

import android.content.Context

class ImageCache(context: Context) {
    private val prefs = context.getSharedPreferences("deezer_image_cache", Context.MODE_PRIVATE)

    // Intenta obtener el link guardado
    fun getUrl(key: String): String? {
        return prefs.getString(key, null)
    }

    // Guarda el link para el futuro
    fun saveUrl(key: String, url: String) {
        prefs.edit().putString(key, url).apply()
    }
}