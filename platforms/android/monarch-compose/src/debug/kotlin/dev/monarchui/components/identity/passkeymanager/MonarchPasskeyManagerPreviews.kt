package dev.monarchui.components.identity.passkeymanager

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

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
