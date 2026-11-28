package dev.monarchui.components.layout.rookbar

import androidx.compose.runtime.Composable
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuItem
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchRookBarPreview() {
    PreviewFrame {
        MonarchRookBar(
            brand = "Northwind",
            items = listOf(MonarchMegaMenuItem("pricing", "Pricing", link = "pricing")),
            menuOpen = false,
            onMenuOpenChange = {},
            onNavigate = {},
            account = MonarchRookBarAccount.SignedOut,
        )
        MonarchRookBar(
            brand = "Northwind",
            items = emptyList(),
            menuOpen = false,
            onMenuOpenChange = {},
            onNavigate = {},
            account = MonarchRookBarAccount.SignedIn("Ada Lovelace"),
        )
    }
}
