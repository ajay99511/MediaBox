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
import com.local.offlinemediaplayer.model.Album
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.model.Playlist
import com.local.offlinemediaplayer.repository.PlaylistRepository
import com.local.offlinemediaplayer.service.PlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    private val app: Application,
    private val playlistRepository: PlaylistRepository
) : AndroidViewModel(app) {

    // Media Lists
    private val _videoList = MutableStateFlow<List<MediaFile>>(emptyList())
    val videoList = _videoList.asStateFlow()

    private val _audioList = MutableStateFlow<List<MediaFile>>(emptyList())
    val audioList = _audioList.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums = _albums.asStateFlow()

    // Playlist State
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    // Player State
    private val _player = MutableStateFlow<MediaController?>(null)
    val player = _player.asStateFlow()

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
    val duration = _duration.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var positionUpdateJob: Job? = null

    init {
        initializeMediaController()
        loadPlaylists()
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
                    Player.STATE_ENDED -> playNext()
                    Player.STATE_READY -> {
                        _duration.value = controller.duration.coerceAtLeast(0L)
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startPositionUpdates() else stopPositionUpdates()
            }
        })
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = viewModelScope.launch {
            while (isActive) {
                _player.value?.let { player ->
                    _currentPosition.value = player.currentPosition
                    _duration.value = player.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // --- Media Loading ---

    fun scanMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            _videoList.value = queryMedia(isVideo = true)
            _audioList.value = queryMedia(isVideo = false)
            _albums.value = queryAlbums()
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
                    var albumId: Long = -1

                    if (!isVideo) {
                        artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                        albumId = cursor.getLong(albumIdColumn)
                        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                        albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                    }

                    mediaList.add(MediaFile(id, contentUri, name, artist, duration, isVideo, albumArtUri, albumId))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaList
    }

    private fun queryAlbums(): List<Album> {
        val albumList = mutableListOf<Album>()
        val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        try {
            app.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val countColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(albumColumn) ?: "Unknown Album"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val count = cursor.getInt(countColumn)

                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, id)

                    albumList.add(Album(id, name, artist, count, albumArtUri))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return albumList
    }

    // --- Playlist Management ---

    private fun loadPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            _playlists.value = playlistRepository.getPlaylists()
        }
    }

    fun createPlaylist(name: String) {
        val newPlaylist = Playlist(name = name)
        val updated = _playlists.value + newPlaylist
        _playlists.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.savePlaylists(updated)
        }
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.savePlaylists(updated)
        }
    }

    fun addSongToPlaylist(playlistId: String, mediaId: Long) {
        val playlist = _playlists.value.find { it.id == playlistId } ?: return
        if (playlist.mediaIds.contains(mediaId)) return

        val updatedPlaylist = playlist.copy(mediaIds = playlist.mediaIds + mediaId)
        val updatedList = _playlists.value.map { if (it.id == playlistId) updatedPlaylist else it }

        _playlists.value = updatedList
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.savePlaylists(updatedList)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, mediaId: Long) {
        val playlist = _playlists.value.find { it.id == playlistId } ?: return
        val updatedPlaylist = playlist.copy(mediaIds = playlist.mediaIds - mediaId)
        val updatedList = _playlists.value.map { if (it.id == playlistId) updatedPlaylist else it }

        _playlists.value = updatedList
        viewModelScope.launch(Dispatchers.IO) {
            playlistRepository.savePlaylists(updatedList)
        }
    }

    // --- Playback Logic ---

    fun playMedia(media: MediaFile) {
        if (media.isVideo) {
            playMediaItem(media)
        } else {
            val allAudioFiles = _audioList.value
            val startIndex = allAudioFiles.indexOfFirst { it.id == media.id }
            if (startIndex >= 0) {
                setQueue(allAudioFiles, startIndex, false)
            }
        }
    }

    fun playPlaylist(playlist: Playlist, shuffle: Boolean) {
        val allAudio = _audioList.value
        val playlistSongs = playlist.mediaIds.mapNotNull { id ->
            allAudio.find { it.id == id }
        }
        if (playlistSongs.isNotEmpty()) {
            val startIndex = if (shuffle) (playlistSongs.indices).random() else 0
            setQueue(playlistSongs, startIndex, shuffle)
        }
    }

    fun playAlbum(album: Album, shuffle: Boolean) {
        val albumSongs = _audioList.value.filter { it.albumId == album.id }
        if (albumSongs.isNotEmpty()) {
            val startIndex = if (shuffle) (albumSongs.indices).random() else 0
            setQueue(albumSongs, startIndex, shuffle)
        }
    }

    fun playAll(shuffle: Boolean) {
        val allAudio = _audioList.value
        if (allAudio.isNotEmpty()) {
            val startIndex = if (shuffle) (allAudio.indices).random() else 0
            setQueue(allAudio, startIndex, shuffle)
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

    fun setQueue(songs: List<MediaFile>, startIndex: Int, shuffle: Boolean = false) {
        if (songs.isEmpty() || startIndex !in songs.indices) return

        _originalQueue.value = songs
        _isShuffleEnabled.value = shuffle

        _currentQueue.value = if (shuffle) {
            createShuffledQueue(songs, startIndex)
        } else {
            songs
        }

        _currentIndex.value = if (shuffle) 0 else startIndex
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
        _currentTrack.value = if (index != null && index in queue.indices) queue[index] else null
    }

    private fun playCurrentTrack() {
        _currentTrack.value?.let { track -> playMediaItem(track) }
    }

    // --- Controls ---

    fun playNext() {
        val currentIdx = _currentIndex.value ?: return
        val queue = _currentQueue.value
        if (queue.isEmpty()) return

        val nextIndex = if (currentIdx < queue.size - 1) currentIdx + 1 else 0
        _currentIndex.value = nextIndex
        updateCurrentTrack()
        playCurrentTrack()
    }

    fun playPrevious() {
        val currentIdx = _currentIndex.value ?: return
        val queue = _currentQueue.value
        if (queue.isEmpty()) return

        if (_currentPosition.value > 3000) {
            seekTo(0)
            return
        }

        val previousIndex = if (currentIdx > 0) currentIdx - 1 else queue.size - 1
        _currentIndex.value = previousIndex
        updateCurrentTrack()
        playCurrentTrack()
    }

    fun togglePlayPause() {
        _player.value?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun toggleShuffle() {
        val currentTrack = _currentTrack.value ?: return
        val originalQueue = _originalQueue.value
        val newShuffleState = !_isShuffleEnabled.value
        _isShuffleEnabled.value = newShuffleState

        if (newShuffleState) {
            val originalIndex = originalQueue.indexOfFirst { it.id == currentTrack.id }
            _currentQueue.value = createShuffledQueue(originalQueue, originalIndex)
            _currentIndex.value = 0
        } else {
            _currentQueue.value = originalQueue
            _currentIndex.value = originalQueue.indexOfFirst { it.id == currentTrack.id }
        }
        updateCurrentTrack()
    }

    fun seekTo(positionMs: Long) {
        _player.value?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun hasNext() = _currentIndex.value?.let { it < _currentQueue.value.size - 1 } ?: false
    fun hasPrevious() = _currentIndex.value?.let { it > 0 } ?: false

    override fun onCleared() {
        super.onCleared()
        stopPositionUpdates()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}