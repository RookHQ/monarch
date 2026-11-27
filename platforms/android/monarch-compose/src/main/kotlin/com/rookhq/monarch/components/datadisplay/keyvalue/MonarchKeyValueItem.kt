package com.rookhq.monarch.components.datadisplay.keyvalue

import androidx.compose.runtime.Immutable

/**
 * One label and value pair of a `MonarchKeyValue`.
 *
 * @property label the key, shown muted before the value: "Region".
 * @property value the value. `null` or empty shows a dash that TalkBack reads as "Not set".
 * @property copyable adds a copy button that copies [value]. Not shown for an empty value.
 * @property copyValue adds a copy button that copies this text instead, such as the full id behind a shortened one.
 * @property monospace draws the value in the code face, for ids, hashes and keys.
 */
@Immutable
public class MonarchKeyValueItem(
    public val label: String,
    public val value: String?,
    public val copyable: Boolean = false,
    public val copyValue: String? = null,
    public val monospace: Boolean = false,
) {
    /** The text the copy button copies, or `null` when the pair has no copy button. */
    internal val copyText: String?
        get() = when {
            value.isNullOrEmpty() -> null
            copyValue != null -> copyValue
            copyable -> value
            else -> null
        }
}
