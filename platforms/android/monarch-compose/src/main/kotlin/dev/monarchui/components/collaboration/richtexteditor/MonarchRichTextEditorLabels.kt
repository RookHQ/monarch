package dev.monarchui.components.collaboration.richtexteditor

import androidx.compose.runtime.Immutable
import dev.monarchui.components.collaboration.mentionpicker.MonarchMentionPickerLabels
import dev.monarchui.components.files.editortoolbar.MonarchEditorToolbarLabels

/**
 * Replacements for the built-in strings of a `MonarchRichTextEditor`. Every
 * `null` keeps the built-in string `monarch_richtexteditor_<key>`.
 *
 * @property count the visible and spoken count without a limit, given the characters: "12 characters".
 * @property countOfLimit the visible count with a limit, given the characters and the limit: "12 / 2,000".
 * @property remaining the spoken count with a limit, given the characters left: "1,988 characters left".
 * @property over the spoken count and the field's error past the limit, given the characters over:
 * "3 characters over the limit".
 * @property toolbar the labels of the formatting toolbar.
 * @property mentions the labels of the mention suggestions.
 */
@Immutable
public class MonarchRichTextEditorLabels(
    public val count: ((Int) -> String)? = null,
    public val countOfLimit: ((Int, Int) -> String)? = null,
    public val remaining: ((Int) -> String)? = null,
    public val over: ((Int) -> String)? = null,
    public val toolbar: MonarchEditorToolbarLabels = MonarchEditorToolbarLabels(),
    public val mentions: MonarchMentionPickerLabels = MonarchMentionPickerLabels(),
)
