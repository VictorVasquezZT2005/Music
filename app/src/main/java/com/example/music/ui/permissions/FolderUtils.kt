package com.example.music.ui.permissions

import android.content.Context
import android.provider.MediaStore
import java.io.File

data class AudioFolder(
    val path: String,
    val name: String,
    val songCount: Int
)

fun scanAudioFolders(context: Context): List<AudioFolder> {
    val foldersMap = mutableMapOf<String, Int>() // Path -> Cantidad de canciones

    val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(MediaStore.Audio.Media.DATA) // Necesitamos la ruta del archivo
    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    try {
        val cursor = context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            null
        )

        cursor?.use {
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val filePath = it.getString(dataColumn)
                // Obtenemos la carpeta padre del archivo
                val file = File(filePath)
                val parentPath = file.parent ?: continue

                // Contamos cuántas canciones hay en esa carpeta
                val count = foldersMap[parentPath] ?: 0
                foldersMap[parentPath] = count + 1
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Convertimos el mapa a una lista de objetos AudioFolder
    return foldersMap.map { (path, count) ->
        val folderName = File(path).name
        AudioFolder(path, folderName, count)
    }.sortedBy { it.name } // Ordenar alfabéticamente
}