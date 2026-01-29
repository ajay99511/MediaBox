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
import com.local.offlinemediaplayer.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val allAudio by viewModel.audioList.collectAsStateWithLifecycle()

    val album = albums.find { it.id == albumId }
    val albumSongs = allAudio.filter { it.albumId == albumId }

    if (album == null) {
        onBack()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(album.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                        viewModel.playAlbum(album, false)
                        onNavigateToPlayer()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = albumSongs.isNotEmpty()
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Play")
                }

                FilledTonalButton(
                    onClick = {
                        viewModel.playAlbum(album, true)
                        onNavigateToPlayer()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = albumSongs.isNotEmpty()
                ) {
                    Icon(Icons.Default.Shuffle, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle")
                }
            }

            LazyColumn {
                items(albumSongs) { song ->
                    ListItem(
                        headlineContent = { Text(song.title, maxLines = 1) },
                        supportingContent = { Text(formatDuration(song.duration), maxLines = 1) },
                        leadingContent = {
                            AsyncImage(
                                model = song.albumArtUri ?: "android.resource://com.local.offlinemediaplayer/drawable/ic_launcher_foreground",
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).background(Color.DarkGray),
                                contentScale = ContentScale.Crop
                            )
                        },
                        modifier = Modifier.clickable {
                            viewModel.playMedia(song)
                            onNavigateToPlayer()
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%d:%02d", minutes, seconds)
}