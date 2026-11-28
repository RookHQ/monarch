package com.rookhq.monarch.components.identity.devicelist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewHour = 3_600_000L

@MonarchPreviews
@Composable
internal fun MonarchDeviceListPreview() {
    PreviewFrame {
        MonarchDeviceList(
            devices = listOf(
                MonarchDevice(
                    id = "pixel",
                    name = "Ana's Pixel",
                    kind = MonarchDeviceKind.Mobile,
                    os = "Android 16",
                    browser = "Rook app",
                    location = "Lisbon, Portugal",
                    lastUsedMillis = PreviewNow,
                    current = true,
                ),
                MonarchDevice(
                    id = "mac",
                    name = "MacBook Pro",
                    os = "macOS",
                    browser = "Safari",
                    lastUsedMillis = PreviewNow - 26 * PreviewHour,
                    firstSeenMillis = PreviewNow - 900 * PreviewHour,
                    blocked = true,
                ),
            ),
            now = PreviewNow,
            onRemove = {},
            deviceIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchDeviceList(devices = emptyList(), state = MonarchDeviceListState.Failed, onRetry = {})
    }
}
