package com.rookhq.monarch.components.collaboration.inboxrow

/** The unit of the compact age of a `MonarchInboxRow`: "now", "5m", "3h", "2d", "4w". */
public enum class MonarchInboxRowAgeUnit {
    /** Less than a minute. */
    Now,

    /** Minutes, under an hour. */
    Minutes,

    /** Hours, under a day. */
    Hours,

    /** Days, under a week. */
    Days,

    /** Weeks, under 30 days. Older times show a short date. */
    Weeks,
}
