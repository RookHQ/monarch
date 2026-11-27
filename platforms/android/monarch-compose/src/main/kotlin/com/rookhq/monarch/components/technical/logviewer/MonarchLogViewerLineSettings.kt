package com.rookhq.monarch.components.technical.logviewer

import java.util.TimeZone

/** How the lines of a MonarchLogViewer are drawn. */
internal data class MonarchLogViewerLineSettings(
    val query: String,
    val wrapped: Boolean,
    val showTimestamps: Boolean,
    val showLineNumbers: Boolean,
    val zone: TimeZone,
)
