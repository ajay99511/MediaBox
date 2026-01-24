package com.local.offlinemediaplayer.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.local.offlinemediaplayer.model.Playlist
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val file = File(context.filesDir, "playlists.json")

    fun getPlaylists(): List<Playlist> {
        if (!file.exists()) return emptyList()
        return try {
            val json = file.readText()
            val type = object : TypeToken<List<Playlist>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun savePlaylists(playlists: List<Playlist>) {
        try {
            val json = gson.toJson(playlists)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
