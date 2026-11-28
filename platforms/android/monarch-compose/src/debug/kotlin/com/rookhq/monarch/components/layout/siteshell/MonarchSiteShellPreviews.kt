package com.rookhq.monarch.components.layout.siteshell

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

@MonarchPreviews
@Composable
internal fun MonarchSiteShellPreview() {
    PreviewFrame {
        MonarchSiteShell(
            brand = "Rook Example",
            nav = listOf(MonarchSiteNavItem("Pricing", link = "pricing")),
            onNavigate = {},
            modifier = Modifier.height(420.dp),
            actions = {
                MonarchButton(
                    text = "Get started",
                    onClick = {},
                    variant = MonarchButtonVariant.Primary,
                    size = MonarchButtonSize.Sm,
                )
            },
            footer = {
                MonarchSiteFooter(
                    columns = listOf(
                        MonarchSiteFooterColumn("Company", listOf(MonarchSiteNavLink("About Rook", "about"))),
                    ),
                    onNavigate = {},
                    legal = "© 2026 Rook",
                    legalLinks = listOf(MonarchSiteNavLink("Privacy", "privacy")),
                )
            },
        ) {}
    }
}
