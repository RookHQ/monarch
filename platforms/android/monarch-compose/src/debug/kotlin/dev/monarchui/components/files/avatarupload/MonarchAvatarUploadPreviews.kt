package dev.monarchui.components.files.avatarupload

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.app.ActivityOptionsCompat
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

private const val PreviewImageWidth = 300
private const val PreviewImageHeight = 200
private const val PreviewZoom = 1.5f

/** The preview has no activity, so the picker is registered with a registry that never opens anything. */
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
internal fun MonarchAvatarUploadPreview() {
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides previewRegistryOwner) {
        PreviewFrame {
            MonarchAvatarUpload(name = "Ada Lovelace", image = null, onChange = {})
            MonarchAvatarUpload(name = "Rook Labs", image = null, onChange = {}, shape = MonarchAvatarShape.Square)
        }
    }
}

@MonarchPreviews
@Composable
internal fun MonarchAvatarUploadEditorPreview() {
    PreviewFrame {
        AvatarUploadEditor(
            image = remember { ImageBitmap(PreviewImageWidth, PreviewImageHeight) },
            view = AvatarUploadView(zoom = PreviewZoom),
            onViewChange = {},
            shape = MonarchAvatarShape.Circle,
            saving = false,
            enabled = true,
            onSave = {},
            onCancel = {},
            strings = AvatarUploadEditorStrings(
                cropArea = "Photo position",
                cropHint = "Drag to move the photo. Pinch or use the slider to zoom.",
                zoom = "Zoom",
                zoomValue = { "$it%" },
                moveLeft = "Move left",
                moveRight = "Move right",
                moveUp = "Move up",
                moveDown = "Move down",
                zoomIn = "Zoom in",
                zoomOut = "Zoom out",
                save = "Save photo",
                cancel = "Cancel",
            ),
            focusRequester = remember { FocusRequester() },
        )
    }
}
