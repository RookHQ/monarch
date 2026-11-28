package com.rookhq.monarch.components.technical.keycreateddialog

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchKeyCreatedDialog`.
 * Every `null` uses the built-in string in `strings_keycreateddialog.xml`,
 * or the copy button's own string for [copy] and [copied].
 *
 * @property title the title when the dialog's title is not given: "Copy your new key".
 * @property label what the value is when the dialog's label is not given: "API key".
 * @property secretNoticeTitle the warning's title for a secret: "Copy it now. It will not be shown again."
 * @property secretNotice the warning's message for a secret.
 * @property publicNoticeTitle the notice's title for a public value: "You can view this again later."
 * @property publicNotice the notice's message for a public value.
 * @property acknowledge writes the checkbox's label from the value's label:
 * "I have saved this API key somewhere safe."
 * @property acknowledgeRequired shown and announced when Done is tapped before the checkbox:
 * "Confirm that you have saved it before you close this dialog."
 * @property done the button that closes the dialog: "Done".
 * @property copy the copy button's label: "Copy".
 * @property copied the copy button's label just after a copy: "Copied".
 */
@Immutable
public class MonarchKeyCreatedDialogLabels(
    public val title: String? = null,
    public val label: String? = null,
    public val secretNoticeTitle: String? = null,
    public val secretNotice: String? = null,
    public val publicNoticeTitle: String? = null,
    public val publicNotice: String? = null,
    public val acknowledge: ((label: String) -> String)? = null,
    public val acknowledgeRequired: String? = null,
    public val done: String? = null,
    public val copy: String? = null,
    public val copied: String? = null,
)
