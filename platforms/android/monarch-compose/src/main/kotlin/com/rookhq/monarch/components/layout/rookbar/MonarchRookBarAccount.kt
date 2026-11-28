package com.rookhq.monarch.components.layout.rookbar

import androidx.compose.runtime.Immutable

/** The account controls of a [MonarchRookBar]. */
@Immutable
public sealed interface MonarchRookBarAccount {
    /** Nobody is signed in: "Log in" and "Sign up". */
    public data object SignedOut : MonarchRookBarAccount

    /**
     * A person is signed in: their name, "Open dashboard", "Account settings" and "Sign out".
     *
     * @property name the person's name.
     */
    public data class SignedIn(val name: String) : MonarchRookBarAccount
}
