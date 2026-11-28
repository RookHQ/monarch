package com.rookhq.monarch.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * The Monarch spacing scale in dp, on a 4dp grid.
 *
 * Property names are the token names in `MonarchTokens.Space`, so a value in
 * a design file reads the same in code. `MonarchSpacingTest` fails when the
 * generated scale gains a step that is not mirrored here.
 *
 * Read it through `MonarchTheme.spacing`.
 */
@Immutable
public class MonarchSpacing private constructor() {
    /** 0dp. */
    public val space0: Dp get() = MonarchTokens.Space.space0.dp

    /** 4dp. */
    public val space1: Dp get() = MonarchTokens.Space.space1.dp

    /** 8dp. */
    public val space2: Dp get() = MonarchTokens.Space.space2.dp

    /** 12dp. */
    public val space3: Dp get() = MonarchTokens.Space.space3.dp

    /** 16dp. */
    public val space4: Dp get() = MonarchTokens.Space.space4.dp

    /** 20dp. */
    public val space5: Dp get() = MonarchTokens.Space.space5.dp

    /** 24dp. */
    public val space6: Dp get() = MonarchTokens.Space.space6.dp

    /** 32dp. */
    public val space8: Dp get() = MonarchTokens.Space.space8.dp

    /** 40dp. */
    public val space10: Dp get() = MonarchTokens.Space.space10.dp

    /** 48dp. */
    public val space12: Dp get() = MonarchTokens.Space.space12.dp

    /** 64dp. */
    public val space16: Dp get() = MonarchTokens.Space.space16.dp

    /** 80dp. */
    public val space20: Dp get() = MonarchTokens.Space.space20.dp

    /** 96dp. */
    public val space24: Dp get() = MonarchTokens.Space.space24.dp

    /** 1dp. */
    public val px: Dp get() = MonarchTokens.Space.px.dp

    /** 2dp. */
    public val space0_5: Dp get() = MonarchTokens.Space.space0_5.dp

    /** 6dp. */
    public val space1_5: Dp get() = MonarchTokens.Space.space1_5.dp

    /** 10dp. */
    public val space2_5: Dp get() = MonarchTokens.Space.space2_5.dp

    /** Holds the spacing scale. */
    public companion object {
        /** The Monarch spacing scale. */
        public val Default: MonarchSpacing = MonarchSpacing()
    }
}
