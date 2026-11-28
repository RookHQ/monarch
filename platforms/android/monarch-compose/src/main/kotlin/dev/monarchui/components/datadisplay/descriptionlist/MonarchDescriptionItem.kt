package dev.monarchui.components.datadisplay.descriptionlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * One fact of a `MonarchDescriptionList`: a term and its value.
 *
 * @property term what the fact is, such as "Owner". Already localised.
 * @property value the value as text. `null` or an empty string shows a dash, read as "Not set".
 * @property copyable adds a copy button after the value that copies [value].
 * @property copyValue copies this text instead of [value], for example the full id behind a shortened one.
 * Given, the copy button is shown even without [copyable].
 * @property maxLines limits the value to this many lines, with "Show more" when the text is cut.
 * `null` shows the whole value.
 * @property wide makes the item take a whole row of a list with several columns.
 * @property content shows this instead of the [value] text, for example a badge. The value text, when
 * given, is still what is copied.
 */
@Immutable
public class MonarchDescriptionItem(
    public val term: String,
    public val value: String?,
    public val copyable: Boolean = false,
    public val copyValue: String? = null,
    public val maxLines: Int? = null,
    public val wide: Boolean = false,
    public val content: (@Composable () -> Unit)? = null,
)
