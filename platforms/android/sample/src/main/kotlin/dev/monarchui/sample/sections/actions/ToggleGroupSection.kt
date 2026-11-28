package dev.monarchui.sample.sections.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.datadisplay.tag.MonarchChip
import dev.monarchui.components.forms.segmentedcontrol.MonarchSegmentedControl
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ToggleGroup: Monarch chips for any number pressed, a Monarch segmented control for one. */
object ToggleGroupSection : CataloguePage {
    override val title = "ToggleGroup"

    private val styles = listOf("Bold", "Italic", "Underline")
    private val alignments = listOf("Start", "Centre", "End")

    @Composable
    override fun Content() {
        var pressed by rememberSaveable { mutableStateOf(listOf("Bold")) }
        var alignment by rememberSaveable { mutableStateOf<String?>("Start") }
        CatalogueCaption("Any number pressed")
        Row(
            modifier = Modifier.semantics { contentDescription = "Text style" },
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            styles.forEach { style ->
                MonarchChip(
                    text = style,
                    selected = style in pressed,
                    onSelectedChange = { on -> pressed = if (on) pressed + style else pressed - style },
                )
            }
        }
        CatalogueCaption("At most one pressed; press it again to release it")
        MonarchSegmentedControl(modifier = Modifier.semantics { contentDescription = "Alignment" }) {
            alignments.forEach { option ->
                val selected = option == alignment
                Segment(selected = selected, onClick = { alignment = if (selected) null else option }, label = option)
            }
        }
        MonarchText(
            text = "Pressed: ${pressed.joinToString().ifEmpty { "none" }}; alignment: ${alignment ?: "none"}",
            color = MonarchTheme.colors.textMuted,
        )
    }
}
