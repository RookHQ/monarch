package com.rookhq.monarch.components.layout.rookbar

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuItem
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

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
