package com.rookhq.monarch.components.layout.marketingsection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The header of a marketing band, shared by the section, the hero, the pricing and the comparison sections: the
 * eyebrow in the accent, the title as a heading and the muted description.
 */
@Composable
internal fun MarketingHeader(
    title: String,
    eyebrow: String?,
    description: String?,
    align: MonarchMarketingAlign,
    modifier: Modifier = Modifier,
    titleStyle: MonarchTextStyle = MonarchTextStyle.Title,
) {
    val colors = MonarchTheme.colors
    val centred = align == MonarchMarketingAlign.Center
    val textAlign = if (centred) TextAlign.Center else TextAlign.Start
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchMarketingSectionTokens.HeaderGap),
        horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (eyebrow != null) {
            MonarchText(
                text = eyebrow,
                style = MonarchTextStyle.Label,
                color = MonarchMarketingSectionTokens.eyebrow(colors),
                textAlign = textAlign,
            )
        }
        MonarchText(text = title, style = titleStyle, textAlign = textAlign, heading = true)
        if (description != null) {
            MonarchText(
                text = description,
                style = MonarchTextStyle.Body,
                color = colors.textMuted,
                textAlign = textAlign,
            )
        }
    }
}
