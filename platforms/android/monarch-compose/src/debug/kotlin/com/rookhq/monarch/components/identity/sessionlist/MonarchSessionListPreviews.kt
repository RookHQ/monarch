@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.identity.sessionlist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceKind
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewHour = 3_600_000L

@MonarchPreviews
@Composable
internal fun MonarchSessionListPreview() {
    PreviewFrame {
        MonarchSessionList(
            sessions = listOf(
                MonarchSession(
                    id = "this",
                    label = "Rook app on Android",
                    kind = MonarchDeviceKind.Mobile,
                    detail = "Lisbon, Portugal",
                    signedInMillis = PreviewNow - 300 * PreviewHour,
                    current = true,
                ),
                MonarchSession(
                    id = "mac",
                    label = "Chrome on macOS",
                    detail = "Porto, Portugal",
                    signedInMillis = PreviewNow - 900 * PreviewHour,
                    lastActiveMillis = PreviewNow - 2 * PreviewHour,
                ),
            ),
            now = PreviewNow,
            footnote = "Open apps stay signed in for 30 days.",
            onSignOut = {},
            onSignOutOthers = {},
            sessionIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchSessionList(sessions = emptyList(), state = MonarchSessionListState.Failed, onRetry = {})
    }
}
