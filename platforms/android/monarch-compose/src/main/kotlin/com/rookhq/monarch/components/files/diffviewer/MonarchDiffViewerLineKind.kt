package com.rookhq.monarch.components.files.diffviewer

/** What happened to one line of a `MonarchDiffViewer`. The same names exist on every platform. */
public enum class MonarchDiffViewerLineKind {
    /** In both texts. */
    Same,

    /** Only in the later text. */
    Added,

    /** Only in the earlier text. */
    Removed,
}
