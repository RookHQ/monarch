package dev.monarchui.components.identity.devicelist

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
