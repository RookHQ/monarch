package com.rookhq.monarch.components.identity.signinwithrookbutton

/**
 * The look of a `MonarchSignInWithRookButton`. `Light`, `Dark` and `Neutral`
 * look the same on every screen, whatever the theme; `Auto` follows the theme.
 */
public enum class MonarchSignInWithRookButtonVariant {
    /** The light theme's surface with a strong border. The default. */
    Light,

    /** The dark theme's surface with a strong border. */
    Dark,

    /** A light grey fill without a border. */
    Neutral,

    /** The surface of the current theme with a strong border. */
    Auto,
}
