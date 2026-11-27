package com.rookhq.monarch.components.collaboration.activitytimeline

import androidx.compose.runtime.Immutable

/**
 * One line of the details of a `MonarchActivityTimelineEvent`: a field and what happened to it.
 *
 * @property label the field, such as "Status". Already localised.
 * @property value what changed, such as "In progress → In review".
 */
@Immutable
public class MonarchActivityTimelineDetail(
    public val label: String,
    public val value: String,
)
