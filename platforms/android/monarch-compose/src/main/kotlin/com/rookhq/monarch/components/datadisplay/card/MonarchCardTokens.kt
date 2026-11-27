package com.rookhq.monarch.components.datadisplay.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchCard, built from primitives and roles only. */
internal object MonarchCardTokens {
    val BorderWidth: Dp = MonarchTokens.Space.px.dp

    /** The padding around the content of a card: 20dp, like the web. */
    val ContentPadding: PaddingValues = PaddingValues(MonarchTokens.Space.space5.dp)

    /** The header has 20dp above and at the sides and none below: the content brings its own padding. */
    val HeaderPadding: PaddingValues = PaddingValues(
        start = MonarchTokens.Space.space5.dp,
        top = MonarchTokens.Space.space5.dp,
        end = MonarchTokens.Space.space5.dp,
    )

    /** The footer has 12dp above and below and 20dp at the sides. */
    val FooterPadding: PaddingValues =
        PaddingValues(horizontal = MonarchTokens.Space.space5.dp, vertical = MonarchTokens.Space.space3.dp)

    /** The space between the title and the description. */
    val TitleGap: Dp = MonarchTokens.Space.space1.dp

    /** The space between the text of the header and its trailing element. */
    val HeaderGap: Dp = MonarchTokens.Space.space3.dp

    fun container(elevated: Boolean, colors: MonarchColors): Color =
        if (elevated) colors.bgRaised else colors.bgSurface

    /** A chosen card takes the subtle accent fill, with the accent edge of [border]; otherwise [container]. */
    fun container(elevated: Boolean, selected: Boolean, colors: MonarchColors): Color =
        if (selected) colors.accentSubtle else container(elevated, colors)

    /** The chosen edge: 1dp, drawn inside the card by the border of the surface, so the content does not move. */
    val SelectedBorderWidth: Dp = BorderWidth

    /** Selected wins, then the tone, then the default border of the surface. */
    fun border(elevated: Boolean, selected: Boolean, tone: MonarchTone?, colors: MonarchColors): BorderStroke = when {
        selected -> BorderStroke(SelectedBorderWidth, colors.accentSolid)
        tone != null && tone != MonarchTone.Neutral ->
            BorderStroke(BorderWidth, MonarchTonePalette.of(tone, colors).border)
        else -> BorderStroke(BorderWidth, if (elevated) colors.borderSubtle else colors.borderDefault)
    }
}
