package dev.monarchui.components.navigation.megamenu

import androidx.compose.runtime.Composable
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.preview.MonarchPreviews
import dev.monarchui.internal.preview.PreviewFrame
import dev.monarchui.internal.preview.PreviewIcon

@MonarchPreviews
@Composable
internal fun MonarchMegaMenuPreview() {
    PreviewFrame {
        MonarchMegaMenu(
            items = listOf(
                MonarchMegaMenuItem(
                    id = "product",
                    label = "Product",
                    sections = listOf(
                        MonarchMegaMenuSection(
                            heading = "Plan",
                            links = listOf(
                                MonarchMegaMenuLink(
                                    label = "Calendar",
                                    link = "calendar",
                                    description = "Deadlines and milestones in one view",
                                    badge = "New",
                                    icon = { MonarchIcon(
                                        PreviewIcon,
                                        contentDescription = null,
                                        size = MonarchIconSize.Sm,
                                    ) },
                                ),
                            ),
                        ),
                    ),
                    featured = MonarchMegaMenuFeatured(
                        title = "What changed this month",
                        link = "changelog",
                        description = "A short tour of the newest features and fixes.",
                        badge = "Updated",
                    ),
                ),
                MonarchMegaMenuItem(id = "support", label = "Support", link = "support"),
            ),
            expandedItem = "product",
            onExpandedItemChange = {},
            onNavigate = {},
        )
    }
}
