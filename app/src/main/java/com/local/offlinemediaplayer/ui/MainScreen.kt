package com.local.offlinemediaplayer.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.ui.navigation.AudioNavigationHost
import com.local.offlinemediaplayer.ui.screens.ImageListScreen
import com.local.offlinemediaplayer.ui.screens.PermissionRationaleScreen
import com.local.offlinemediaplayer.ui.screens.PermissionRequestScreen
import com.local.offlinemediaplayer.ui.screens.VideoListScreen
import com.local.offlinemediaplayer.ui.screens.VideoPlayerScreen
import com.local.offlinemediaplayer.ui.theme.Headers.AppHeader
import com.local.offlinemediaplayer.viewmodel.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val context = LocalContext.current

    // Define permissions based on Android version
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES
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
fun MediaPlayerAppContent(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentMedia by remember { mutableStateOf<MediaFile?>(null) }

    // Hoist Navigation State
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Define which routes should hide the AppHeader and BottomBar
    val isAudioDetailScreen = currentRoute != "audio_library"
    val isVideoPlaying = currentMedia?.isVideo == true

    // Only show bars if we are:
    // 1. NOT playing a video
    // 2. AND (We are in Video Tab OR Images Tab OR (We are in Audio Tab and at the root library screen))
    val showBars = !isVideoPlaying && (selectedTab != 1 || !isAudioDetailScreen)

    Scaffold(
        // Custom Top Bar (Conditionally Visible)
        topBar = {
            if (showBars) {
                AppHeader()
            }
        },
        bottomBar = {
            if (showBars) {
                // FastBeat Custom Navigation Bar
                val navContainerColor = Color(0xFF0B0B0F) // Deep Dark
                val activeIndicatorColor = Color(0xFFFF1F48).copy(alpha = 0.15f) // Neon Red Glow
                val activeIconColor = Color(0xFFFF1F48)
                val inactiveIconColor = Color.Gray

                NavigationBar(
                    containerColor = navContainerColor,
                    contentColor = Color.White
                ) {
                    // 1. Videos
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow,
                                contentDescription = "Videos"
                            )
                        },
                        label = { Text("Videos", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeIconColor,
                            selectedTextColor = activeIconColor,
                            indicatorColor = activeIndicatorColor,
                            unselectedIconColor = inactiveIconColor,
                            unselectedTextColor = inactiveIconColor
                        )
                    )

                    // 2. Music
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.MusicNote else Icons.Outlined.MusicNote,
                                contentDescription = "Music"
                            )
                        },
                        label = { Text("Music", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeIconColor,
                            selectedTextColor = activeIconColor,
                            indicatorColor = activeIndicatorColor,
                            unselectedIconColor = inactiveIconColor,
                            unselectedTextColor = inactiveIconColor
                        )
                    )

                    // 3. Images
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.Image else Icons.Outlined.Image,
                                contentDescription = "Images"
                            )
                        },
                        label = { Text("Images", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = activeIconColor,
                            selectedTextColor = activeIconColor,
                            indicatorColor = activeIndicatorColor,
                            unselectedIconColor = inactiveIconColor,
                            unselectedTextColor = inactiveIconColor
                        )
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
                    0 -> {
                        VideoListScreen(viewModel) { file ->
                            currentMedia = file
                            viewModel.playMedia(file)
                        }
                    }
                    1 -> {
                        // Pass the hoisted controller to the host
                        AudioNavigationHost(viewModel, navController)
                    }
                    2 -> {
                        ImageListScreen(viewModel)
                    }
                }
            }
        }
    }
}
