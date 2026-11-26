package com.rookhq.monarch.foundation

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The Monarch corner shapes. Property names are the token names in
 * `MonarchTokens.Radius`.
 *
 * Read it through `MonarchTheme.shapes`.
 */
@Immutable
public class MonarchShapes private constructor() {
    /** Square corners. */
    public val none: CornerBasedShape = RoundedCornerShape(MonarchTokens.Radius.none.dp)

    /** 4dp corners. */
    public val xs: CornerBasedShape = RoundedCornerShape(MonarchTokens.Radius.xs.dp)

    /** 6dp corners. */
    public val sm: CornerBasedShape = RoundedCornerShape(MonarchTokens.Radius.sm.dp)

    /** 8dp corners: buttons and fields. */
    public val md: CornerBasedShape = RoundedCornerShape(MonarchTokens.Radius.md.dp)

    /** 12dp corners: cards and banners. */
    public val lg: CornerBasedShape = RoundedCornerShape(MonarchTokens.Radius.lg.dp)

    /** 16dp corners. */
    public val xl: CornerBasedShape = RoundedCornerShape(MonarchTokens.Radius.xl.dp)

    /** 20dp corners. */
    public val xl2: CornerBasedShape = RoundedCornerShape(MonarchTokens.Radius.xl2.dp)

    /** Fully rounded ends: badges and pills. */
    public val full: CornerBasedShape = RoundedCornerShape(percent = FULL_PERCENT)

    /** Holds the shape scale. */
    public companion object {
        private const val FULL_PERCENT = 50

        /** The Monarch corner shapes. */
        public val Default: MonarchShapes = MonarchShapes()
    }
}
