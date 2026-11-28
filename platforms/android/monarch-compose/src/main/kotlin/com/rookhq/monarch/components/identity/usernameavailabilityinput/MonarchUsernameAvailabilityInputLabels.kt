package com.rookhq.monarch.components.identity.usernameavailabilityinput

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchUsernameAvailabilityInput`.
 * Every `null` keeps the built-in string `monarch_usernameavailabilityinput_<key>`.
 * A message returned by the check wins over both.
 *
 * @property checking shown while the check runs, "Checking availability…". Not announced.
 * @property valid shown and announced when the value can be used, "Available".
 * @property invalid shown and announced when it cannot, "Not available".
 * @property error shown and announced when the check failed, "Couldn’t check availability."
 * @property retry the button shown after a failed check, "Try again".
 */
@Immutable
public class MonarchUsernameAvailabilityInputLabels(
    public val checking: String? = null,
    public val valid: String? = null,
    public val invalid: String? = null,
    public val error: String? = null,
    public val retry: String? = null,
)
