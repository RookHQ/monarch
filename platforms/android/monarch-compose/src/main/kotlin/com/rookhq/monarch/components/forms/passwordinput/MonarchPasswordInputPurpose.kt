package com.rookhq.monarch.components.forms.passwordinput

/** What a password field is for, which sets the autofill content type. */
public enum class MonarchPasswordInputPurpose {
    /** An existing password: sign-in or confirming an action. Autofill offers the saved password. */
    Current,

    /** A new password: sign-up or reset. Autofill offers to generate and save a strong password. */
    New,
}
