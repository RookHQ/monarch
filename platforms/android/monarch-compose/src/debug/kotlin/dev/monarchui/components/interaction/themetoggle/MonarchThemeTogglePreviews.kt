package dev.monarchui.components.interaction.themetoggle

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon
import dev.monarchui.theme.MonarchScheme

@MonarchPreviews
@Composable
internal fun MonarchThemeTogglePreview() {
    PreviewFrame {
        MonarchThemeToggle(scheme = MonarchScheme.System, onSchemeChange = {})
        MonarchThemeToggle(scheme = MonarchScheme.Dark, onSchemeChange = {}, size = MonarchThemeToggleSize.Sm)
        MonarchThemeToggle(scheme = MonarchScheme.Light, onSchemeChange = {}, enabled = false)
        MonarchThemeToggle(
            scheme = MonarchScheme.Dark,
            onSchemeChange = {},
            variant = MonarchThemeToggleVariant.Menu,
            icon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchThemeToggle(
            scheme = MonarchScheme.Light,
            onSchemeChange = {},
            variant = MonarchThemeToggleVariant.Menu,
            size = MonarchThemeToggleSize.Sm,
        )
    }
}
