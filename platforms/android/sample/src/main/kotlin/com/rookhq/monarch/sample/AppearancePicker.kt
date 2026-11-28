package com.rookhq.monarch.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.forms.radiorow.MonarchRadioRow
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchAccent
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The theme, accent and typeface of the catalogue, built from Monarch: radio rows for the theme in a card,
 * and selects for the accent and the typeface.
 */
@Composable
fun AppearancePicker(
    theme: CatalogueTheme,
    onThemeChange: (CatalogueTheme) -> Unit,
    accent: MonarchAccent,
    onAccentChange: (MonarchAccent) -> Unit,
    typeface: CatalogueTypeface = CatalogueTypeface.System,
    onTypefaceChange: (CatalogueTypeface) -> Unit = {},
) {
    CatalogueGroup("Theme") {
        MonarchCard(contentPadding = PaddingValues(MonarchTheme.spacing.space0)) {
            Column(
                Modifier
                    .selectableGroup()
                    .padding(horizontal = MonarchTheme.spacing.space4, vertical = MonarchTheme.spacing.space1),
            ) {
                CatalogueTheme.entries.forEach { option ->
                    MonarchRadioRow(
                        selected = option == theme,
                        onClick = { onThemeChange(option) },
                        label = option.label,
                    )
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
        MonarchSelect(
            options = MonarchTokens.Accents.all.map { MonarchSelectOption(it.id, it.label) },
            selected = accent.id,
            onSelect = { id -> onAccentChange(MonarchTokens.Accents.all.first { it.id == id }) },
            label = "Accent",
        )
        MonarchSelect(
            options = CatalogueTypeface.entries.map { MonarchSelectOption(it, it.label) },
            selected = typeface,
            onSelect = onTypefaceChange,
            label = "Typeface",
            helperText = "Monarch uses the system font unless the app passes another one.",
        )
    }
}
