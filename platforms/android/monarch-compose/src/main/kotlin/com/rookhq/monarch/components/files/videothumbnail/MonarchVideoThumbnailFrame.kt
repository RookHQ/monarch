package com.rookhq.monarch.components.files.videothumbnail

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MicrosPerMilli = 1_000L

/**
 * Captures one frame of a video, for the `image` of a `MonarchVideoThumbnail`
 * when the app has no poster.
 *
 * Reads the video with the platform `MediaMetadataRetriever` on the IO
 * dispatcher, so the thumbnail itself never decodes video while it draws.
 * Call it from a `LaunchedEffect` or `produceState` and pass the result as a
 * `BitmapPainter`.
 *
 * @param context a context to open [uri] with.
 * @param uri a `file:` or `content:` URI of the video. Remote URLs are not read.
 * @param timeMillis the time of the frame. `null` takes one second in, or a tenth of the length for a
 * video shorter than ten seconds. Kept inside the video when its length is known; without a known
 * length `null` takes the first frame.
 * @return the frame, or `null` when the video has none at that time.
 * @throws IllegalArgumentException when [uri] cannot be opened.
 * @throws RuntimeException when the platform cannot read the video.
 */
public suspend fun captureVideoFrame(context: Context, uri: Uri, timeMillis: Long? = null): ImageBitmap? =
    withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            val at = if (duration != null) {
                MonarchVideoThumbnailTokens.captureTimeMillis(timeMillis, duration)
            } else {
                (timeMillis ?: 0L).coerceAtLeast(0L)
            }
            retriever.getFrameAtTime(at * MicrosPerMilli, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?.asImageBitmap()
        } finally {
            retriever.release()
        }
    }
