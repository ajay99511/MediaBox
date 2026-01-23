//@file:OptIn(ExperimentalPermissionsApi::class, UnstableApi::class)
//
//package com.local.offlinemediaplayer
//
//import android.Manifest
//import android.os.Build
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import androidx.media3.common.util.UnstableApi
//import androidx.media3.ui.PlayerView
//import coil.compose.AsyncImage
//import com.local.offlinemediaplayer.model.MediaFile
//import com.local.offlinemediaplayer.viewmodel.MainViewModel
//import com.google.accompanist.permissions.ExperimentalPermissionsApi
//import com.google.accompanist.permissions.rememberMultiplePermissionsState
//import dagger.hilt.android.AndroidEntryPoint
//
//@AndroidEntryPoint
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//
//        setContent {
//            MaterialTheme(colorScheme = darkColorScheme()) {
//                MainScreen()
//            }
//        }
//    }
//}
//
//@Composable
//fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
//    val permissions = if (Build.VERSION.SDK_INT >= 33) {
//        listOf(
//            Manifest.permission.READ_MEDIA_VIDEO,
//            Manifest.permission.READ_MEDIA_AUDIO
//        )
//    } else {
//        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
//    }
//
//    val permissionState = rememberMultiplePermissionsState(permissions)
//
//    LaunchedEffect(Unit) {
//        if (!permissionState.allPermissionsGranted) {
//            permissionState.launchMultiplePermissionRequest()
//        } else {
//            viewModel.scanMedia()
//        }
//    }
//
//    if (permissionState.allPermissionsGranted) {
//        MediaPlayerAppContent(viewModel)
//    } else {
//        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            Button(onClick = { permissionState.launchMultiplePermissionRequest() }) {
//                Text("Grant Permissions to Access Media")
//            }
//        }
//    }
//}
//
//@Composable
//fun MediaPlayerAppContent(viewModel: MainViewModel) {
//    var selectedTab by remember { mutableIntStateOf(0) }
//    var currentMedia by remember { mutableStateOf<MediaFile?>(null) }
//
//    Scaffold(
//        bottomBar = {
//            if (currentMedia?.isVideo != true) {
//                NavigationBar {
//                    NavigationBarItem(
//                        icon = { Icon(Icons.Default.Home, contentDescription = "Video") },
//                        label = { Text("Video") },
//                        selected = selectedTab == 0,
//                        onClick = { selectedTab = 0 }
//                    )
//
//                    NavigationBarItem(
//                        icon = { Icon(Icons.Default.Settings, contentDescription = "Audio") },
//                        label = { Text("Audio") },
//                        selected = selectedTab == 1,
//                        onClick = { selectedTab = 1 }
//                    )
//                }
//            }
//        }
//    ) { padding ->
//        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
//            if (currentMedia != null && currentMedia!!.isVideo) {
//                VideoPlayerScreen(viewModel = viewModel, onBack = {
//                    currentMedia = null
//                    viewModel.player.value?.pause()
//                })
//            } else {
//                when (selectedTab) {
//                    0 -> VideoListScreen(viewModel) { file ->
//                        currentMedia = file
//                        viewModel.playMedia(file)
//                    }
//                    else -> AudioListScreen(viewModel) { file ->
//                        viewModel.playMedia(file)
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//fun VideoListScreen(viewModel: MainViewModel, onVideoClick: (MediaFile) -> Unit) {
//    val videos by viewModel.videoList.collectAsStateWithLifecycle()
//
//    if (videos.isEmpty()) {
//        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            Text("No videos found on device")
//        }
//    } else {
//        LazyColumn {
//            items(items = videos, key = { it.id }) { video ->
//                VideoListItem(video, onVideoClick)
//            }
//        }
//    }
//}
//
//@Composable
//private fun VideoListItem(video: MediaFile, onVideoClick: (MediaFile) -> Unit) {
//    ListItem(
//        headlineContent = { Text(video.title, maxLines = 1) },
//        leadingContent = {
//            AsyncImage(
//                model = video.uri,
//                contentDescription = video.title,
//                modifier = Modifier.size(80.dp).background(Color.Gray),
//                contentScale = ContentScale.Crop
//            )
//        },
//        modifier = Modifier.clickable { onVideoClick(video) }
//    )
//    HorizontalDivider()
//}
//
//@Composable
//fun AudioListScreen(viewModel: MainViewModel, onAudioClick: (MediaFile) -> Unit) {
//    val audio by viewModel.audioList.collectAsStateWithLifecycle()
//
//    if (audio.isEmpty()) {
//        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
//            Text("No audio found on device")
//        }
//    } else {
//        LazyColumn {
//            items(items = audio, key = { it.id }) { song ->
//                AudioListItem(song, onAudioClick)
//            }
//        }
//    }
//}
//
//@Composable
//private fun AudioListItem(song: MediaFile, onAudioClick: (MediaFile) -> Unit) {
//    ListItem(
//        headlineContent = { Text(song.title, maxLines = 1) },
//        supportingContent = { Text(song.artist ?: "Unknown Artist", maxLines = 1) },
//        leadingContent = {
//            AsyncImage(
//                model = song.albumArtUri ?: "android.resource://com.local.offlinemediaplayer/drawable/ic_launcher_foreground",
//                contentDescription = song.title,
//                modifier = Modifier.size(50.dp).background(Color.DarkGray),
//                contentScale = ContentScale.Crop
//            )
//        },
//        modifier = Modifier.clickable { onAudioClick(song) }
//    )
//    HorizontalDivider()
//}
//
//@Composable
//fun VideoPlayerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
//    val player by viewModel.player.collectAsStateWithLifecycle()
//
//    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
//        if (player != null) {
//            AndroidView(
//                factory = { context ->
//                    PlayerView(context).apply {
//                        this.player = player
//                        this.useController = true
//                        this.setShowNextButton(false)
//                        this.setShowPreviousButton(false)
//                    }
//                },
//                modifier = Modifier.fillMaxSize()
//            )
//        }
//
//        IconButton(
//            onClick = onBack,
//            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
//        ) {
//            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
//        }
//    }
//}


@file:OptIn(UnstableApi::class)

package com.local.offlinemediaplayer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.ui.components.MiniPlayer
import com.local.offlinemediaplayer.ui.screens.NowPlayingScreen
import com.local.offlinemediaplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current

    // Define permissions based on Android version
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    // Track permission state
    var permissionsGranted by remember {
        mutableStateOf(
            permissions.all { permission ->
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PermissionChecker.PERMISSION_GRANTED
            }
        )
    }

    var shouldShowRationale by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        permissionsGranted = allGranted

        if (allGranted) {
            viewModel.scanMedia()
        } else {
            // Check if we should show rationale
            shouldShowRationale = permissions.any { permission ->
                (context as? ComponentActivity)?.shouldShowRequestPermissionRationale(permission) == true
            }
        }
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            viewModel.scanMedia()
        }
    }

    // Main content or permission request UI
    when {
        permissionsGranted -> {
            MediaPlayerAppContent(viewModel)
        }
        shouldShowRationale -> {
            PermissionRationaleScreen(
                onRequestPermission = {
                    permissionLauncher.launch(permissions.toTypedArray())
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
        else -> {
            PermissionRequestScreen(
                onRequestPermission = {
                    permissionLauncher.launch(permissions.toTypedArray())
                }
            )
        }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Storage Permission Required",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "This app needs access to your media files to play videos and music.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun PermissionRationaleScreen(
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Permission Denied",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Storage permission is required to access your media files. Please grant the permission to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try Again")
            }

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open App Settings")
            }
        }
    }
}

@Composable
fun MediaPlayerAppContent(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentMedia by remember { mutableStateOf<MediaFile?>(null) }

    Scaffold(
        bottomBar = {
            if (currentMedia?.isVideo != true) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Video") },
                        label = { Text("Videos") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Audio") },
                        label = { Text("Music") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (currentMedia != null && currentMedia!!.isVideo) {
                VideoPlayerScreen(viewModel = viewModel, onBack = {
                    currentMedia = null
                    viewModel.player.value?.pause()
                })
            } else {
                when (selectedTab) {
                    0 -> VideoListScreen(viewModel) { file ->
                        currentMedia = file
                        viewModel.playMedia(file)
                    }
                    // Audio tab now has navigation
                    else -> AudioNavigationHost(viewModel)
                }
            }
        }
    }
}

// NEW: Navigation Host for Audio Section Only
@Composable
fun AudioNavigationHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "audio_list"
    ) {
        composable("audio_list") {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Audio List takes available space
                    Box(modifier = Modifier.weight(1f)) {
                        AudioListScreen(viewModel) { file ->
                            viewModel.playMedia(file)
                            // Navigate to Now Playing
                            navController.navigate("now_playing")
                        }
                    }

                    // Mini Player at bottom
                    MiniPlayer(
                        viewModel = viewModel,
                        onTap = {
                            navController.navigate("now_playing")
                        }
                    )
                }
            }
        }

        composable("now_playing") {
            NowPlayingScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

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
        LazyColumn {
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

@Composable
fun AudioListScreen(viewModel: MainViewModel, onAudioClick: (MediaFile) -> Unit) {
    val audio by viewModel.audioList.collectAsStateWithLifecycle()

    if (audio.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "No audio found on device",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn {
            items(items = audio, key = { it.id }) { song ->
                AudioListItem(song, onAudioClick)
            }
        }
    }
}

@Composable
private fun AudioListItem(song: MediaFile, onAudioClick: (MediaFile) -> Unit) {
    ListItem(
        headlineContent = { Text(song.title, maxLines = 1) },
        supportingContent = {
            Text(
                "${song.artist ?: "Unknown Artist"} • ${formatDuration(song.duration)}",
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            AsyncImage(
                model = song.albumArtUri ?: "android.resource://com.local.offlinemediaplayer/drawable/ic_launcher_foreground",
                contentDescription = song.title,
                modifier = Modifier.size(50.dp).background(Color.DarkGray),
                contentScale = ContentScale.Crop
            )
        },
        modifier = Modifier.clickable { onAudioClick(song) }
    )
    HorizontalDivider()
}

@Composable
fun VideoPlayerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val player by viewModel.player.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (player != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        this.player = player
                        this.useController = true
                        this.setShowNextButton(false)
                        this.setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

// Utility function to format duration
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


