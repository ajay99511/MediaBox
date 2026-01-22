package com.local.offlinemediaplayer.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    private val _videoList = MutableStateFlow<List<MediaFile>>(emptyList())
    val videoList = _videoList.asStateFlow()

    private val _audioList = MutableStateFlow<List<MediaFile>>(emptyList())
    val audioList = _audioList.asStateFlow()

    private val _player = MutableStateFlow<MediaController?>(null)
    val player = _player.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null

    init {
        initializeMediaController()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                _player.value = controllerFuture?.get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    fun scanMedia() {
        viewModelScope.launch {
            _videoList.value = queryMedia(isVideo = true)
            _audioList.value = queryMedia(isVideo = false)
        }
    }

    private fun queryMedia(isVideo: Boolean): List<MediaFile> {
        val mediaList = mutableListOf<MediaFile>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
            if (isVideo) MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = if (isVideo) {
            arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION)
        } else {
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID)
        }

        try {
            app.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(if (isVideo) MediaStore.Video.Media._ID else MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(if (isVideo) MediaStore.Video.Media.DISPLAY_NAME else MediaStore.Audio.Media.TITLE)
                val durationColumn = cursor.getColumnIndexOrThrow(if (isVideo) MediaStore.Video.Media.DURATION else MediaStore.Audio.Media.DURATION)
                val artistColumn = if (!isVideo) cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST) else -1
                val albumIdColumn = if (!isVideo) cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val duration = cursor.getLong(durationColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    var artist = ""
                    var albumArtUri: Uri? = null

                    if (!isVideo) {
                        artist = cursor.getString(artistColumn)
                        val albumId = cursor.getLong(albumIdColumn)
                        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                        albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                    }

                    mediaList.add(MediaFile(id, contentUri, name, artist, duration, isVideo, albumArtUri))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return mediaList
    }

    fun playMedia(media: MediaFile) {
        _player.value?.let { exoPlayer ->
            val mediaItem = MediaItem.Builder()
                .setUri(media.uri)
                .setMediaId(media.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(media.title)
                        .setArtist(media.artist)
                        .setArtworkUri(media.albumArtUri)
                        .build()
                )
                .build()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    override fun onCleared() {
        super.onCleared()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
