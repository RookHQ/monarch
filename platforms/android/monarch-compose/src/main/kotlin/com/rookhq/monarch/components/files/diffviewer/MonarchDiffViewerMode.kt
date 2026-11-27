package com.rookhq.monarch.components.files.diffviewer

/** The layout of a `MonarchDiffViewer`. The same names exist on every platform. */
public enum class MonarchDiffViewerMode {
    /** One column: the removed lines of each change, then the added lines. */
    Unified,

    /** Two columns: the earlier text beside the later text. */
    Split,
}
