package com.rookhq.monarch.components.collaboration.activitytimeline

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.datagrid.loadmore.MonarchLoadMoreLabels

/**
 * Replacements for the built-in strings of a `MonarchActivityTimeline`. Every `null` uses the built-in string
 * named after it in `strings_activitytimeline.xml`.
 *
 * @property list the spoken name of a feed that is not grouped by day: "Activity".
 * @property today the heading of today's events: "Today".
 * @property yesterday the heading of yesterday's events: "Yesterday".
 * @property automated the tag of automation and system actors: "Automated".
 * @property details the disclosure that shows an event's details: "Details".
 * @property hideDetails the same disclosure while the details are shown: "Hide details".
 * @property empty the title of the empty state: "No activity yet".
 * @property loading the spoken name of the loading placeholder: "Loading activity".
 * @property error the message when the first page failed: "Activity didn't load.".
 * @property loadMore the strings of the Load more footer.
 */
@Immutable
public class MonarchActivityTimelineLabels(
    public val list: String? = null,
    public val today: String? = null,
    public val yesterday: String? = null,
    public val automated: String? = null,
    public val details: String? = null,
    public val hideDetails: String? = null,
    public val empty: String? = null,
    public val loading: String? = null,
    public val error: String? = null,
    public val loadMore: MonarchLoadMoreLabels = MonarchLoadMoreLabels(),
)
