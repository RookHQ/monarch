package com.rookhq.monarch.components.identity.usernameavailabilityinput

import androidx.compose.runtime.Immutable

/**
 * The answer of the check of a `MonarchUsernameAvailabilityInput`.
 *
 * @property valid whether the value can be used, for example whether the handle is free.
 * @property message shown under the field and announced. `null` uses the built-in text of the status.
 */
@Immutable
public class MonarchUsernameAvailabilityResult(
    public val valid: Boolean,
    public val message: String? = null,
)
