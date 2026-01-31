package com.local.offlinemediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.viewmodel.MainViewModel

@Composable
fun AlbumDetailScreen(
    albumId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val allAudio by viewModel.audioList.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    val album = albums.find { it.id == albumId }
    val albumSongs = allAudio.filter { it.albumId == albumId }

    // Check if album is "Favorited" (i.e., all its songs are in Favorites playlist)
    val favPlaylist = playlists.find { it.name == "Favorites" }
    val isFavorite = if (favPlaylist != null && albumSongs.isNotEmpty()) {
        albumSongs.all { favPlaylist.mediaIds.contains(it.id) }
    } else false

    if (album == null) {
        onBack()
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Background Image (Full Screen with Gradient)
        AsyncImage(
            model = album.albumArtUri ?: "android.resource://com.local.offlinemediaplayer/drawable/ic_launcher_foreground",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. Gradient Overlay (Transparent top -> Solid Black bottom)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f), // Top slightly dark
                            Color.Black.copy(alpha = 0.8f), // Mid dark
                            Color.Black // Bottom solid black
                        ),
                        startY = 0f,
                        endY = 1500f // Adjust gradient point
                    )
                )
        )

        // 3. Content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button with semi-transparent background
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp) // Space for MiniPlayer
            ) {
                // Header Content
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Small Album Art Card
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(8.dp),
                            modifier = Modifier.size(140.dp)
                        ) {
                            AsyncImage(
                                model = album.albumArtUri ?: "android.resource://com.local.offlinemediaplayer/drawable/ic_launcher_foreground",
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // "ALBUM" Label
                        Text(
                            text = "ALBUM",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFFFD600), // Yellow/Gold
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Album Title
                        Text(
                            text = album.name,
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            lineHeight = 40.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Metadata Row (Artist Image + Info)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Artist Avatar (Placeholder or re-use album art)
                            AsyncImage(
                                model = album.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${album.artist} • ${album.songCount} Songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.LightGray
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Buttons Row (Play, Heart, More)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Big Yellow Play Button
                            Button(
                                onClick = {
                                    if (albumSongs.isNotEmpty()) {
                                        viewModel.playAlbum(album, false)
                                        onNavigateToPlayer()
                                    }
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            // Heart Icon (Favorites)
                            IconButton(
                                onClick = { viewModel.toggleAlbumInFavorites(albumSongs) }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color(0xFFFFD600) else Color.White, // Yellow if active
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // More Options
                            IconButton(onClick = { /* TODO: More options */ }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    // List Headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(32.dp)
                        )
                        Text(
                            text = "TITLE",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Duration",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 24.dp))
                }

                // Song List
                itemsIndexed(albumSongs) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.playMedia(song)
                                onNavigateToPlayer()
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Index
                        Text(
                            text = "${index + 1}",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(32.dp)
                        )

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist ?: "Unknown",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Duration
                        Text(
                            text = formatDuration(song.duration),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
