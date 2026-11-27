package com.rookhq.monarch.components.collaboration.postcard

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.foundation.MonarchTone

/**
 * A marker after the author line of a `MonarchPostCard`, such as the category
 * or a review status, drawn as a `MonarchBadge`.
 *
 * @property label the text of the badge.
 * @property tone the colour of the badge.
 */
@Immutable
public class MonarchPostCardTag(public val label: String, public val tone: MonarchTone = MonarchTone.Neutral)
