package dev.monarchui.components.datadisplay.keyvalue

/** How the pairs of a `MonarchKeyValue` are laid out. */
public enum class MonarchKeyValueLayout {
    /** Pairs flow onto more lines, with separators between them. The default. */
    Wrap,

    /** Each pair on its own line, for narrow cards. No separators. */
    Stack,
}
