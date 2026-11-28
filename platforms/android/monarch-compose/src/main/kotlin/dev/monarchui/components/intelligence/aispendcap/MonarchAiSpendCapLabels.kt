package dev.monarchui.components.intelligence.aispendcap

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiSpendCap`. Every
 * `null`, and every function that returns `null`, keeps the built-in string
 * `monarch_aispendcap_<key>`. Amounts and percentages arrive already formatted.
 *
 * @property title the heading, which also names the card, "AI spending limit".
 * @property period the period in running text, such as "this month".
 * @property state the state badge text, such as "Limit reached".
 * @property spentOfLimit the amount line from the spent amount, the limit and the period:
 * "£12.40 of £50.00 this month".
 * @property spentNoLimit the amount line without a limit, from the spent amount and the period.
 * @property meterLabel the TalkBack name of the bar, "Spent against the limit".
 * @property remaining what is left, from an amount: "£37.60 left".
 * @property over how far spending is past the limit, from an amount.
 * @property warnsAt when the warning starts, from a percentage: "Warns at 80%".
 * @property behaviorSummary one line on what happens at the limit.
 * @property resets when the period starts again, from a formatted date.
 * @property breakdownLabel the caption of the breakdown, "Where it was spent".
 * @property edit the button that opens the form when a limit is set, "Edit limit".
 * @property setLimit the button that opens the form when there is no limit, "Set a limit".
 * @property pausedTitle the title of the notice shown when AI is paused at the limit.
 * @property pausedBody the body of that notice, from the limit and the period.
 * @property raiseLimit the notice action that opens the form, "Raise limit".
 * @property requestIncrease the action for people who cannot edit, "Request a higher limit".
 * @property readOnly who can change the limit, shown to people who cannot.
 * @property amountLabel the label of the amount field, "Limit".
 * @property amountHint the help under the amount field, from the spent amount and the period.
 * @property errorRequired the error for an empty amount.
 * @property errorPositive the error for an amount of zero or less.
 * @property errorMax the error for an amount above the highest allowed, from that amount.
 * @property belowSpent the warning for a limit below what is spent, from the spent amount, the period and the
 * chosen behavior.
 * @property warnAtLabel the label of the warn-at choice, "Warn me at".
 * @property warnAtOption one warn-at choice, from a percentage: "80% of the limit".
 * @property behaviorLabel the label of the behaviour choice, "When the limit is reached".
 * @property behaviorOption the name of a behaviour choice.
 * @property behaviorDescription what a behaviour choice means.
 * @property save the save button, "Save".
 * @property saveAnyway the save button after the below-spent warning, "Save anyway".
 * @property cancel the button that closes the form, "Cancel".
 * @property removeLimit the button that starts removing the limit and confirms it, "Remove limit".
 * @property removeConfirmTitle the title of the dialog that confirms the removal.
 * @property removeConfirmBody what removing the limit means.
 * @property removeCancel the dialog button that keeps the limit, "Keep limit".
 * @property announce the announcement of a new state, from the title, the state and the amount line.
 */
@Immutable
public class MonarchAiSpendCapLabels(
    public val title: String? = null,
    public val period: ((MonarchAiSpendCapPeriod) -> String?)? = null,
    public val state: ((MonarchAiSpendCapState) -> String?)? = null,
    public val spentOfLimit: ((spent: String, limit: String, period: String) -> String)? = null,
    public val spentNoLimit: ((spent: String, period: String) -> String)? = null,
    public val meterLabel: String? = null,
    public val remaining: ((amount: String) -> String)? = null,
    public val over: ((amount: String) -> String)? = null,
    public val warnsAt: ((percent: String) -> String)? = null,
    public val behaviorSummary: ((MonarchAiSpendCapBehavior) -> String?)? = null,
    public val resets: ((date: String) -> String)? = null,
    public val breakdownLabel: String? = null,
    public val edit: String? = null,
    public val setLimit: String? = null,
    public val pausedTitle: String? = null,
    public val pausedBody: ((limit: String, period: String) -> String)? = null,
    public val raiseLimit: String? = null,
    public val requestIncrease: String? = null,
    public val readOnly: String? = null,
    public val amountLabel: String? = null,
    public val amountHint: ((spent: String, period: String) -> String)? = null,
    public val errorRequired: String? = null,
    public val errorPositive: String? = null,
    public val errorMax: ((max: String) -> String)? = null,
    public val belowSpent: ((spent: String, period: String, behavior: MonarchAiSpendCapBehavior) -> String)? = null,
    public val warnAtLabel: String? = null,
    public val warnAtOption: ((percent: String) -> String)? = null,
    public val behaviorLabel: String? = null,
    public val behaviorOption: ((MonarchAiSpendCapBehavior) -> String?)? = null,
    public val behaviorDescription: ((MonarchAiSpendCapBehavior) -> String?)? = null,
    public val save: String? = null,
    public val saveAnyway: String? = null,
    public val cancel: String? = null,
    public val removeLimit: String? = null,
    public val removeConfirmTitle: String? = null,
    public val removeConfirmBody: String? = null,
    public val removeCancel: String? = null,
    public val announce: ((title: String, state: String, amount: String) -> String)? = null,
)
