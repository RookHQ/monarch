@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence.aiartifactversions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.sampleicons.SampleIcons
import java.util.Locale
import java.util.TimeZone

private const val Now = 1_793_750_400_000L
private const val Hour = 3_600_000L
private val Versions = listOf(
    MonarchAiArtifactVersion(1, Now - 2 * Hour, content = "Ship on Friday.\nTell support."),
    MonarchAiArtifactVersion(2, Now - Hour, label = "Shorter intro", content = "Ship on Monday.\nTell support."),
)

@MonarchPreviews
@Composable
internal fun MonarchAiArtifactVersionsPreview() {
    PreviewFrame {
        MonarchAiArtifactVersions(
            selection = 2,
            versions = Versions,
            onSelectionChange = {},
            previousIcon = { PreviewChevron(SampleIcons.ChevronLeft) },
            nextIcon = { PreviewChevron(SampleIcons.ChevronRight) },
            comparing = true,
            locale = Locale.US,
            timeZone = "UTC",
        )
        MonarchAiArtifactVersions(
            selection = 1,
            versions = Versions.take(1),
            onSelectionChange = {},
            previousIcon = { PreviewChevron(SampleIcons.ChevronLeft) },
            nextIcon = { PreviewChevron(SampleIcons.ChevronRight) },
            enabled = false,
        )
    }
}

@Composable
private fun PreviewChevron(icon: ImageVector) {
    MonarchIcon(icon, contentDescription = null)
}
