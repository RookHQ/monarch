package dev.monarchui.components.typography.prosestyle

/** The base size of long-form text. Headings, code and spacing are multiples of it. */
public enum class MonarchProseSize {
    /** 14sp, for comments, chat and side panels. */
    Sm,

    /** 16sp, for documents. The web's `base`. */
    Md,

    /** 18sp, for long reads. */
    Lg,
}
