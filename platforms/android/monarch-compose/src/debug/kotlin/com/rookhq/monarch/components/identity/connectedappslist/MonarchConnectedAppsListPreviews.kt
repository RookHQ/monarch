package com.rookhq.monarch.components.identity.connectedappslist

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewHour = 3_600_000L

@MonarchPreviews
@Composable
internal fun MonarchConnectedAppsListPreview() {
    PreviewFrame {
        MonarchConnectedAppsList(
            apps = listOf(
                MonarchConnectedApp(
                    id = "notes",
                    name = "Notes",
                    domain = "notes.example.com",
                    verified = true,
                    workspace = "Acme",
                    scopes = listOf("See your email address", "See your name"),
                    connectedAtMillis = PreviewNow - 72 * PreviewHour,
                    lastUsedAtMillis = PreviewNow - 2 * PreviewHour,
                ),
                MonarchConnectedApp(id = "sheets", name = "Sheets", suspended = true),
            ),
            now = PreviewNow,
            onRevoke = {},
        )
        MonarchConnectedAppsList(apps = emptyList())
        MonarchConnectedAppsList(apps = emptyList(), state = MonarchConnectedAppsListState.Error, onRetry = {})
    }
}
