package com.rookhq.monarch.components.technical.apikeyrow

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val PreviewNow = 1_763_100_000_000L
private const val Day = 86_400_000L

@MonarchPreviews
@Composable
internal fun MonarchApiKeyRowPreview() {
    PreviewFrame {
        MonarchApiKeyRow(
            name = "Reporting job",
            prefix = "sk_live_4f2a",
            createdAt = PreviewNow - 30 * Day,
            lastUsedAt = PreviewNow - Day,
            scopes = listOf("items:read", "items:write", "reports:read", "users:read"),
            now = PreviewNow,
            onRotate = {},
            onRevoke = {},
        )
        MonarchApiKeyRow(
            name = "Old CI key",
            prefix = "sk_live_9a01",
            createdAt = PreviewNow - 400 * Day,
            expiresAt = PreviewNow - 5 * Day,
            status = MonarchApiKeyStatus.Expired,
            now = PreviewNow,
        )
    }
}
