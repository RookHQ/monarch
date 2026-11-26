package com.rookhq.monarch.components.interaction.appearancepanel

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchContrast
import com.rookhq.monarch.tokens.MonarchTokens

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
