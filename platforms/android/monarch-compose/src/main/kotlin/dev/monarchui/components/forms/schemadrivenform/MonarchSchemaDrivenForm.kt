package dev.monarchui.components.forms.schemadrivenform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalConfiguration
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionItem
import dev.monarchui.components.datadisplay.descriptionlist.MonarchDescriptionList
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.forms.formactionsbar.MonarchFormActionsBar
import dev.monarchui.components.workflow.fieldrenderer.MonarchFieldRenderer
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldProblem
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldType
import dev.monarchui.components.workflow.fieldrenderer.MonarchFormFieldValue
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.monarchString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * A form generated from a schema: every question of [fields] rendered with the right Monarch input, with
 * sections, conditional questions, validation and a submit button that shows progress and errors.
 *
 * Built by Monarch as a `LazyColumn` of `MonarchFieldRenderer`s, so a long form scrolls itself and composes only
 * the questions on screen. A [MonarchFormFieldType.Section] question is a heading that starts a section; when
 * its conditions do not hold, its questions are hidden with it, and hidden questions are neither validated nor
 * submitted ([MonarchSchemaDrivenFormDefaults]). Errors come from `MonarchFormFieldDefinition.problem` and show
 * when [validateOn] says; [errors], such as the server's, show at once. Submitting validates every shown
 * question: an invalid answer scrolls the form to the first invalid question and moves focus into it;
 * otherwise [onSubmit] receives the shown answers while the button shows progress, and a thrown error is shown
 * in an assertive `MonarchBanner`. With [dirty], the actions are a `MonarchFormActionsBar` pinned under the
 * questions instead, for edit forms that save changes. With [readOnly], the answers are a
 * `MonarchDescriptionList`.
 *
 * The form holds no answers: [values] and [onValuesChange] are the app's. Give the form a bounded height, as
 * for any lazy list.
 *
 * @param fields the questions and section headings, in order.
 * @param values the answers, keyed by question id; a missing key is no answer.
 * @param onValuesChange called with all answers and the id of the question that changed.
 * @param modifier the modifier of the form.
 * @param onSubmit receives the shown answers once they are valid; shows the submit button, or saves from the
 * actions bar with [dirty]. Throw to show the error's message.
 * @param validateOn when a question's own error first shows.
 * @param enabled whether the answers can be changed and the form submitted.
 * @param readOnly shows the answers as text instead of inputs.
 * @param dirty `null` puts a submit button after the questions; a value pins a `MonarchFormActionsBar` under them
 * that offers Save while the answers differ from the saved ones.
 * @param errors error messages from the app, such as the server's, keyed by question id; shown at once and in
 * place of the question's own error.
 * @param state the scroll state of the questions.
 * @param contentPadding the padding around the questions.
 * @param locale the locale of numbers and dates in the read-only view. `null` uses the first locale of the
 * configuration.
 * @param labels replacements for the built-in strings, including the questions' and the actions bar's.
 * @param yesNoAsSwitch draws yes-or-no questions as switch rows, as the web does.
 * @param onDiscard with [dirty], offers Discard in the actions bar, confirmed first.
 * @param secondaryActions further buttons after the submit button, such as Cancel.
 */
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchSchemaDrivenForm(
    fields: List<MonarchFormFieldDefinition>,
    values: Map<String, MonarchFormFieldValue?>,
    onValuesChange: (values: Map<String, MonarchFormFieldValue?>, fieldId: String) -> Unit,
    modifier: Modifier = Modifier,
    onSubmit: (suspend (values: Map<String, MonarchFormFieldValue?>) -> Unit)? = null,
    validateOn: MonarchSchemaDrivenFormValidateOn = MonarchSchemaDrivenFormValidateOn.Blur,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    dirty: Boolean? = null,
    errors: Map<String, String> = emptyMap(),
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    locale: Locale? = null,
    labels: MonarchSchemaDrivenFormLabels = MonarchSchemaDrivenFormLabels(),
    yesNoAsSwitch: Boolean = false,
    onDiscard: (() -> Unit)? = null,
    secondaryActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val tokens = MonarchSchemaDrivenFormTokens
    val defaults = MonarchSchemaDrivenFormDefaults
    val shown = defaults.visibleFields(fields, values)
    if (readOnly) {
        MonarchSchemaDrivenFormView(shown, values, locale ?: LocalConfiguration.current.locales[0], labels, modifier)
        return
    }
    val scope = rememberCoroutineScope()
    var visited by remember { mutableStateOf(emptySet<String>()) }
    var submitted by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val problems = defaults.problems(fields, values)
    val failedText = monarchString(labels.submitFailed, R.string.monarch_schemadrivenform_submit_failed)

    val submit: () -> Unit = submit@{
        val send = onSubmit ?: return@submit
        if (pending || !enabled) return@submit
        submitted = true
        failure = null
        val firstInvalid = shown.indexOfFirst { it.id in problems }
        if (firstInvalid >= 0) {
            scope.launch {
                state.scrollToItem(firstInvalid)
                withFrameNanos { }
                requesters[shown[firstInvalid].id]?.requestFocus()
            }
            return@submit
        }
        pending = true
        val answers = defaults.submittedValues(fields, values)
        scope.launch {
            val error = try {
                monarchSchemaDrivenFormAttempt { send(answers) }
            } finally {
                pending = false
            }
            failure = error?.let { tokens.failureText(it, failedText) }
        }
    }

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
            state = state,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(tokens.FieldGap),
        ) {
            items(shown, key = { it.id }) { field ->
                MonarchSchemaDrivenFormQuestion(
                    field = field,
                    value = values[field.id],
                    onValueChange = { value ->
                        if (!tokens.visitedOnFocusLoss(field.type)) visited = visited + field.id
                        onValuesChange(values + (field.id to value), field.id)
                    },
                    onLeave = { visited = visited + field.id },
                    requesters = requesters,
                    enabled = enabled && !pending,
                    showProblem = tokens.showsProblem(validateOn, submitted, field.id in visited),
                    problem = problems[field.id],
                    errorText = errors[field.id],
                    labels = labels,
                    yesNoAsSwitch = yesNoAsSwitch,
                )
            }
            if (dirty == null) {
                failure?.let { message ->
                    item(key = "\u0000failure") {
                        MonarchBanner(
                            message = message,
                            tone = MonarchTone.Danger,
                            announcement = MonarchBannerAnnouncement.Assertive,
                        )
                    }
                }
                if (onSubmit != null || secondaryActions != null) {
                    item(key = "\u0000actions") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                            verticalArrangement = Arrangement.spacedBy(tokens.ActionGap),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (onSubmit != null) {
                                MonarchButton(
                                    text = monarchString(labels.submit, R.string.monarch_schemadrivenform_submit),
                                    onClick = submit,
                                    variant = MonarchButtonVariant.Primary,
                                    enabled = enabled,
                                    loading = pending,
                                )
                            }
                            secondaryActions?.invoke(this)
                        }
                    }
                }
            }
        }
        if (dirty != null && onSubmit != null) {
            MonarchFormActionsBar(
                dirty = dirty,
                onSave = submit,
                onDiscard = onDiscard,
                pending = pending,
                error = failure,
                labels = labels.actionsBar,
            )
        }
    }
}

/** One question, which counts as visited when focus leaves it, and takes focus after a failed submit. */
@Suppress("LongParameterList")
@Composable
private fun MonarchSchemaDrivenFormQuestion(
    field: MonarchFormFieldDefinition,
    value: MonarchFormFieldValue?,
    onValueChange: (MonarchFormFieldValue?) -> Unit,
    onLeave: () -> Unit,
    requesters: MutableMap<String, FocusRequester>,
    enabled: Boolean,
    showProblem: Boolean,
    problem: MonarchFormFieldProblem?,
    errorText: String?,
    labels: MonarchSchemaDrivenFormLabels,
    yesNoAsSwitch: Boolean,
) {
    val requester = remember { FocusRequester() }
    DisposableEffect(field.id, requester) {
        requesters[field.id] = requester
        onDispose { if (requesters[field.id] === requester) requesters.remove(field.id) }
    }
    var focused by remember { mutableStateOf(false) }
    MonarchFieldRenderer(
        field = field,
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged { focus ->
                if (focused && !focus.hasFocus && MonarchSchemaDrivenFormTokens.visitedOnFocusLoss(field.type)) {
                    onLeave()
                }
                focused = focus.hasFocus
            },
        enabled = enabled,
        problem = if (showProblem) problem else null,
        errorText = errorText,
        labels = labels.fields,
        yesNoAsSwitch = yesNoAsSwitch,
    )
}

/** The read-only view: the shown answers as a `MonarchDescriptionList`, in a `LazyColumn` like the form. */
@Composable
private fun MonarchSchemaDrivenFormView(
    shown: List<MonarchFormFieldDefinition>,
    values: Map<String, MonarchFormFieldValue?>,
    locale: Locale,
    labels: MonarchSchemaDrivenFormLabels,
    modifier: Modifier,
) {
    val yes = monarchString(labels.fields.yes, R.string.monarch_fieldrenderer_yes)
    val no = monarchString(labels.fields.no, R.string.monarch_fieldrenderer_no)
    val items = shown.filter { it.type != MonarchFormFieldType.Section }.map { field ->
        MonarchDescriptionItem(
            term = field.label,
            value = MonarchSchemaDrivenFormTokens.display(field, values[field.id], locale, yes, no),
        )
    }
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        item { MonarchDescriptionList(items = items, emptyLabel = labels.empty) }
    }
}

/** Runs [work]; the error it threw, other than a cancellation, or `null` when it succeeded. */
@Suppress("TooGenericExceptionCaught")
private suspend fun monarchSchemaDrivenFormAttempt(work: suspend () -> Unit): Exception? = try {
    work()
    null
} catch (cancel: CancellationException) {
    throw cancel
} catch (error: Exception) {
    error
}
