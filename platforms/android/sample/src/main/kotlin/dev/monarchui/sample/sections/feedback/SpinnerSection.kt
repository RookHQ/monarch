package dev.monarchui.sample.sections.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import dev.monarchui.components.feedback.spinner.MonarchSpinner
import dev.monarchui.components.feedback.spinner.MonarchSpinnerSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Spinner. */
object SpinnerSection : CataloguePage {
    override val title = "Spinner"

    @Composable
    override fun Content() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchSpinnerSize.entries.forEach { size ->
                MonarchSpinner(size = size, color = MonarchTheme.colors.accentStrong, label = "Loading activity")
            }
            MonarchText(text = "MonarchSpinner, 14, 16 and 24dp.", color = MonarchTheme.colors.textMuted)
        }
    }
}
