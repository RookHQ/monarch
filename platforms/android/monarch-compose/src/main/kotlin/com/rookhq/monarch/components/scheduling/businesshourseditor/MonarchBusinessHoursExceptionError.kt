package com.rookhq.monarch.components.scheduling.businesshourseditor

/** Why an exception date of a [MonarchBusinessHoursValue] is invalid. */
public enum class MonarchBusinessHoursExceptionError {
    /** No date is chosen. */
    Date,

    /** The date has no name. */
    Name,

    /** The same date is listed twice. */
    Duplicate,

    /** A special-hours interval closes before it opens or falls outside the day. */
    Hours,
}
