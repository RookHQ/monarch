package com.rookhq.monarch.sample.sections.interaction

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearancePanel
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearancePanelRow
import com.rookhq.monarch.components.interaction.appearancepanel.MonarchAppearancePreferences
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

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
