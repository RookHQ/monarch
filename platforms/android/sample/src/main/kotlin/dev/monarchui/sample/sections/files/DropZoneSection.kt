package dev.monarchui.sample.sections.files

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.files.fileupload.MonarchFileUpload
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of DropZone: Compose drag and drop on a region, with the file picker as the main path. */
object DropZoneSection : CataloguePage {
    override val title = "DropZone"

    @Composable
    override fun Content() {
        var targeted by remember { mutableStateOf(false) }
        var dropped by remember { mutableIntStateOf(0) }
        var picked by remember { mutableIntStateOf(0) }
        val target = remember {
            object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) {
                    targeted = true
                }

                override fun onExited(event: DragAndDropEvent) {
                    targeted = false
                }

                override fun onEnded(event: DragAndDropEvent) {
                    targeted = false
                }

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val clip: ClipData = event.toAndroidDragEvent().clipData ?: return false
                    dropped += (0 until clip.itemCount).count { clip.getItemAt(it).uri != null }
                    return true
                }
            }
        }
        val colors = MonarchTheme.colors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event -> event.mimeTypes().any { it.startsWith("image/") } },
                    target = target,
                )
                .background(if (targeted) colors.accentSubtle else colors.bgCanvas, MonarchTheme.shapes.lg)
                .border(
                    MonarchTheme.spacing.px,
                    if (targeted) colors.accentBorder else colors.borderSubtle,
                    MonarchTheme.shapes.lg,
                )
                .padding(MonarchTheme.spacing.space4),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            MonarchFileUpload(onFiles = { accepted, _ -> picked += accepted.size }, accept = listOf("image/*"))
            Text(
                text = if (targeted) "Release to add the images" else "Or drag images here from another app",
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.caption,
                color = if (targeted) colors.accentStrong else colors.textMuted,
            )
        }
        CatalogueCaption("Received")
        Text("$picked picked, $dropped dropped", style = MonarchTheme.typography.body)
    }
}
