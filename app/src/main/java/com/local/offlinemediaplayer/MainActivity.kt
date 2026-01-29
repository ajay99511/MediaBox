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
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
//AppHeader
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.local.offlinemediaplayer.ui.screens.AlbumDetailScreen
import com.local.offlinemediaplayer.ui.screens.AlbumListScreen
import com.local.offlinemediaplayer.ui.screens.NowPlayingScreen
import com.local.offlinemediaplayer.ui.screens.PlaylistDetailScreen
import com.local.offlinemediaplayer.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

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
        // Custom Top Bar (Only visible when not playing fullscreen video)
        topBar = {
            if (currentMedia?.isVideo != true) {
                AppHeader()
            }
        },
        bottomBar = {
            if (currentMedia?.isVideo != true) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Video") },
                        label = { Text("Videos") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Audio") },
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
// Professional Anime-Styled MediaBox Header
// No complications, just beautiful and elegant!

@Composable
fun AppHeader() {
    val greeting = remember { getGreeting() }

    // Animated gradient background with anime aesthetic
    val infiniteTransition = rememberInfiniteTransition(label = "header_animation")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_alpha"
    )

    // Sophisticated anime-style gradient
    val animeGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.background
        ),
        startY = 0f,
        endY = 500f
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(animeGradient)
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Greeting text with subtle animation
        Text(
            text = greeting,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 13.sp,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Header content row with profile
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            // App title with anime elegance
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "MediaBox",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 26.sp,
                    letterSpacing = (-0.5).sp
                )
            }

            // Anime-styled profile indicator with glow effect
            AnimeProfileAvatar(
                initial = "M",
                animatedAlpha = animatedAlpha
            )
        }
    }
}

@Composable
fun AnimeProfileAvatar(
    initial: String,
    animatedAlpha: Float
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    )
                ),
                shape = MaterialTheme.shapes.medium
            )
            // Subtle glow effect
            .shadow(
                elevation = 8.dp,
                shape = MaterialTheme.shapes.medium,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = animatedAlpha * 0.3f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp
        )
    }
}

// Helper function for greeting message
fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning 🌅"
        in 12..16 -> "Good Afternoon ☀️"
        in 17..21 -> "Good Evening 🌙"
        else -> "Welcome Back 🌌"
    }
}


// NEW: Navigation Host for Audio Section Only
@Composable
fun AudioNavigationHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "audio_library" // Fixed start destination name
    ) {
        composable("audio_library") {
            AudioLibraryScreen(
                viewModel = viewModel,
                onNavigateToPlayer = { navController.navigate("now_playing") },
                onNavigateToPlaylist = { id -> navController.navigate("playlist_detail/$id") },
                onNavigateToAlbum = { id -> navController.navigate("album_detail/$id") }
            )
        }
        composable("now_playing") {
            NowPlayingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("playlist_detail/{playlistId}") { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("now_playing") }
            )
        }
        composable("album_detail/{albumId}") { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId")?.toLongOrNull() ?: return@composable
            AlbumDetailScreen(
                albumId = albumId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate("now_playing") }
            )
        }
    }
}

@Composable
fun AudioLibraryScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit,
    onNavigateToAlbum: (Long) -> Unit
) {
    // 0 = Tracks, 1 = Albums, 2 = Playlists
    var selectedSubTab by remember { mutableIntStateOf(0) }

    // Add to Playlist Dialog State
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var songToAdd by remember { mutableStateOf<MediaFile?>(null) }

    // Create Playlist Dialog State
    var showCreateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            // Tab Switcher
            TabRow(selectedTabIndex = selectedSubTab) {
                Tab(selected = selectedSubTab == 0, onClick = { selectedSubTab = 0 }, text = { Text("Tracks") })
                Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Albums") })
                Tab(selected = selectedSubTab == 2, onClick = { selectedSubTab = 2 }, text = { Text("Playlists") })
            }

            when (selectedSubTab) {
                0 -> {
                    // TRACKS VIEW
                    AudioListScreen(
                        viewModel = viewModel,
                        onAudioClick = { file ->
                            viewModel.playMedia(file)
                            onNavigateToPlayer()
                        },
                        onAddToPlaylist = { file ->
                            songToAdd = file
                            showAddToPlaylistDialog = true
                        }
                    )
                }
                1 -> {
                    // ALBUMS VIEW
                    AlbumListScreen(
                        viewModel = viewModel,
                        onAlbumClick = onNavigateToAlbum
                    )
                }
                2 -> {
                    // PLAYLISTS VIEW
                    PlaylistListScreen(
                        viewModel = viewModel,
                        onPlaylistClick = onNavigateToPlaylist,
                        onCreateClick = { showCreateDialog = true }
                    )
                }
            }
        }

        // Mini Player
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            MiniPlayer(
                viewModel = viewModel,
                onTap = onNavigateToPlayer
            )
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name -> viewModel.createPlaylist(name) }
        )
    }

    if (showAddToPlaylistDialog && songToAdd != null) {
        AddToPlaylistDialog(
            song = songToAdd!!,
            viewModel = viewModel,
            onDismiss = { showAddToPlaylistDialog = false },
            onCreateNew = { showCreateDialog = true } // Stack dialogs
        )
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

@Composable
fun AudioListScreen(
    viewModel: MainViewModel,
    onAudioClick: (MediaFile) -> Unit,
    onAddToPlaylist: (MediaFile) -> Unit
) {
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
        LazyColumn(contentPadding = PaddingValues(top = 8.dp)) {
            // Header Buttons
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { viewModel.playAll(false) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play All")
                    }
                    FilledTonalButton(onClick = { viewModel.playAll(true) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Shuffle, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
            }
            items(items = audio, key = { it.id }) { song ->
                AudioListItem(song, onAudioClick, onAddToPlaylist)
            }

            // Padding for MiniPlayer
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun AudioListItem(
    song: MediaFile,
    onAudioClick: (MediaFile) -> Unit,
    onAddToPlaylist: (MediaFile) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist(song)
                        }
                    )
                }
            }
        },
        modifier = Modifier.clickable { onAudioClick(song) }
    )
    HorizontalDivider()
}

@Composable
fun PlaylistListScreen(
    viewModel: MainViewModel,
    onPlaylistClick: (String) -> Unit,
    onCreateClick: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, "Create Playlist")
            }
        }
    ) { padding ->
        if (playlists.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No playlists yet")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(playlists) { playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { Text("${playlist.mediaIds.size} songs") },
                        leadingContent = {
                            Icon(Icons.Default.FormatListNumbered, null, modifier = Modifier.size(40.dp))
                        },
                        modifier = Modifier.clickable { onPlaylistClick(playlist.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(70.dp)) }
            }
        }
    }
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name)
                        onDismiss()
                    }
                }
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    song: MediaFile,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                item {
                    TextButton(
                        onClick = onCreateNew,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("New Playlist")
                    }
                    Divider()
                }
                items(playlists) { playlist ->
                    val isAdded = playlist.mediaIds.contains(song.id)
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        trailingContent = { if (isAdded) Icon(Icons.Default.Check, null) },
                        modifier = Modifier.clickable(enabled = !isAdded) {
                            viewModel.addSongToPlaylist(playlist.id, song.id)
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(UnstableApi::class)
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
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).statusBarsPadding()
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