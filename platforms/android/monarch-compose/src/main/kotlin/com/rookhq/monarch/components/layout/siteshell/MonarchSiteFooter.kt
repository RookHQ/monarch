package com.rookhq.monarch.components.layout.siteshell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The footer of a public area of an app: titled groups of links, a line, and the legal line with its links.
 *
 * Built from Compose layouts. Group titles are headings; each link is a button with a 48dp touch target that calls
 * [onNavigate]. Put it in the `footer` of a [MonarchSiteShell].
 *
 * @param columns the groups of links, stacked in order.
 * @param onNavigate called with the link the user taps.
 * @param modifier the modifier of the footer.
 * @param legal the legal line, such as "© 2026 Rook".
 * @param legalLinks the links after the legal line, such as "Privacy".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchSiteFooter(
    columns: List<MonarchSiteFooterColumn>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    legal: String? = null,
    legalLinks: List<MonarchSiteNavLink> = emptyList(),
) {
    val colors = MonarchTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .padding(MonarchSiteShellTokens.FooterPadding),
        verticalArrangement = Arrangement.spacedBy(MonarchSiteShellTokens.FooterGap),
    ) {
        columns.forEach { column ->
            Column(verticalArrangement = Arrangement.spacedBy(MonarchSiteShellTokens.FooterLinkGap)) {
                MonarchText(text = column.title, style = MonarchTextStyle.BodyStrong, heading = true)
                column.links.forEach { link ->
                    FooterLink(link.label, MonarchTextStyle.Body) { onNavigate(link.link) }
                }
            }
        }
        if (legal != null || legalLinks.isNotEmpty()) {
            MonarchDivider()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchSiteShellTokens.LegalGap),
            ) {
                if (legal != null) {
                    MonarchText(
                        text = legal,
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MonarchTextStyle.Caption,
                        color = colors.textMuted,
                    )
                }
                legalLinks.forEach { link ->
                    FooterLink(link.label, MonarchTextStyle.Caption) { onNavigate(link.link) }
                }
            }
        }
    }
}

@Composable
private fun FooterLink(label: String, style: MonarchTextStyle, onClick: () -> Unit) {
    MonarchText(
        text = label,
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(MonarchShapeDefaults.Control)
            .clickable(role = Role.Button, onClick = onClick)
            .wrapContentHeight(Alignment.CenterVertically),
        style = style,
        color = MonarchSiteShellTokens.footerLink(MonarchTheme.colors),
    )
}
