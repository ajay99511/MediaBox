package com.local.offlinemediaplayer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.ui.components.MiniPlayer
import com.local.offlinemediaplayer.ui.screens.NowPlayingScreen
import com.local.offlinemediaplayer.ui.screens.PlaylistDetailScreen
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
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    var permissionsGranted by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
        })
    }
    var shouldShowRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        permissionsGranted = allGranted
        if (allGranted) {
            viewModel.scanMedia()
        } else {
            shouldShowRationale = true
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) permissionLauncher.launch(permissions.toTypedArray())
        else viewModel.scanMedia()
    }

    if (permissionsGranted) {
        MediaPlayerAppContent(viewModel)
    } else {
        // Simple permission fallback UI
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { permissionLauncher.launch(permissions.toTypedArray()) }) {
                Text("Grant Permissions")
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
                        icon = { Icon(Icons.Default.VideoLibrary, "Videos") },
                        label = { Text("Videos") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LibraryMusic, "Music") },
                        label = { Text("Music") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            //bottom = padding.calculateBottomPadding()
            if (currentMedia?.isVideo == true) {
                VideoPlayerScreen(viewModel, onBack = {
                    currentMedia = null
                    viewModel.player.value?.pause()
                })
            } else {
                when (selectedTab) {
                    0 -> VideoListScreen(viewModel) { file ->
                        currentMedia = file
                        viewModel.playMedia(file)
                    }
                    1 -> AudioNavigationHost(viewModel)
                }
            }
        }
    }
}

@Composable
fun AudioNavigationHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "audio_library") {
        composable("audio_library") {
            AudioLibraryScreen(
                viewModel = viewModel,
                onNavigateToPlayer = { navController.navigate("now_playing") },
                onNavigateToPlaylist = { id -> navController.navigate("playlist_detail/$id") }
            )
        }
        composable("now_playing") {
            NowPlayingScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
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
    }
}

@Composable
fun AudioLibraryScreen(
    viewModel: MainViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPlaylist: (String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Tracks, 1 = Playlists

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
                Tab(selected = selectedSubTab == 1, onClick = { selectedSubTab = 1 }, text = { Text("Playlists") })
            }

            if (selectedSubTab == 0) {
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
            } else {
                // PLAYLISTS VIEW
                PlaylistListScreen(
                    viewModel = viewModel,
                    onPlaylistClick = onNavigateToPlaylist,
                    onCreateClick = { showCreateDialog = true }
                )
            }
        }

        // Mini Player
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            MiniPlayer(viewModel = viewModel, onTap = onNavigateToPlayer)
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
fun AudioListScreen(
    viewModel: MainViewModel,
    onAudioClick: (MediaFile) -> Unit,
    onAddToPlaylist: (MediaFile) -> Unit
) {
    val audio by viewModel.audioList.collectAsStateWithLifecycle()

    if (audio.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No audio found")
        }
    } else {
        LazyColumn {
            // Header Buttons
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

            items(audio, key = { it.id }) { song ->
                AudioListItem(song, onAudioClick, onAddToPlaylist)
            }

            item { Spacer(modifier = Modifier.height(70.dp)) } // Spacer for MiniPlayer
        }
    }
}

@Composable
fun AudioListItem(
    song: MediaFile,
    onAudioClick: (MediaFile) -> Unit,
    onAddToPlaylist: (MediaFile) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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

// Keeping existing Video related components...
@Composable
fun VideoListScreen(viewModel: MainViewModel, onVideoClick: (MediaFile) -> Unit) {
    val videos by viewModel.videoList.collectAsStateWithLifecycle()
    if (videos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No videos found")
        }
    } else {
        LazyColumn {
            items(videos, key = { it.id }) { video ->
                ListItem(
                    headlineContent = { Text(video.title, maxLines = 1) },
                    leadingContent = {
                        AsyncImage(
                            model = video.uri,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                    },
                    modifier = Modifier.clickable { onVideoClick(video) }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class) // <--- Add this line
@Composable
fun VideoPlayerScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val player by viewModel.player.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (player != null) {
            AndroidView(
                factory = { context ->
                    androidx.media3.ui.PlayerView(context).apply {
                        this.player = player
                        this.useController = true
                        this.setShowNextButton(false)     // triggers unstable warning
                        this.setShowPreviousButton(false) // triggers unstable warning
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Color.White)
        }
    }
}