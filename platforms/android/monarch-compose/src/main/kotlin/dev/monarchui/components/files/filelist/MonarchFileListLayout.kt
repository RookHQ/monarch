package dev.monarchui.components.files.filelist

/** How a `MonarchFileList` lays out its files. The same names exist on every platform. */
public enum class MonarchFileListLayout {
    /** One row per file, with a small preview. The default. */
    List,

    /** Cards with previews in columns whose width grows with the font size. */
    Grid,
}
