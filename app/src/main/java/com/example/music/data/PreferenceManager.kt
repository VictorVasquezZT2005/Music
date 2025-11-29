package com.example.music.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MusicAppPrefs", Context.MODE_PRIVATE)

    fun isFirstRun(): Boolean {
        return sharedPreferences.getBoolean("is_first_run", true)
    }

    fun setFirstRunCompleted() {
        sharedPreferences.edit().putBoolean("is_first_run", false).apply()
    }

    // --- Manejo de Carpetas Excluidas ---

    fun saveExcludedFolders(paths: Set<String>) {
        sharedPreferences.edit().putStringSet("excluded_folders", paths).apply()
    }

    fun getExcludedFolders(): Set<String> {
        return sharedPreferences.getStringSet("excluded_folders", emptySet()) ?: emptySet()
    }

    // --- NUEVO: Manejo de IDs de canciones favoritas ---

    fun getFavoriteSongIds(): Set<String> {
        // Almacenamos los IDs como Strings en un Set para persistencia.
        return sharedPreferences.getStringSet("favorite_song_ids", emptySet()) ?: emptySet()
    }

    fun saveFavoriteSongIds(ids: Set<String>) {
        sharedPreferences.edit().putStringSet("favorite_song_ids", ids).apply()
    }
}