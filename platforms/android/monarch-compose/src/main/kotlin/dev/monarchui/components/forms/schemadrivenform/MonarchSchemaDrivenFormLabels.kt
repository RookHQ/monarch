package dev.monarchui.components.forms.schemadrivenform

import androidx.compose.runtime.Immutable
import dev.monarchui.components.forms.formactionsbar.MonarchFormActionsBarLabels
import dev.monarchui.components.workflow.fieldrenderer.MonarchFieldRendererLabels

/**
 * Replacements for the built-in strings of a `MonarchSchemaDrivenForm`. Every `null` keeps the built-in string
 * `monarch_schemadrivenform_<key>`.
 *
 * @property submit the submit button: "Save".
 * @property submitFailed shown when `onSubmit` throws an error without a message.
 * @property empty what TalkBack reads for an unanswered question in the read-only view, which shows a dash.
 * @property fields the strings of the questions: the validation messages, Yes and No, and the pickers.
 * @property actionsBar the strings of the actions bar shown with `dirty`.
 */
@Immutable
public class MonarchSchemaDrivenFormLabels(
    public val submit: String? = null,
    public val submitFailed: String? = null,
    public val empty: String? = null,
    public val fields: MonarchFieldRendererLabels = MonarchFieldRendererLabels(),
    public val actionsBar: MonarchFormActionsBarLabels = MonarchFormActionsBarLabels(),
)
