package com.rookhq.monarch.foundation

/**
 * The type roles of Monarch. The same nine names exist on every platform.
 *
 * Resolve a role to a Compose text style with [MonarchTypography.style].
 */
public enum class MonarchTextStyle {
    /** The largest text on a screen, for example a hero figure. */
    Display,

    /** A screen title. */
    Title,

    /** A section heading. */
    Heading,

    /** A heading inside a section or a card. */
    Subheading,

    /** Running text. */
    Body,

    /** Running text with emphasis. */
    BodyStrong,

    /** Text of controls: buttons, field labels, tabs. */
    Label,

    /** Secondary text: helper text, timestamps, metadata. */
    Caption,

    /** Code and other fixed-width text. */
    Code,

    /** Small fixed-width text in a caption: keyboard keys, ids and hashes. */
    CodeSmall,
}

/** Whether text in this role is announced as a heading unless the caller says otherwise. */
internal val MonarchTextStyle.isHeading: Boolean
    get() = when (this) {
        MonarchTextStyle.Display,
        MonarchTextStyle.Title,
        MonarchTextStyle.Heading,
        MonarchTextStyle.Subheading,
        -> true
        else -> false
    }
