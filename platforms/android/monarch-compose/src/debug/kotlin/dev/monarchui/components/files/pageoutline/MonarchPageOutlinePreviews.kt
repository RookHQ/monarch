package dev.monarchui.components.files.pageoutline

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchTheme

/** Lazy lists need a bounded height inside the scrolling preview column. */
private const val PreviewHeightSteps = 3

@MonarchPreviews
@Composable
internal fun MonarchPageOutlinePreview() {
    PreviewFrame {
        MonarchPageOutline(
            headings = listOf(
                MonarchPageOutlineHeading("Overview", 1, "overview"),
                MonarchPageOutlineHeading("Goals", 2, "goals"),
                MonarchPageOutlineHeading("Timeline", 2, "timeline"),
                MonarchPageOutlineHeading("Risks", 3, "risks"),
                MonarchPageOutlineHeading("Next steps", 1, "next"),
            ),
            onNavigate = {},
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * PreviewHeightSteps),
            currentId = "timeline",
            stats = MonarchPageOutlineStats(words = 1_234, minutes = 6),
        )
        MonarchPageOutline(
            headings = emptyList(),
            onNavigate = {},
            modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24),
        )
    }
}
