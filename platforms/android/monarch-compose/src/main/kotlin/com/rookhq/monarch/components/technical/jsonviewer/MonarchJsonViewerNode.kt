package com.rookhq.monarch.components.technical.jsonviewer

/** What a node row shows and how it behaves. */
internal class MonarchJsonViewerNode(
    val row: MonarchJsonViewerRow,
    val summary: String,
    val needle: String,
    val open: Boolean,
    val selected: Boolean,
)
