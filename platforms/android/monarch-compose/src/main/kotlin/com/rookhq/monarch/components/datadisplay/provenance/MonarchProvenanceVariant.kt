package com.rookhq.monarch.components.datadisplay.provenance

/** The form of a `MonarchProvenance`. The same names exist on every platform. */
public enum class MonarchProvenanceVariant {
    /** A small bordered tag next to a value. The default. */
    Chip,

    /** Plain muted text, for use inside a sentence or a dense row. */
    Inline,
}
