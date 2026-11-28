package com.rookhq.monarch.components.interaction.themetoggle

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon
import com.rookhq.monarch.theme.MonarchScheme

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
