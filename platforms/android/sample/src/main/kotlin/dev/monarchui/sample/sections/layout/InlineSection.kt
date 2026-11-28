package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Inline: FlowRow with spacing from the space scale. */
object InlineSection : CataloguePage {
    override val title = "Inline"

    private val labels = listOf("Billing", "Urgent", "Enterprise", "Follow up", "Renewal", "Security review")

    @Composable
    override fun Content() {
        val gap = MonarchTheme.spacing.space2
        CatalogueCaption("Tags that wrap")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalArrangement = Arrangement.spacedBy(gap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            labels.forEach { MonarchTag(text = it) }
        }

        CatalogueCaption("Actions aligned to the end")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Ghost)
            MonarchButton(text = "Save draft", onClick = {})
            MonarchButton(text = "Publish", onClick = {}, variant = MonarchButtonVariant.Primary)
        }
    }
}
