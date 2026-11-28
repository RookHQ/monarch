package dev.monarchui.components.identity.avatar

/** The size of a `MonarchAvatar`, before the font size setting is applied. */
public enum class MonarchAvatarSize {
    /**
     * 20dp, inline with caption text such as a byline or a reaction. An avatar
     * is not a touch target; give a clickable one a 48dp target.
     */
    Xs,

    /** 24dp, for dense lists and avatar groups. */
    Sm,

    /** 32dp. The default. */
    Md,

    /** 40dp, for a list item or a header. */
    Lg,

    /** 56dp, on a profile header or a call screen. */
    Xl,
}
