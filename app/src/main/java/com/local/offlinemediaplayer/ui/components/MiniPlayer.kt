package com.local.offlinemediaplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.local.offlinemediaplayer.viewmodel.MainViewModel

@Composable
fun MiniPlayer(
    viewModel: MainViewModel,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    // Gradient brush matching the Play button and Progress bar (Cyan -> Purple -> Pink)
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF2BE4DC), // Cyan
            Color(0xFF9656CE), // Purple
            Color(0xFFE44CD8)  // Pink
        )
    )

    // Only show if there's a current track
    currentTrack?.let { track ->
        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp) // Fixed height to match design
                .clickable(onClick = onTap),
            color = Color(0xFF181818), // Deep dark grey background
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Gradient Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.White.copy(alpha = 0.1f)) // Faint track
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .background(gradientBrush)
                    )
                }

                // 2. Main Content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Album Art (Rounded Square)
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(48.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        AsyncImage(
                            model = track.albumArtUri ?: "android.resource://com.local.offlinemediaplayer/drawable/ic_launcher_foreground",
                            contentDescription = track.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Track Title & Artist
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = track.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray, // Grey artist text
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Play/Pause Button (Gradient Circle)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(gradientBrush)
                                .clickable { viewModel.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Next Button (Simple Grey Outline style)
                        IconButton(
                            onClick = { viewModel.playNext() },
                            enabled = viewModel.hasNext()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = Color.Gray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}