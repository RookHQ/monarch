package com.rookhq.monarch.components.layout.marketinghero

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.components.layout.marketingsection.MonarchMarketingAlign
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The opening of a welcome, launch or onboarding screen: an eyebrow, a large title, a lead, the actions, a note and
 * optional media, on a subtle wash of the product accent.
 *
 * Built from Compose layouts. The title uses the display style and is the heading of the screen; the actions wrap
 * onto more lines at large font sizes. Make the first action a primary `MonarchButton` and give the actions the
 * full shape, as the web's pill-shaped hero buttons.
 *
 * @param title the main line, read as a heading.
 * @param modifier the modifier of the hero.
 * @param eyebrow a short line above the title, in the accent, such as the product name.
 * @param lead one or two sentences under the title, in the muted colour.
 * @param note a small line under the actions, such as "Set up in under five minutes."
 * @param align centred (the default) or at the start edge.
 * @param wash whether the subtle accent wash is drawn behind the hero.
 * @param media an illustration below the text, such as a `MonarchProductFrame`.
 * @param actions the buttons.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchMarketingHero(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    lead: String? = null,
    note: String? = null,
    align: MonarchMarketingAlign = MonarchMarketingAlign.Center,
    wash: Boolean = true,
    media: (@Composable () -> Unit)? = null,
    actions: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    val colors = MonarchTheme.colors
    val centred = align == MonarchMarketingAlign.Center
    val textAlign = if (centred) TextAlign.Center else TextAlign.Start
    val horizontal = if (centred) Alignment.CenterHorizontally else Alignment.Start
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .then(if (wash) Modifier.background(MonarchMarketingHeroTokens.wash(colors)) else Modifier)
            .padding(
                horizontal = MonarchMarketingHeroTokens.HorizontalPadding,
                vertical = MonarchMarketingHeroTokens.VerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(MonarchMarketingHeroTokens.Gap),
        horizontalAlignment = horizontal,
    ) {
        if (eyebrow != null) {
            MonarchText(
                text = eyebrow,
                style = MonarchTextStyle.Label,
                color = MonarchMarketingHeroTokens.eyebrow(colors),
                textAlign = textAlign,
            )
        }
        MonarchText(text = title, style = MonarchTextStyle.Display, textAlign = textAlign, heading = true)
        if (lead != null) {
            Text(
                text = lead,
                style = MonarchTheme.typography.subheading.copy(fontWeight = FontWeight.Normal),
                color = colors.textMuted,
                textAlign = textAlign,
            )
        }
        if (actions != null) {
            Spacer(Modifier.height(MonarchMarketingHeroTokens.ActionGap))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchMarketingHeroTokens.ActionGap, horizontal),
                verticalArrangement = Arrangement.spacedBy(MonarchMarketingHeroTokens.ActionGap),
                content = actions,
            )
        }
        if (note != null) {
            MonarchText(text = note, style = MonarchTextStyle.Caption, color = colors.textMuted, textAlign = textAlign)
        }
        if (media != null) {
            Spacer(Modifier.height(MonarchMarketingHeroTokens.MediaGap))
            media()
        }
    }
}
