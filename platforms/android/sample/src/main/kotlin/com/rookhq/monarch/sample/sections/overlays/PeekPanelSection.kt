package com.rookhq.monarch.sample.sections.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The catalogue page of PeekPanel. At expanded width the panel is the
 * supporting pane of SupportingPaneScaffold; the catalogue does not depend
 * on the adaptive libraries, so it shows the phone form, a bottom sheet.
 */
object PeekPanelSection : CataloguePage {
    override val title = "PeekPanel"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var peeking by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("On a phone: a sheet")
        MonarchButton(text = "Preview INV-1042", onClick = { peeking = true })
        MonarchText(
            text = "On a wide window the same content is the supporting pane of SupportingPaneScaffold, " +
                "beside the list.",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        if (peeking) {
            MonarchBottomSheet(
                onDismissRequest = { peeking = false },
                title = "INV-1042",
                description = "Due in 5 days",
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                ) {
                    MonarchButton(
                        text = "Open the invoice",
                        onClick = { this@MonarchBottomSheet.dismiss() },
                        variant = MonarchButtonVariant.Primary,
                    )
                }
            }
        }
    }
}
