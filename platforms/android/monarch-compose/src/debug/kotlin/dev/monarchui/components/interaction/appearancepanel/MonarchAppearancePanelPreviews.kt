package dev.monarchui.components.interaction.appearancepanel

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.theme.MonarchContrast
import dev.monarchui.tokens.MonarchTokens

private val Rook = MonarchTokens.Accents.rook

@MonarchPreviews
@Composable
internal fun MonarchAppearancePanelPreview() {
    PreviewFrame {
        MonarchAppearancePanel(
            preferences = MonarchAppearancePreferences(Rook, contrast = MonarchContrast.More),
            onPreferencesChange = {},
            title = "Appearance",
            rows = MonarchAppearancePanelRow.Standard + MonarchAppearancePanelRow.Accent,
        )
    }
}
