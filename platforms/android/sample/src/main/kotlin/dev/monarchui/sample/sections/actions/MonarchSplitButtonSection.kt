package dev.monarchui.sample.sections.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.splitbutton.MonarchSplitButton
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.overlays.menu.MonarchMenuDivider
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of SplitButton: a main action and a menu of alternatives. */
object MonarchSplitButtonSection : CataloguePage {
    override val title = "SplitButton"

    @Composable
    override fun Content() {
        var last by rememberSaveable { mutableStateOf("Nothing yet") }
        var menuEnabled by rememberSaveable { mutableStateOf(true) }
        val arrow: @Composable () -> Unit = {
            MonarchIcon(SampleIcons.ChevronDown, null, size = MonarchIconSize.Sm)
        }
        CatalogueCaption("Primary, with a danger item")
        MonarchSplitButton(
            text = "Save",
            onClick = { last = "Save" },
            arrowIcon = arrow,
            variant = MonarchButtonVariant.Primary,
            menuEnabled = menuEnabled,
        ) { close ->
            listOf("Save as draft", "Save and close").forEach { action ->
                MonarchMenuItem(
                    text = action,
                    onClick = {
                        last = action
                        close()
                    },
                )
            }
            MonarchMenuDivider()
            MonarchMenuItem(
                text = "Discard changes",
                onClick = {
                    last = "Discard changes"
                    close()
                },
                destructive = true,
            )
        }
        MonarchSwitchRow(checked = menuEnabled, onCheckedChange = { menuEnabled = it }, label = "Menu enabled")
        MonarchText(text = "Last action: $last", color = MonarchTheme.colors.textMuted)
        CatalogueCaption("Secondary, loading and disabled")
        MonarchSplitButton(text = "Export", onClick = {}, arrowIcon = arrow, loading = true) {}
        MonarchSplitButton(text = "Export", onClick = {}, arrowIcon = arrow, enabled = false) {}
    }
}
