package com.rookhq.monarch.components.files.fileupload

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityOptionsCompat
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewMaxSize = 10L * 1024 * 1024

/** The preview has no activity, so the pickers are registered with a registry that never opens anything. */
private val previewRegistryOwner = object : ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) = Unit
    }
}

@MonarchPreviews
@Composable
internal fun MonarchFileUploadPreview() {
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides previewRegistryOwner) {
        PreviewFrame {
            MonarchFileUpload(onFiles = { _, _ -> }, accept = listOf("application/pdf"), maxSize = PreviewMaxSize)
            MonarchFileUpload(onFiles = { _, _ -> }, variant = MonarchFileUploadVariant.Compact, maxFiles = 3)
            MonarchFileUpload(onFiles = { _, _ -> }, maxFiles = 2, currentCount = 2)
        }
    }
}
