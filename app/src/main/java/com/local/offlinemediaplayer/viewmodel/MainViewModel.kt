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
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    // Existing state
    private val _videoList = MutableStateFlow<List<MediaFile>>(emptyList())
    val videoList = _videoList.asStateFlow()

    private val _audioList = MutableStateFlow<List<MediaFile>>(emptyList())
    val audioList = _audioList.asStateFlow()

    private val _player = MutableStateFlow<MediaController?>(null)
    val player = _player.asStateFlow()

    // NEW: Playlist State
    private val _currentQueue = MutableStateFlow<List<MediaFile>>(emptyList())
    val currentQueue = _currentQueue.asStateFlow()

    private val _originalQueue = MutableStateFlow<List<MediaFile>>(emptyList())

    private val _currentIndex = MutableStateFlow<Int?>(null)
    val currentIndex = _currentIndex.asStateFlow()

    private val _currentTrack = MutableStateFlow<MediaFile?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled = _isShuffleEnabled.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _currentPosition.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionUpdateJob: Job? = null

    init {
        initializeMediaController()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                _player.value = controller
                setupPlayerListener(controller)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener(controller: MediaController?) {
        controller?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        // Auto-play next song
                        playNext()
                    }
                    Player.STATE_READY -> {
                        _duration.value = controller.duration
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPositionUpdates()
                } else {
                    stopPositionUpdates()
                }
            }
        })
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                _player.value?.let { player ->
                    _currentPosition.value = player.currentPosition
                    _duration.value = player.duration
                }
                delay(500) // Update every 500ms
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
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

    // NEW: Play media with queue setup
    fun playMedia(media: MediaFile) {
        if (media.isVideo) {
            // Video playback (existing behavior - single item)
            playMediaItem(media)
        } else {
            // Audio playback - set up queue
            val allAudioFiles = _audioList.value
            val startIndex = allAudioFiles.indexOfFirst { it.id == media.id }
            if (startIndex >= 0) {
                setQueue(allAudioFiles, startIndex)
            }
        }
    }

    private fun playMediaItem(media: MediaFile) {
        _player.value?.let { controller ->
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
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }
    }

    // NEW: Queue Management
    fun setQueue(songs: List<MediaFile>, startIndex: Int) {
        if (songs.isEmpty() || startIndex !in songs.indices) return

        _originalQueue.value = songs
        _currentQueue.value = if (_isShuffleEnabled.value) {
            createShuffledQueue(songs, startIndex)
        } else {
            songs
        }

        _currentIndex.value = if (_isShuffleEnabled.value) 0 else startIndex
        updateCurrentTrack()
        playCurrentTrack()
    }

    private fun createShuffledQueue(songs: List<MediaFile>, currentIndex: Int): List<MediaFile> {
        val currentSong = songs[currentIndex]
        val otherSongs = songs.toMutableList().apply { removeAt(currentIndex) }.shuffled()
        return listOf(currentSong) + otherSongs
    }

    private fun updateCurrentTrack() {
        val index = _currentIndex.value
        val queue = _currentQueue.value
        _currentTrack.value = if (index != null && index in queue.indices) {
            queue[index]
        } else {
            null
        }
    }

    private fun playCurrentTrack() {
        _currentTrack.value?.let { track ->
            playMediaItem(track)
        }
    }

    // NEW: Playback Controls
    fun playNext() {
        val currentIdx = _currentIndex.value ?: return
        val queue = _currentQueue.value

        if (queue.isEmpty()) return

        val nextIndex = if (currentIdx < queue.size - 1) {
            currentIdx + 1
        } else {
            0 // Loop to beginning
        }

        _currentIndex.value = nextIndex
        updateCurrentTrack()
        playCurrentTrack()
    }

    fun playPrevious() {
        val currentIdx = _currentIndex.value ?: return
        val queue = _currentQueue.value

        if (queue.isEmpty()) return

        // If more than 3 seconds into song, restart current song
        if (_currentPosition.value > 3000) {
            seekTo(0)
            return
        }

        val previousIndex = if (currentIdx > 0) {
            currentIdx - 1
        } else {
            queue.size - 1 // Loop to end
        }

        _currentIndex.value = previousIndex
        updateCurrentTrack()
        playCurrentTrack()
    }

    fun togglePlayPause() {
        _player.value?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun toggleShuffle() {
        val currentTrack = _currentTrack.value ?: return
        val originalQueue = _originalQueue.value

        _isShuffleEnabled.value = !_isShuffleEnabled.value

        if (_isShuffleEnabled.value) {
            // Enable shuffle
            val originalIndex = originalQueue.indexOfFirst { it.id == currentTrack.id }
            _currentQueue.value = createShuffledQueue(originalQueue, originalIndex)
            _currentIndex.value = 0 // Current song is now at index 0
        } else {
            // Disable shuffle - restore original order
            _currentQueue.value = originalQueue
            _currentIndex.value = originalQueue.indexOfFirst { it.id == currentTrack.id }
        }

        updateCurrentTrack()
    }

    fun seekTo(positionMs: Long) {
        _player.value?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    // Helper functions
    fun hasNext(): Boolean {
        val currentIdx = _currentIndex.value ?: return false
        val queue = _currentQueue.value
        return queue.isNotEmpty() && currentIdx < queue.size - 1
    }

    fun hasPrevious(): Boolean {
        val currentIdx = _currentIndex.value ?: return false
        val queue = _currentQueue.value
        return queue.isNotEmpty() && currentIdx > 0
    }

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
