package dev.monarchui.internal

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A built-in string, unless the caller replaced it.
 *
 * Every string Monarch supplies is a resource named `monarch_<component>_<key>`
 * in `res/values/strings_<component>.xml`, and every one can be replaced per
 * call by a nullable parameter. This is the one place that rule is applied.
 *
 * @param override the caller's text, or `null` for the built-in string.
 * @param default the resource of the built-in string.
 */
@Composable
internal fun monarchString(override: String?, @StringRes default: Int): String = override ?: stringResource(default)
