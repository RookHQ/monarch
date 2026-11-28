package dev.monarchui.components.navigation.link

/** The look of a `MonarchLink`. The same three names exist on the web and on iOS. */
public enum class MonarchLinkVariant {
    /** The link colour, underlined. For a link that follows a sentence or sits among text. The default. */
    Inline,

    /** The link colour in medium weight, underlined while pressed or focused. For links in lists and footers. */
    Standalone,

    /** The muted text colour, underlined while pressed or focused. For secondary links under a field. */
    Subtle,
}
