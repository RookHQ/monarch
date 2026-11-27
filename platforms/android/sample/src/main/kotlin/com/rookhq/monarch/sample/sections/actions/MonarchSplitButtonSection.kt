package com.rookhq.monarch.sample.sections.actions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.splitbutton.MonarchSplitButton
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

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
