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

    // --- NUEVO: Manejo de Carpetas Excluidas ---

    // Guardamos las rutas (paths) que el usuario NO quiere escanear
    fun saveExcludedFolders(paths: Set<String>) {
        sharedPreferences.edit().putStringSet("excluded_folders", paths).apply()
    }

    fun getExcludedFolders(): Set<String> {
        return sharedPreferences.getStringSet("excluded_folders", emptySet()) ?: emptySet()
    }
}