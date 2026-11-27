package com.rookhq.monarch.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchShadowLayer
import com.rookhq.monarch.tokens.MonarchShadowTokens

/**
 * One elevation level: the shadow layers of a theme at that level.
 *
 * Draw it with [monarchShadow].
 */
@Immutable
public class MonarchShadow internal constructor(internal val layers: List<Shadow>)

/**
 * The five shadow levels of the active theme.
 *
 * Read it through `MonarchTheme.elevation`.
 *
 * @property xs the lowest level: controls.
 * @property sm cards.
 * @property md raised cards and menus.
 * @property lg dialogs and sheets.
 * @property xl the highest level.
 */
@Immutable
public class MonarchElevation internal constructor(
    public val xs: MonarchShadow,
    public val sm: MonarchShadow,
    public val md: MonarchShadow,
    public val lg: MonarchShadow,
    public val xl: MonarchShadow,
) {
    /**
     * The one shadow for presented surfaces: menus, popovers, dialogs, sheets and toasts. It is the soft, low
     * [md] level. Buttons, fields, chips, list rows and inline cards draw no shadow: a hairline border and the
     * surface colour separate them. In the dark themes the shadow barely shows, so surfaces are told apart by
     * their surface step (`bgRaised`).
     */
    public val presented: MonarchShadow get() = md

    internal companion object {
        /** Converts the generated shadow tokens of a theme. */
        fun from(tokens: MonarchShadowTokens): MonarchElevation = MonarchElevation(
            xs = tokens.xs.toShadow(),
            sm = tokens.sm.toShadow(),
            md = tokens.md.toShadow(),
            lg = tokens.lg.toShadow(),
            xl = tokens.xl.toShadow(),
        )

        private fun List<MonarchShadowLayer>.toShadow(): MonarchShadow = MonarchShadow(
            map { layer ->
                Shadow(
                    radius = layer.blur.dp,
                    color = Color(layer.color),
                    spread = layer.spread.dp,
                    offset = DpOffset(layer.offsetX.dp, layer.offsetY.dp),
                )
            },
        )
    }
}

/**
 * Draws a Monarch shadow behind this element.
 *
 * @param shadow a level from `MonarchTheme.elevation`.
 * @param shape the outline that casts the shadow; use the shape of the element.
 */
public fun Modifier.monarchShadow(shadow: MonarchShadow, shape: Shape): Modifier =
    // The first token layer is the one closest to the element, so it is drawn last.
    shadow.layers.asReversed().fold(this) { modifier, layer -> modifier.dropShadow(shape, layer) }

/** The colours of the layers of [this] level, for a control that draws its own hairline (a thumb on a canvas). */
internal val MonarchShadow.layerColors: List<Color> get() = layers.map { it.color }
