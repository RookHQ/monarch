package dev.monarchui.components.navigation.megamenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.badge.MonarchBadgeSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.MonarchGlyphs
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.MonarchSmallCapsLabel
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * Site navigation with large groups, as a list that expands in place: the Android form of the web's mega menu, for
 * a navigation drawer or a full-height sheet.
 *
 * Each top-level entry is a row. An entry with sections expands below its row to show headed groups of links (icon
 * tile, label, badge and description) and an optional featured card; an entry without sections is a destination.
 * One entry is expanded at a time. Rows are buttons with 48dp touch targets; an expandable row reads its expanded
 * or collapsed state. Put it in a `ModalDrawerSheet` or a `MonarchBottomSheet`.
 *
 * @param items the top-level entries, in order.
 * @param expandedItem the id of the expanded entry, or `null`.
 * @param onExpandedItemChange called with the id of the entry to expand, or `null` to collapse it.
 * @param onNavigate called with the link of the destination the user taps.
 * @param modifier the modifier of the list.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchMegaMenu(
    items: List<MonarchMegaMenuItem>,
    expandedItem: String?,
    onExpandedItemChange: (String?) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    labels: MonarchMegaMenuLabels = MonarchMegaMenuLabels(),
) {
    val strings = MegaMenuStrings(
        expanded = monarchString(labels.expanded, R.string.monarch_megamenu_expanded),
        collapsed = monarchString(labels.collapsed, R.string.monarch_megamenu_collapsed),
        explore = monarchString(labels.explore, R.string.monarch_megamenu_explore),
        external = monarchString(labels.external, R.string.monarch_megamenu_external),
    )
    Column(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            val expanded = item.expandable && item.id == expandedItem
            TopRow(item, expanded, strings) {
                when {
                    item.expandable -> onExpandedItemChange(if (expanded) null else item.id)
                    item.link != null -> onNavigate(item.link)
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(MonarchTheme.motion.spec()),
                exit = shrinkVertically(MonarchTheme.motion.spec()),
            ) {
                Panel(item, strings, onNavigate)
            }
        }
    }
}

private class MegaMenuStrings(val expanded: String, val collapsed: String, val explore: String, val external: String)

@Composable
private fun TopRow(item: MonarchMegaMenuItem, expanded: Boolean, strings: MegaMenuStrings, onClick: () -> Unit) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .clip(MonarchShapeDefaults.Control)
            .background(MonarchMegaMenuTokens.expandedRow(expanded, colors))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                if (item.expandable) stateDescription = if (expanded) strings.expanded else strings.collapsed
            }
            .padding(
                horizontal = MonarchMegaMenuTokens.RowPaddingHorizontal,
                vertical = MonarchMegaMenuTokens.RowPaddingVertical,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(text = item.label, modifier = Modifier.weight(1f), style = MonarchTextStyle.BodyStrong)
        if (item.expandable) {
            MonarchIcon(
                imageVector = if (expanded) MonarchGlyphs.KeyboardArrowUp else MonarchGlyphs.KeyboardArrowDown,
                contentDescription = null,
                size = MonarchIconSize.Sm,
                tint = colors.textMuted,
            )
        }
    }
}

@Composable
private fun Panel(item: MonarchMegaMenuItem, strings: MegaMenuStrings, onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .padding(
                top = MonarchMegaMenuTokens.LinkPaddingVertical,
                bottom = MonarchMegaMenuTokens.PanelPaddingBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(MonarchMegaMenuTokens.SectionGap),
    ) {
        item.sections.forEach { section ->
            Column {
                if (section.heading != null) {
                    MonarchSmallCapsLabel(
                        text = section.heading,
                        modifier = Modifier.padding(horizontal = MonarchMegaMenuTokens.RowPaddingHorizontal),
                    )
                }
                if (section.description != null) {
                    MonarchText(
                        text = section.description,
                        modifier = Modifier.padding(horizontal = MonarchMegaMenuTokens.RowPaddingHorizontal),
                        style = MonarchTextStyle.Caption,
                        color = MonarchTheme.colors.textMuted,
                    )
                }
                section.links.forEach { link -> LinkRow(link, strings) { onNavigate(link.link) } }
            }
        }
        item.featured?.let { featured -> FeaturedCard(featured, strings) { onNavigate(featured.link) } }
    }
}

@Composable
private fun IconTile(fill: Color, icon: @Composable () -> Unit, bordered: Boolean = false) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Control
    Box(
        modifier = Modifier
            .size(MonarchMegaMenuTokens.IconTile)
            .background(fill, shape)
            .then(
                if (bordered) {
                    Modifier.border(MonarchShapeDefaults.BorderWidth, colors.borderSubtle, shape)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MonarchMegaMenuTokens.iconTint(colors), content = icon)
    }
}

@Composable
private fun LinkRow(link: MonarchMegaMenuLink, strings: MegaMenuStrings, onClick: () -> Unit) {
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MonarchTouchTarget.Minimum)
            .clip(MonarchShapeDefaults.Control)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (link.external) {
                    contentDescription = listOfNotNull(link.label, link.badge, link.description, strings.external)
                        .joinToString(", ")
                }
            }
            .padding(
                horizontal = MonarchMegaMenuTokens.RowPaddingHorizontal,
                vertical = MonarchMegaMenuTokens.LinkPaddingVertical,
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchMegaMenuTokens.IconGap),
        verticalAlignment = Alignment.Top,
    ) {
        link.icon?.let { IconTile(MonarchMegaMenuTokens.iconTile(colors), it) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(MonarchMegaMenuTokens.TextGap)) {
            LabelWithBadge(link.label, link.badge, MonarchTextStyle.BodyStrong)
            if (link.description != null) {
                MonarchText(text = link.description, style = MonarchTextStyle.Caption, color = colors.textMuted)
            }
        }
        if (link.external) {
            MonarchIcon(
                MonarchGlyphs.OpenInNew,
                contentDescription = null,
                size = MonarchIconSize.Sm,
                tint = colors.textMuted,
            )
        }
    }
}

@Composable
private fun LabelWithBadge(label: String, badge: String?, style: MonarchTextStyle) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MonarchMegaMenuTokens.BadgeGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(text = label, modifier = Modifier.weight(1f, fill = false), style = style, heading = false)
        if (badge != null) MonarchBadge(text = badge, tone = MonarchTone.Accent, size = MonarchBadgeSize.Sm)
    }
}

@Composable
private fun FeaturedCard(featured: MonarchMegaMenuFeatured, strings: MegaMenuStrings, onClick: () -> Unit) {
    val colors = MonarchTheme.colors
    val shape = MonarchShapeDefaults.Container
    val action = featured.actionLabel ?: strings.explore
    Column(
        modifier = Modifier
            .padding(horizontal = MonarchMegaMenuTokens.RowPaddingHorizontal)
            .fillMaxWidth()
            .clip(shape)
            .background(MonarchMegaMenuTokens.featuredFill(colors), shape)
            .border(MonarchShapeDefaults.BorderWidth, MonarchMegaMenuTokens.featuredBorder(colors), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (featured.external) {
                    contentDescription = listOfNotNull(
                        featured.title,
                        featured.badge,
                        featured.description,
                        action,
                        strings.external,
                    ).joinToString(", ")
                }
            }
            .padding(MonarchMegaMenuTokens.FeaturedPadding),
        verticalArrangement = Arrangement.spacedBy(MonarchMegaMenuTokens.FeaturedGap),
    ) {
        if (featured.icon != null || featured.badge != null) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                featured.icon?.let { IconTile(MonarchMegaMenuTokens.featuredTile(colors), it, bordered = true) }
                Box(Modifier.weight(1f))
                if (featured.badge != null) {
                    MonarchBadge(text = featured.badge, tone = MonarchTone.Accent, size = MonarchBadgeSize.Sm)
                }
            }
        }
        MonarchText(text = featured.title, style = MonarchTextStyle.Subheading, heading = false)
        if (featured.description != null) {
            MonarchText(text = featured.description, style = MonarchTextStyle.Label, color = colors.textMuted)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchMegaMenuTokens.TextGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = action, style = MonarchTextStyle.Label)
            MonarchIcon(
                MonarchGlyphs.KeyboardArrowRight,
                contentDescription = null,
                size = MonarchIconSize.Sm,
                tint = MonarchMegaMenuTokens.iconTint(colors),
            )
        }
    }
}
