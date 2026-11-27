package com.rookhq.monarch.foundation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.tokens.MonarchTokens

/**
 * Widths and strokes for laying out content, from the generated
 * `MonarchTokens.Width` and `MonarchTokens.Layout` (the web's `width` and
 * `layout` tokens), in dp. The same names as `MonarchLayout` on iOS.
 *
 * ```kotlin
 * Column(Modifier.widthIn(max = MonarchLayout.readingMax).verticalScroll(rememberScrollState())) {
 *     MonarchText(article)
 * }
 * ```
 *
 * On a phone the screen is narrower than every width here; they matter on
 * tablets, foldables and desktop windows.
 */
public object MonarchLayout {
    /** The longest comfortable line of running text: 672dp (42rem). */
    public val readingMax: Dp = MonarchTokens.Layout.readingMax.dp

    /** A narrow column, such as a settings page or a form: 704dp (44rem). */
    public val narrowWidth: Dp = MonarchTokens.Width.narrow.dp

    /** The main content column: 1152dp (72rem). */
    public val contentWidth: Dp = MonarchTokens.Width.content.dp

    /** A wide content area, such as a dashboard: 1280dp (80rem). */
    public val wideWidth: Dp = MonarchTokens.Width.wide.dp

    /** The widest an app's content grows: 1280dp (80rem). */
    public val contentMax: Dp = MonarchTokens.Layout.contentMax.dp

    /** The border of a selected surface, such as a chosen card or row: 2dp. */
    public val selectedBorderWidth: Dp = MonarchTokens.Space.space0_5.dp

    /** The border of a surface at rest: 1dp. */
    public val borderWidth: Dp = MonarchTokens.Space.px.dp
}
