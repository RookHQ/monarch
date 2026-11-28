package com.rookhq.monarch.components.feedback.calloutbanner

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.internal.MonarchTonePalette
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens of MonarchCalloutBanner, built from primitives and roles only. */
internal object MonarchCalloutBannerTokens {
    val TitleGap: Dp = MonarchTokens.Space.space0_5.dp
    val ActionGap: Dp = MonarchTokens.Space.space2.dp

    fun contentGap(variant: MonarchCalloutBannerVariant): Dp = when (variant) {
        MonarchCalloutBannerVariant.Callout -> MonarchTokens.Space.space3.dp
        MonarchCalloutBannerVariant.Scope -> MonarchTokens.Space.space2.dp
    }

    fun padding(variant: MonarchCalloutBannerVariant, placement: MonarchCalloutBannerPlacement): PaddingValues =
        when {
            variant == MonarchCalloutBannerVariant.Scope ->
                PaddingValues(horizontal = MonarchTokens.Space.space3.dp, vertical = MonarchTokens.Space.space1_5.dp)
            placement == MonarchCalloutBannerPlacement.Page ->
                PaddingValues(horizontal = MonarchTokens.Space.space4.dp, vertical = MonarchTokens.Space.space2_5.dp)
            else ->
                PaddingValues(horizontal = MonarchTokens.Space.space4.dp, vertical = MonarchTokens.Space.space3.dp)
        }

    /** Square for a page strip; 8dp corners for a scope notice; 12dp corners for a section callout. */
    fun shape(variant: MonarchCalloutBannerVariant, placement: MonarchCalloutBannerPlacement): Shape = when {
        placement == MonarchCalloutBannerPlacement.Page -> RectangleShape
        variant == MonarchCalloutBannerVariant.Scope -> MonarchShapeDefaults.Control
        else -> MonarchShapeDefaults.Container
    }

    /**
     * The tinted background, border and icon colour of the tone. The neutral
     * banner sits on the subtle background with a muted icon, as on the web.
     */
    fun colors(tone: MonarchTone, colors: MonarchColors): MonarchSurfaceColors {
        if (tone == MonarchTone.Neutral) {
            return MonarchSurfaceColors(colors.bgSubtle, colors.textMuted, colors.borderDefault)
        }
        val palette = MonarchTonePalette.of(tone, colors)
        return MonarchSurfaceColors(palette.subtle, palette.strong, palette.border)
    }
}
