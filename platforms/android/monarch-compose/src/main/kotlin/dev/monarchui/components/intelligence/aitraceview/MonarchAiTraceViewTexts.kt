package dev.monarchui.components.intelligence.aitraceview

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import dev.monarchui.R
import java.util.Locale

/** Every string of a trace view, resolved from the labels and the built-in strings in one locale. */
internal class MonarchAiTraceViewTexts(
    private val labels: MonarchAiTraceViewLabels,
    private val resources: Resources,
    private val locale: Locale,
    private val currency: String,
) {
    private fun pick(override: String?, resource: Int): String = override ?: resources.getString(resource)

    val label: String = pick(labels.label, R.string.monarch_aitraceview_label)
    val filterLabel: String = pick(labels.filterLabel, R.string.monarch_aitraceview_filter_label)
    val totalDuration: String = pick(labels.totalDuration, R.string.monarch_aitraceview_total_duration)
    val totalTokens: String = pick(labels.totalTokens, R.string.monarch_aitraceview_total_tokens)
    val totalCost: String = pick(labels.totalCost, R.string.monarch_aitraceview_total_cost)
    val errors: String = pick(labels.errors, R.string.monarch_aitraceview_errors)
    val notAvailable: String = pick(labels.notAvailable, R.string.monarch_aitraceview_not_available)
    val empty: String = pick(labels.empty, R.string.monarch_aitraceview_empty)
    val noErrors: String = pick(labels.noErrors, R.string.monarch_aitraceview_no_errors)
    val closeDetails: String = pick(labels.closeDetails, R.string.monarch_aitraceview_close_details)
    val openPart: String = pick(labels.openPart, R.string.monarch_aitraceview_open_part)
    val start: String = pick(labels.start, R.string.monarch_aitraceview_start)
    val end: String = pick(labels.end, R.string.monarch_aitraceview_end)
    val duration: String = pick(labels.duration, R.string.monarch_aitraceview_duration)
    val inputTokens: String = pick(labels.inputTokens, R.string.monarch_aitraceview_input_tokens)
    val outputTokens: String = pick(labels.outputTokens, R.string.monarch_aitraceview_output_tokens)
    val model: String = pick(labels.model, R.string.monarch_aitraceview_model)
    val statusName: String = pick(labels.statusName, R.string.monarch_aitraceview_status_name)
    val kindName: String = pick(labels.kindName, R.string.monarch_aitraceview_kind_name)
    val error: String = pick(labels.error, R.string.monarch_aitraceview_error)
    val attributes: String = pick(labels.attributes, R.string.monarch_aitraceview_attributes)
    val input: String = pick(labels.input, R.string.monarch_aitraceview_input)
    val output: String = pick(labels.output, R.string.monarch_aitraceview_output)

    /** The name of a filter. */
    fun filter(filter: MonarchAiTraceFilter): String = labels.filter?.invoke(filter) ?: resources.getString(
        when (filter) {
            MonarchAiTraceFilter.All -> R.string.monarch_aitraceview_filter_all
            MonarchAiTraceFilter.Errors -> R.string.monarch_aitraceview_filter_errors
            MonarchAiTraceFilter.Slowest -> R.string.monarch_aitraceview_filter_slowest
        },
    )

    /** The name of a span kind. */
    fun kind(kind: MonarchAiTraceSpanKind): String = labels.kind?.invoke(kind) ?: resources.getString(
        when (kind) {
            MonarchAiTraceSpanKind.Model -> R.string.monarch_aitraceview_kind_model
            MonarchAiTraceSpanKind.Tool -> R.string.monarch_aitraceview_kind_tool
            MonarchAiTraceSpanKind.Retrieval -> R.string.monarch_aitraceview_kind_retrieval
            MonarchAiTraceSpanKind.Agent -> R.string.monarch_aitraceview_kind_agent
            MonarchAiTraceSpanKind.Guardrail -> R.string.monarch_aitraceview_kind_guardrail
            MonarchAiTraceSpanKind.Other -> R.string.monarch_aitraceview_kind_other
        },
    )

    /** The name of a status. */
    fun status(status: MonarchAiTraceSpanStatus): String = labels.status?.invoke(status) ?: resources.getString(
        when (status) {
            MonarchAiTraceSpanStatus.Ok -> R.string.monarch_aitraceview_status_ok
            MonarchAiTraceSpanStatus.Error -> R.string.monarch_aitraceview_status_error
            MonarchAiTraceSpanStatus.Cancelled -> R.string.monarch_aitraceview_status_cancelled
            MonarchAiTraceSpanStatus.Running -> R.string.monarch_aitraceview_status_running
        },
    )

    /** A duration in milliseconds. */
    fun duration(millis: Long): String =
        labels.formatDuration?.invoke(millis) ?: MonarchAiTraceViewFormat.duration(millis, locale, resources)

    /** An offset from the start of the run. */
    fun offset(millis: Long): String = resources.getString(R.string.monarch_aitraceview_offset, duration(millis))

    /** A count in the locale. */
    fun count(value: Long): String = MonarchAiTraceViewFormat.count(value, locale)

    /** The tokens of a span, "1,380 tokens". */
    fun tokens(value: Long): String {
        labels.tokens?.invoke(value)?.let { return it }
        val quantity = value.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
        return resources.getQuantityString(R.plurals.monarch_aitraceview_tokens, quantity, count(value))
    }

    /** A span's own cost, in its own currency or the view's. */
    fun cost(span: MonarchAiTraceSpan): String? =
        span.costMicros?.let { MonarchAiTraceViewFormat.cost(it, span.currency ?: currency, locale) }

    /** The costs of a run, joined across currencies. */
    fun costs(costs: List<Pair<String, Long>>): String = if (costs.isEmpty()) {
        notAvailable
    } else {
        costs.joinToString(resources.getString(R.string.monarch_aitraceview_cost_separator)) { (code, micros) ->
            MonarchAiTraceViewFormat.cost(micros, code, locale)
        }
    }

    /** The spoken timing of a span, "Starts at +1.4 s, takes 600 ms". */
    fun timing(startOffset: String, duration: String?): String = labels.timing?.invoke(startOffset, duration)
        ?: if (duration == null) {
            resources.getString(R.string.monarch_aitraceview_timing_running, startOffset)
        } else {
            resources.getString(R.string.monarch_aitraceview_timing, startOffset, duration)
        }

    /** One fact as TalkBack reads it, "Total time, 4.2 s". */
    val fact: (name: String, value: String) -> String = { name, value ->
        resources.getString(R.string.monarch_aitraceview_fact, name, value)
    }

    /** The spoken name of a span's open or close button. */
    val toggle: (name: String, open: Boolean) -> String = { name, open ->
        if (open) {
            labels.collapse?.invoke(name) ?: resources.getString(R.string.monarch_aitraceview_collapse, name)
        } else {
            labels.expand?.invoke(name) ?: resources.getString(R.string.monarch_aitraceview_expand, name)
        }
    }
}

/** Resolves [labels] against the built-in strings, again when the resources, locale or currency change. */
@Composable
internal fun rememberMonarchAiTraceViewTexts(
    labels: MonarchAiTraceViewLabels,
    locale: Locale,
    currency: String,
): MonarchAiTraceViewTexts {
    val resources = LocalResources.current
    return remember(labels, resources, locale, currency) {
        MonarchAiTraceViewTexts(labels, resources, locale, currency)
    }
}
