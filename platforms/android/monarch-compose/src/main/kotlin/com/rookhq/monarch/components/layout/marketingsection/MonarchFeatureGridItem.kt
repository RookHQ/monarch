package com.rookhq.monarch.components.layout.marketingsection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One feature of a [MonarchFeatureGrid].
 *
 * @property title the name of the feature, read as a heading.
 * @property description one or two sentences on what it does.
 * @property icon an optional icon above the title, for example a `MonarchIcon` with a `null` content description.
 */
@Immutable
public class MonarchFeatureGridItem(
    public val title: String,
    public val description: String? = null,
    public val icon: (@Composable () -> Unit)? = null,
)
