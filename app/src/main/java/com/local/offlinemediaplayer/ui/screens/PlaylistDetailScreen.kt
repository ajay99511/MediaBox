package com.local.offlinemediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val allAudio by viewModel.audioList.collectAsStateWithLifecycle()

    val playlist = playlists.find { it.id == playlistId }

    if (playlist == null) {
        onBack()
        return
    }

    val songs = playlist.mediaIds.mapNotNull { id -> allAudio.find { it.id == id } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deletePlaylist(playlistId)
                        onBack()
                    }) {
                        Icon(Icons.Default.Delete, "Delete Playlist", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.playPlaylist(playlist, false)
                        onNavigateToPlayer()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = songs.isNotEmpty()
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play All")
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.playPlaylist(playlist, true)
                        onNavigateToPlayer()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = songs.isNotEmpty()
                ) {
                    Icon(Icons.Default.Shuffle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }

            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No songs in this playlist", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(songs) { song ->
                        ListItem(
                            headlineContent = { Text(song.title, maxLines = 1) },
                            supportingContent = { Text(song.artist ?: "Unknown", maxLines = 1) },
                            leadingContent = {
                                AsyncImage(
                                    model = song.albumArtUri ?: "android.resource://com.local.offlinemediaplayer/drawable/ic_launcher_foreground",
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).background(Color.DarkGray),
                                    contentScale = ContentScale.Crop
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeSongFromPlaylist(playlistId, song.id) }) {
                                    Icon(Icons.Default.Close, "Remove")
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.playMedia(song)
                                onNavigateToPlayer()
                            }
                        )
                    }
                }
            }
        }
    }
}
