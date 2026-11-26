package com.rookhq.monarch.components.files.editortoolbar

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchEditorToolbar`. Every
 * `null` uses the built-in string named after it in `strings_editortoolbar.xml`.
 *
 * @property toolbar the TalkBack name of the toolbar: "Formatting".
 * @property tools names of tools by tool, such as "Fett" for [MonarchEditorTool.Bold]. A heading's built-in
 * name is "Heading 2"; a custom tool uses its title.
 * @property textStyle the TalkBack name of the text style menu, and its label when no style is active:
 * "Text style".
 * @property linkTitle the title of the link form: "Link".
 * @property linkAddress the label of the address field: "Address".
 * @property linkApply the button that applies the link: "Apply".
 * @property linkRemove the button that removes the link: "Remove link".
 * @property linkCancel the button that closes the form: "Cancel".
 * @property linkHelp the help under the address field: "A web address or an email address".
 */
@Immutable
public class MonarchEditorToolbarLabels(
    public val toolbar: String? = null,
    public val tools: Map<MonarchEditorTool, String> = emptyMap(),
    public val textStyle: String? = null,
    public val linkTitle: String? = null,
    public val linkAddress: String? = null,
    public val linkApply: String? = null,
    public val linkRemove: String? = null,
    public val linkCancel: String? = null,
    public val linkHelp: String? = null,
)
