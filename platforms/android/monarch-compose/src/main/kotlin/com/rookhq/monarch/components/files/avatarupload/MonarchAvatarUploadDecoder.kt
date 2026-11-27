package com.rookhq.monarch.components.files.avatarupload

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.IOException

/** Reads a picked image into memory, scaled down so its longest side is at most a limit. */
internal object MonarchAvatarUploadDecoder {
    /** The power of two to divide by so the longest side fits [maxSide]. */
    fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > maxSide) sample *= 2
        return sample
    }

    /**
     * Decodes [uri], or returns `null` when it cannot be read as an image.
     * From API 28 `ImageDecoder` applies the EXIF rotation; below, `BitmapFactory` does not.
     */
    fun decode(resolver: ContentResolver, uri: Uri, maxSide: Int): ImageBitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.setTargetSampleSize(sampleSize(info.size.width, info.size.height, maxSide))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide)
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}
