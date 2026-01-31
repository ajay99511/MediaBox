package com.local.offlinemediaplayer.model

import android.net.Uri

data class VideoFolder(
    val id: String,
    val name: String,
    val videoCount: Int,
    val thumbnailUri: Uri
)
