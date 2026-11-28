package com.rookhq.monarch.components.identity.recoverycodes

import androidx.compose.runtime.Immutable

/**
 * One recovery code of a `MonarchRecoveryCodes`.
 *
 * @property code the code, such as "4f7k-2m9q".
 * @property used whether it was already used. Used codes are struck through and not copied or saved.
 */
@Immutable
public class MonarchRecoveryCode(public val code: String, public val used: Boolean = false)
