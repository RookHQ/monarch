package dev.monarchui.components.forms.switchsettingslist

import androidx.compose.runtime.Composable
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSwitchSettingsListPreview() {
    PreviewFrame {
        MonarchSwitchSettingsList(
            title = "Email notifications",
            description = "Each setting saves as soon as you change it.",
        ) {
            MonarchSwitchSettingsRow(
                checked = true,
                onCheckedChange = {},
                onSave = {},
                label = "Weekly summary",
                description = "Sent on Mondays.",
            )
            MonarchSwitchSettingsRow(checked = false, onCheckedChange = {}, onSave = {}, label = "Product news")
            MonarchSwitchSettingsRow(
                checked = false,
                onCheckedChange = {},
                onSave = {},
                label = "Security alerts",
                enabled = false,
            )
        }
    }
}
