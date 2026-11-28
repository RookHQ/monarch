package dev.monarchui.sample.sections.identity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of PrivacyChoicesLink: a recipe of a settings link, a sheet and MonarchSwitchRow. */
object PrivacyChoicesLinkSection : CataloguePage {
    override val title = "PrivacyChoicesLink"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var allowed by rememberSaveable { mutableStateOf(true) }
        var draft by rememberSaveable { mutableStateOf(true) }
        var open by rememberSaveable { mutableStateOf(false) }
        var locked by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Shown only where the opt-out applies")
        MonarchLink(
            text = "Your privacy choices",
            onClick = {
                draft = allowed && !locked
                open = true
            },
            variant = MonarchLinkVariant.Standalone,
        )
        MonarchText(text = if (allowed && !locked) "Sale or sharing: allowed" else "Sale or sharing: stopped")
        MonarchSwitchRow(
            checked = locked,
            onCheckedChange = { locked = it },
            label = "Simulate a Global Privacy Control signal",
        )
        if (open) {
            MonarchBottomSheet(
                onDismissRequest = { open = false },
                title = "Your privacy choices",
                description = "We share some personal information with advertising partners. " +
                    "Under some state laws this counts as a sale or sharing, and you can stop it.",
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4),
                ) {
                    MonarchSwitchRow(
                        checked = draft && !locked,
                        onCheckedChange = { draft = it },
                        label = "Allow sale or sharing",
                        enabled = !locked,
                        description = if (locked) {
                            "Your Global Privacy Control signal turned this off."
                        } else {
                            "Turn this off to stop the sale or sharing of your personal information."
                        },
                    )
                    MonarchLink(text = "Privacy policy", onClick = {}, external = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2, Alignment.End),
                    ) {
                        MonarchButton(
                            text = "Cancel",
                            onClick = { this@MonarchBottomSheet.dismiss() },
                            variant = MonarchButtonVariant.Ghost,
                        )
                        MonarchButton(
                            text = "Save",
                            onClick = {
                                allowed = draft && !locked
                                this@MonarchBottomSheet.dismiss()
                            },
                            variant = MonarchButtonVariant.Primary,
                        )
                    }
                }
            }
        }
    }
}
