package dev.monarchui.components.identity.recoverycodes

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchRecoveryCodes`. Every
 * `null` keeps the built-in string `monarch_recoverycodes_<key>`.
 *
 * @property listLabel the name TalkBack reads for the codes, "Recovery codes".
 * @property remaining the line above the codes when some are used, from the codes left and the total:
 * "8 of 10 codes left".
 * @property used the state TalkBack reads for a used code, "Used".
 * @property copy the copy button, "Copy codes".
 * @property copied the copy button after a copy, "Copied".
 * @property save the save button, "Save or share".
 * @property reveal the button that shows hidden codes, "Show codes".
 * @property conceal the button that hides the codes, "Hide codes".
 * @property concealedHint shown while the codes are hidden.
 * @property regenerate the button that replaces the codes, "Generate new codes".
 * @property regenerateTitle the title of the confirmation, "Generate new recovery codes?".
 * @property regenerateDescription the text of the confirmation.
 * @property regenerateConfirm the confirming button, "Generate new codes".
 * @property cancel the cancelling button, "Cancel".
 * @property regenerated announced after new codes arrive.
 * @property acknowledge the checkbox, "I have saved my recovery codes".
 * @property acknowledgeMissing shown and announced when Continue is pressed without the checkbox.
 * @property continueTitle the button that finishes the step, "Continue".
 * @property download the button that saves the codes to a text file, "Download", as on the web.
 * @property downloaded announced after the file is written, "Recovery codes saved to a file.".
 * @property downloadFailed shown and announced when the file cannot be written.
 */
@Immutable
public class MonarchRecoveryCodesLabels(
    public val listLabel: String? = null,
    public val remaining: ((left: Int, total: Int) -> String)? = null,
    public val used: String? = null,
    public val copy: String? = null,
    public val copied: String? = null,
    public val save: String? = null,
    public val reveal: String? = null,
    public val conceal: String? = null,
    public val concealedHint: String? = null,
    public val regenerate: String? = null,
    public val regenerateTitle: String? = null,
    public val regenerateDescription: String? = null,
    public val regenerateConfirm: String? = null,
    public val cancel: String? = null,
    public val regenerated: String? = null,
    public val acknowledge: String? = null,
    public val acknowledgeMissing: String? = null,
    public val continueTitle: String? = null,
    public val download: String? = null,
    public val downloaded: String? = null,
    public val downloadFailed: String? = null,
)
