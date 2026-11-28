package dev.monarchui.components.layout.marketingsection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.theme.MonarchTheme

/**
 * A band of a marketing or onboarding screen: an optional header (eyebrow, title, description) above its content,
 * such as a [MonarchFeatureGrid], a [MonarchStatStrip] or a [MonarchFaq].
 *
 * Built from Compose `Column`; the title is a heading so TalkBack users can move from band to band, and the band is
 * a traversal group so its content is read together.
 *
 * @param modifier the modifier of the band.
 * @param title the title of the band, read as a heading. `null` shows no header.
 * @param eyebrow a short line above the title, in the accent.
 * @param description the muted line under the title.
 * @param align whether the header starts at the start edge or is centred.
 * @param tone no fill, or the subtle background fill.
 * @param content the blocks of the band, in a column.
 */
@Composable
public fun MonarchMarketingSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    eyebrow: String? = null,
    description: String? = null,
    align: MonarchMarketingAlign = MonarchMarketingAlign.Start,
    tone: MonarchMarketingSectionTone = MonarchMarketingSectionTone.Default,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val fill = MonarchMarketingSectionTokens.sectionFill(tone, colors)
    val padding = if (tone == MonarchMarketingSectionTone.Subtle) MonarchMarketingSectionTokens.SectionPadding else null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .background(fill, MonarchTheme.shapes.lg)
            .then(if (padding != null) Modifier.padding(padding) else Modifier),
        verticalArrangement = Arrangement.spacedBy(MonarchMarketingSectionTokens.SectionGap),
        horizontalAlignment = if (align == MonarchMarketingAlign.Center) {
            Alignment.CenterHorizontally
        } else {
            Alignment.Start
        },
    ) {
        if (title != null) {
            MarketingHeader(title = title, eyebrow = eyebrow, description = description, align = align)
        }
        content()
    }
}
