package dev.monarchui.sample.sections.actions

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.buttongroup.MonarchButtonGroup
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of ButtonGroup. */
object MonarchButtonGroupSection : CataloguePage {
    override val title = "ButtonGroup"

    private const val ZOOM_STEP = 10
    private const val START_ZOOM = 100

    @Composable
    override fun Content() {
        var page by rememberSaveable { mutableIntStateOf(1) }
        var zoom by rememberSaveable { mutableIntStateOf(START_ZOOM) }
        CatalogueCaption("Attached, secondary")
        MonarchButtonGroup(label = "Pages") {
            button(text = "Previous", onClick = { page-- }, enabled = page > 1)
            button(text = "Next", onClick = { page++ })
        }
        MonarchText(text = "Page $page", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Every variant, small")
        MonarchButtonVariant.entries.forEach { variant ->
            MonarchButtonGroup(variant = variant, size = MonarchButtonSize.Sm) {
                button(text = "Day", onClick = {})
                button(text = "Week", onClick = {})
                button(text = "Month", onClick = {})
            }
        }
        CatalogueCaption("Vertical icon buttons")
        MonarchButtonGroup(orientation = Orientation.Vertical, label = "Zoom") {
            iconButton(contentDescription = "Zoom in", onClick = { zoom += ZOOM_STEP }) {
                MonarchIcon(imageVector = SampleIcons.Plus, contentDescription = null)
            }
            iconButton(contentDescription = "Back to 100%", onClick = { zoom = START_ZOOM }) {
                MonarchIcon(imageVector = SampleIcons.Undo, contentDescription = null)
            }
        }
        MonarchText(text = "Zoom $zoom%", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Detached, full width")
        MonarchButtonGroup(attached = false, fullWidth = true, variant = MonarchButtonVariant.Primary) {
            button(text = "Approve", onClick = {})
            button(text = "Approve and notify", onClick = {})
        }
    }
}
