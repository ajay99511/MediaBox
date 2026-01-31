//package com.local.offlinemediaplayer.model
//import android.net.Uri
//
//data class MediaFile(
//    val id: Long,
//    val uri: Uri,
//    val title: String,
//    val artist: String? = null,
//    val duration: Long,
//    val isVideo: Boolean,
//    val albumArtUri: Uri? = null,
//    val albumId: Long = -1
//)

package com.local.offlinemediaplayer.model
import android.net.Uri

data class MediaFile(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String? = null,
    val duration: Long,
    val isVideo: Boolean,
    val isImage: Boolean = false,
    val albumArtUri: Uri? = null,
    val albumId: Long = -1
)
