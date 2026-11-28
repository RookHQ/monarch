package dev.monarchui.components.collaboration.postcard

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchPostCard`. Every `null`
 * keeps the built-in string `monarch_postcard_<key>`.
 *
 * @property pinned the pinned marker in the author line.
 * @property answered the answered badge.
 * @property reactions the reaction count, given the count: "12 reactions".
 * @property comments the comment count, given the count: "4 comments".
 */
@Immutable
public class MonarchPostCardLabels(
    public val pinned: String? = null,
    public val answered: String? = null,
    public val reactions: ((Int) -> String)? = null,
    public val comments: ((Int) -> String)? = null,
)
