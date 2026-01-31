package com.local.offlinemediaplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.local.offlinemediaplayer.model.MediaFile
import com.local.offlinemediaplayer.ui.screens.VideoFolderScreen
import com.local.offlinemediaplayer.ui.screens.VideoListScreen
import com.local.offlinemediaplayer.viewmodel.MainViewModel

@Composable
fun VideoNavigationHost(
    viewModel: MainViewModel,
    navController: NavHostController,
    onVideoClick: (MediaFile) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "video_folders"
    ) {
        composable("video_folders") {
            VideoFolderScreen(
                viewModel = viewModel,
                onFolderClick = { folderId ->
                    navController.navigate("video_list/$folderId")
                }
            )
        }

        composable("video_list/{bucketId}") { backStackEntry ->
            val bucketId = backStackEntry.arguments?.getString("bucketId") ?: ""
            // Filter videos by bucketId inside VideoListScreen or pass filtered list
            // We'll expose a filtered list way or just filter here

            // Note: VideoListScreen currently observes `videoList`.
            // We should ideally update VideoListScreen to accept a list parameter,
            // but for minimal changes, let's filter in the ViewModel or pass a specific list here.

            // Let's assume we modify VideoListScreen to take a list, OR we filter here:
            val allVideos by viewModel.videoList.collectAsState()
            val folderVideos = allVideos.filter { it.bucketId == bucketId }

            // We'll modify VideoListScreen slightly to allow passing a list,
            // OR use a modified version.
            // Since VideoListScreen uses `viewModel.videoList` internally in the provided code,
            // we should refactor it or create a variation.
            // For now, I will wrap it to override the list if the component supports it,
            // but looking at previous file, it takes `viewModel` and observes `viewModel.videoList`.
            // We will need to update VideoListScreen to optionally take a list parameter.

            // Assuming VideoListScreen is updated to accept `videos` param (see update below).
            VideoListScreen(
                viewModel = viewModel,
                onVideoClick = onVideoClick,
                videoListOverride = folderVideos,
                title = folderVideos.firstOrNull()?.bucketName ?: "Videos",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
