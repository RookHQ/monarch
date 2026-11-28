package dev.monarchui.components.geo.languageselect

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import java.util.Locale

@MonarchPreviews
@Composable
internal fun MonarchLanguageSelectPreview() {
    PreviewFrame {
        MonarchLanguageSelect(selected = "de", onSelectedChange = {}, locale = Locale.US)
        MonarchLanguageSelect(
            selected = "ja",
            onSelectedChange = {},
            locale = Locale.US,
            labelStyle = MonarchLanguageSelectLabelStyle.Localized,
        )
        MonarchLanguageSelect(
            selected = null,
            onSelectedChange = {},
            locale = Locale.US,
            errorText = "Choose a language.",
        )
        MonarchLanguageSelect(selected = "fr", onSelectedChange = {}, locale = Locale.US, enabled = false)
    }
}
