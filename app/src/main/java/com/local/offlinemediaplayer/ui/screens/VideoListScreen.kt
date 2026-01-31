package com.local.offlinemediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
fun VideoListScreen(
    viewModel: MainViewModel,
    onVideoClick: (MediaFile) -> Unit,
    videoListOverride: List<MediaFile>? = null,
    title: String? = null,
    onBack: (() -> Unit)? = null
) {
    val videosState by viewModel.videoList.collectAsStateWithLifecycle()
    val videos = videoListOverride ?: videosState

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0B0B0F))
    ) {
        // Optional Header for Folder View
        if (title != null && onBack != null) {
            TopAppBar(
                title = { Text(title, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0B0F))
            )
        }

        if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No videos found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 8.dp)) {
                items(items = videos, key = { it.id }) { video ->
                    VideoListItem(video, onVideoClick)
                }
            }
        }
    }
}

@Composable
private fun VideoListItem(video: MediaFile, onVideoClick: (MediaFile) -> Unit) {
    ListItem(
        headlineContent = { Text(video.title, maxLines = 1, color = Color.White) },
        supportingContent = {
            Text(
                formatDuration(video.duration),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        },
        leadingContent = {
            AsyncImage(
                model = video.uri,
                contentDescription = video.title,
                modifier = Modifier.size(80.dp).background(Color.Gray),
                contentScale = ContentScale.Crop
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onVideoClick(video) }
    )
    HorizontalDivider(color = Color(0xFF1E1E24))
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
