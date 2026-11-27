package com.rookhq.monarch.components.layout.deviceframe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.theme.MonarchTheme

@MonarchPreviews
@Composable
internal fun MonarchDeviceFramePreview() {
    PreviewFrame {
        MonarchDeviceFrame(
            label = "Preview of your link page on a phone",
            modifier = Modifier.width(160.dp),
            statusBarTime = "9:41",
        ) {
            Column(modifier = Modifier.padding(MonarchTheme.spacing.space3)) {
                MonarchText(text = "Ada Lovelace", style = MonarchTextStyle.BodyStrong)
                MonarchText(text = "Links", style = MonarchTextStyle.Caption)
            }
        }
        MonarchDeviceFrame(
            label = "Preview on a tablet",
            modifier = Modifier.width(200.dp),
            device = MonarchDeviceFrameDevice.Tablet,
            flat = true,
        ) {
            MonarchText(text = "Tablet layout", modifier = Modifier.padding(MonarchTheme.spacing.space3))
        }
    }
}
