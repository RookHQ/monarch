package dev.monarchui.sample.sections.interaction

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.interaction.appearancepanel.MonarchAppearancePanel
import dev.monarchui.components.interaction.appearancepanel.MonarchAppearancePanelRow
import dev.monarchui.components.interaction.appearancepanel.MonarchAppearancePreferences
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

private val Rook = MonarchTokens.Accents.rook

/** The catalogue page of AppearancePanel. */
object MonarchAppearancePanelSection : CataloguePage {
    override val title = "AppearancePanel"

    @Composable
    override fun Content() {
        var preferences by remember { mutableStateOf(MonarchAppearancePreferences(Rook)) }
        CatalogueCaption("The preview below applies scheme, contrast and accent with MonarchTheme")
        MonarchAppearancePanel(
            preferences = preferences,
            onPreferencesChange = { preferences = it },
            title = "Appearance",
            rows = MonarchAppearancePanelRow.Standard + MonarchAppearancePanelRow.Accent,
            accents = MonarchTokens.Accents.all,
        )
        MonarchTheme(scheme = preferences.scheme, contrast = preferences.contrast, accent = preferences.accent) {
            MonarchButton(text = "Preview of the choice", onClick = {})
        }
        MonarchText(
            text = "Reduce motion in the app: ${preferences.reducesMotion(systemReducesMotion = false)}. " +
                "Density: ${preferences.density}. Device dark theme: ${isSystemInDarkTheme()}.",
        )
    }
}
