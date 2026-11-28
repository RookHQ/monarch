package dev.monarchui.components.files.filetypeicon

/** The size of a `MonarchFileTypeIcon`. The same three names exist on every platform. */
public enum class MonarchFileTypeIconSize {
    /** A 16dp glyph. Inside captions and dense rows. */
    Sm,

    /** A 20dp glyph. Next to body text, in file rows. The default. */
    Md,

    /** A 24dp glyph on a 40dp tinted tile. For file cards and empty previews. */
    Lg,
}
