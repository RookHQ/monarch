package com.rookhq.monarch.components.typography.prosestyle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import com.rookhq.monarch.components.navigation.link.MonarchLinkDefaults
import com.rookhq.monarch.components.navigation.link.MonarchLinkVariant
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Type styles for long-form content the app does not write element by
 * element: help articles, release notes, rendered Markdown, comments and
 * documents. A base size, a reading measure and the look of each kind of
 * block, on the theme's type roles and colours.
 *
 * Pass it to `MonarchProse`, which lays out headings, paragraphs, lists,
 * quotations, code and rules with the web's rhythm, or read the text styles
 * directly when the app renders its own blocks. Monarch renders no HTML and
 * parses no Markdown: the app's renderer (or the app) says which kind each
 * block is.
 *
 * @property size the base size: `Sm` 14sp, `Md` 16sp, `Lg` 18sp. Headings, code and spacing are multiples of it,
 * and every size follows the user's font size setting.
 * @property limitLineLength limits the column to the reading measure, 42 base sizes (672dp at `Md` and the
 * default font size), at the start edge. Turn it off when the container already sets the width.
 */
@Immutable
public class MonarchProseStyle(
    public val size: MonarchProseSize = MonarchProseSize.Md,
    public val limitLineLength: Boolean = true,
) {
    /** The style of running text in the current theme: the body face at the base size with relaxed leading. */
    @Composable
    @ReadOnlyComposable
    public fun bodyStyle(): TextStyle =
        MonarchProseStyleTokens.body(size, MonarchTheme.typography, MonarchTheme.colors)

    /**
     * The style of a heading in the current theme: semibold, from 1.875 times the base size at level 1 down to
     * the base size at levels 5 and 6; level 6 is in the muted colour.
     *
     * @param level 1 to 6; values outside are clamped.
     */
    @Composable
    @ReadOnlyComposable
    public fun headingStyle(level: Int): TextStyle =
        MonarchProseStyleTokens.heading(level, size, MonarchTheme.typography, MonarchTheme.colors)

    /** The style of links inside prose: the link colour, underlined, for a `LinkAnnotation`. */
    @Composable
    public fun linkStyles(): TextLinkStyles = MonarchLinkDefaults.styles(MonarchLinkVariant.Inline)

    override fun equals(other: Any?): Boolean =
        other is MonarchProseStyle && other.size == size && other.limitLineLength == limitLineLength

    override fun hashCode(): Int = 31 * size.hashCode() + limitLineLength.hashCode()
}
