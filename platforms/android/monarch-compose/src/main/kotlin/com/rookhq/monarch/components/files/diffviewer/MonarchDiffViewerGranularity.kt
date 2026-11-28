package com.rookhq.monarch.components.files.diffviewer

/** How finely a `MonarchDiffViewer` marks changes. The same names exist on every platform. */
public enum class MonarchDiffViewerGranularity {
    /** Whole lines only. */
    Line,

    /** Whole lines, and the changed words inside a changed line. The default. */
    Word,
}
