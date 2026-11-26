package com.rookhq.monarch.components.identity.passkeymanager

import androidx.compose.runtime.Composable
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame
import com.rookhq.monarch.internal.preview.PreviewIcon

private const val PreviewNow = 1_800_000_000_000L
private const val PreviewDay = 86_400_000L

@MonarchPreviews
@Composable
internal fun MonarchPasskeyManagerPreview() {
    PreviewFrame {
        MonarchPasskeyManager(
            passkeys = listOf(
                MonarchPasskey(
                    id = "google",
                    name = "Google Password Manager",
                    createdMillis = PreviewNow - 30 * PreviewDay,
                    lastUsedMillis = PreviewNow - PreviewDay,
                    synced = true,
                ),
                MonarchPasskey(id = "key", name = "Security key", createdMillis = PreviewNow - 2 * PreviewDay),
            ),
            now = PreviewNow,
            onAdd = { true },
            onRename = { _, _ -> },
            onRemove = {},
            passkeyIcon = { MonarchIcon(imageVector = PreviewIcon, contentDescription = null) },
        )
        MonarchPasskeyManager(passkeys = emptyList(), supported = false, onAdd = { true })
    }
}
