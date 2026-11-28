package dev.monarchui.components.technical.dnsrecordtable

/** How a `MonarchDnsRecordTable` lays its records out. */
public enum class MonarchDnsRecordTableLayout {
    /** A table when the width is at least 640dp and the font scale at most 1.3, cards otherwise. */
    Automatic,

    /** One card per record. Fits any width and any font size. */
    Cards,

    /** One row per record under column headings, for wide screens. */
    Table,
}
