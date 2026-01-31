package com.local.offlinemediaplayer.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.local.offlinemediaplayer.viewmodel.MainViewModel
import com.local.offlinemediaplayer.viewmodel.ResizeMode
import kotlinx.coroutines.delay
import java.util.Formatter
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val player by viewModel.player.collectAsStateWithLifecycle()
    val resizeMode by viewModel.resizeMode.collectAsStateWithLifecycle()
    val isLocked by viewModel.isPlayerLocked.collectAsStateWithLifecycle()
    val isInPip by viewModel.isInPipMode.collectAsStateWithLifecycle()

    // Force Landscape on Entry, restore on Exit
    DisposableEffect(Unit) {
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        hideSystemBars(activity)

        onDispose {
            activity?.requestedOrientation = originalOrientation
            showSystemBars(activity)
        }
    }

    // Handle Back Press
    BackHandler {
        if (isLocked) {
            // Toast or hint could go here
        } else {
            onBack()
        }
    }

    // PiP Listener (Update ViewModel when PiP changes)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                if (activity?.isInPictureInPictureMode == true) {
                    viewModel.setPipMode(true)
                }
            } else if (event == Lifecycle.Event.ON_RESUME) {
                if (activity?.isInPictureInPictureMode == false) {
                    viewModel.setPipMode(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (player != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        this.useController = false // We build our own
                        this.layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        // Keep screen on
                        this.keepScreenOn = true
                    }
                },
                update = { playerView ->
                    playerView.player = player
                    playerView.resizeMode = when (resizeMode) {
                        ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Custom Overlay Controls (Hidden in PiP)
        if (!isInPip) {
            VideoPlayerControls(
                viewModel = viewModel,
                onBack = onBack,
                onPip = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val params = PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                        activity?.enterPictureInPictureMode(params)
                    }
                },
                onRotate = {
                    if (activity?.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    } else {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                }
            )
        }
    }
}

@Composable
fun VideoPlayerControls(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onRotate: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.currentPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isLocked by viewModel.isPlayerLocked.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val resizeMode by viewModel.resizeMode.collectAsStateWithLifecycle()

    var isVisible by remember { mutableStateOf(true) }

    // Auto-hide controls
    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            delay(3000)
            isVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isVisible = !isVisible
            }
    ) {
        // Gradient Overlays (Top and Bottom) for visibility
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // Top Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                )
                // Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                )
            }
        }

        // LOCK BUTTON (Always visible if locked, on left side)
        if (isVisible || isLocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
            ) {
                IconButton(
                    onClick = { viewModel.toggleLock() },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Outlined.Lock else Icons.Outlined.LockOpen,
                        contentDescription = "Lock",
                        tint = Color.White
                    )
                }
            }
        }

        // --- FULL CONTROLS (Only when visible and NOT locked) ---
        AnimatedVisibility(
            visible = isVisible && !isLocked,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                // 1. Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Back", tint = Color.White)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTrack?.title ?: "Video",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "${currentTrack?.resolution ?: "Unknown"} • ${formatSize(currentTrack?.size ?: 0)}",
                            color = Color(0xFF00E5FF), // Cyan Accent
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // PiP Button
                    IconButton(onClick = onPip) {
                        Icon(Icons.Default.PictureInPictureAlt, "PiP", tint = Color.White)
                    }

                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                    }

                    IconButton(onClick = { /* More */ }) {
                        Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                    }
                }

                // 2. Center Controls (Prev, Rewind, Play, Forward, Next)
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    IconButton(onClick = { viewModel.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "Prev", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    IconButton(onClick = { viewModel.rewind() }) {
                        Icon(Icons.Default.Replay10, "Rewind", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    // Big Play Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .border(2.dp, Color(0xFF00E5FF), CircleShape) // Cyan border
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(onClick = { viewModel.forward() }) {
                        Icon(Icons.Default.Forward10, "Forward", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    IconButton(onClick = { viewModel.playNext() }) {
                        Icon(Icons.Default.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                // 3. Bottom Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                ) {
                    // Time and Duration Labels (Above Seekbar)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(position), color = Color(0xFF00E5FF), style = MaterialTheme.typography.labelMedium) // Current Cyan
                        Text(formatTime(duration), color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    // Seek Bar
                    Slider(
                        value = if (duration > 0) position.toFloat() else 0f,
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.height(20.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Action Row (Resize, Speed, Rotate, Playlist)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Resize Button
                            ControlPill(
                                icon = Icons.Outlined.AspectRatio,
                                text = when(resizeMode) {
                                    ResizeMode.FIT -> "Fit"
                                    ResizeMode.FILL -> "Fill"
                                    ResizeMode.ZOOM -> "Zoom"
                                },
                                onClick = { viewModel.toggleResizeMode() }
                            )

                            // Speed Button
                            ControlPill(
                                icon = Icons.Outlined.Speed,
                                text = "Speed: ${playbackSpeed}x",
                                onClick = { viewModel.cyclePlaybackSpeed() }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(onClick = { /* Lock orientation logic or just visual */ }) {
                                Icon(Icons.Outlined.Lock, "Lock Orientation", tint = Color.Gray)
                            }
                            IconButton(onClick = onRotate) {
                                Icon(Icons.Outlined.ScreenRotation, "Rotate", tint = Color.Gray)
                            }
                            IconButton(onClick = { /* Queue list */ }) {
                                Icon(Icons.AutoMirrored.Filled.FeaturedPlayList, "Queue", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlPill(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.15f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// Helpers
private fun hideSystemBars(activity: Activity?) {
    activity?.window?.let { window ->
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

private fun showSystemBars(activity: Activity?) {
    activity?.window?.let { window ->
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }
}

private fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    val mFormatter = Formatter(StringBuilder(), Locale.getDefault())
    return if (hours > 0) {
        mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
    } else {
        mFormatter.format("%02d:%02d", minutes, seconds).toString()
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}
