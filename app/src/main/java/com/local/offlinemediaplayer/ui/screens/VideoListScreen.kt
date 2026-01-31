package com.local.offlinemediaplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

@Composable
fun VideoListScreen(viewModel: MainViewModel, onVideoClick: (MediaFile) -> Unit) {
    val videos by viewModel.videoList.collectAsStateWithLifecycle()

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
                    "No videos found on device",
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

@Composable
private fun VideoListItem(video: MediaFile, onVideoClick: (MediaFile) -> Unit) {
    ListItem(
        headlineContent = { Text(video.title, maxLines = 1) },
        supportingContent = {
            Text(
                formatDuration(video.duration),
                style = MaterialTheme.typography.bodySmall
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
        modifier = Modifier.clickable { onVideoClick(video) }
    )
    HorizontalDivider()
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