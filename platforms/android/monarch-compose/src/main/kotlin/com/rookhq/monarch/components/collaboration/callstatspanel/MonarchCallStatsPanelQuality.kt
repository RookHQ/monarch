package com.rookhq.monarch.components.collaboration.callstatspanel

/** The connection grade of one person in a `MonarchCallStatsPanel`, shown as a status badge with its name. */
public enum class MonarchCallStatsPanelQuality {
    /** "Good connection", in the success tone. */
    Good,

    /** "Unstable connection", in the warning tone. */
    Fair,

    /** "Poor connection", in the danger tone. */
    Poor,

    /** "Not measured yet", neutral. */
    Unknown,
}
