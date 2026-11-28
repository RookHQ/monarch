package dev.monarchui.sample.sections.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import dev.monarchui.components.files.avatarupload.MonarchAvatarUpload
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import kotlinx.coroutines.delay

/** The catalogue page of AvatarUpload: the cropped picture replaces the avatar after a short pretend upload. */
object MonarchAvatarUploadSection : CataloguePage {
    override val title = "AvatarUpload"

    private const val UPLOAD_MILLIS = 700L

    @Composable
    override fun Content() {
        var person by remember { mutableStateOf<ImageBitmap?>(null) }
        var workspace by remember { mutableStateOf<ImageBitmap?>(null) }
        MonarchAvatarUpload(
            name = "Ada Lovelace",
            image = person?.let { BitmapPainter(it) },
            onChange = {
                delay(UPLOAD_MILLIS)
                person = it.image
            },
            onRemove = { person = null },
        )
        CatalogueCaption("Square, for a workspace")
        MonarchAvatarUpload(
            name = "Rook Labs",
            image = workspace?.let { BitmapPainter(it) },
            onChange = { workspace = it.image },
            shape = MonarchAvatarShape.Square,
            onRemove = { workspace = null },
        )
        CatalogueCaption("Disabled")
        MonarchAvatarUpload(name = "Grace Hopper", image = null, onChange = {}, enabled = false)
    }
}
