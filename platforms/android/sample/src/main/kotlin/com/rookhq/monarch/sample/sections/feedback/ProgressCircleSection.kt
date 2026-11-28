package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgressVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlin.math.roundToInt

/** The catalogue page of ProgressCircle: MonarchProgress in its circular form. */
object ProgressCircleSection : CataloguePage {
    override val title = "ProgressCircle"

    private const val STEP = 0.1f
    private const val START = 0.4f
    private const val PERCENT = 100

    @Composable
    override fun Content() {
        var value by rememberSaveable { mutableFloatStateOf(START) }
        CatalogueCaption("Determinate")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchProgress(progress = value, variant = MonarchProgressVariant.Circular, label = "Uploading report")
            MonarchText(text = "${(value * PERCENT).roundToInt()}%")
            MonarchButton(
                text = "Advance",
                onClick = { value = if (value >= 1f) 0f else (value + STEP).coerceAtMost(1f) },
                size = MonarchButtonSize.Sm,
            )
        }
        CatalogueCaption("Indeterminate")
        MonarchProgress(variant = MonarchProgressVariant.Circular, label = "Loading activity")
    }
}
