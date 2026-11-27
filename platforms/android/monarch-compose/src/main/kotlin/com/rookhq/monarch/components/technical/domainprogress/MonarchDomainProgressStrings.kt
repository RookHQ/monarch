package com.rookhq.monarch.components.technical.domainprogress

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.rookhq.monarch.R

/** The strings of one MonarchDomainProgress, with the caller's replacements applied. */
internal class MonarchDomainProgressStrings(
    private val labels: MonarchDomainProgressLabels,
    private val resources: Resources,
) {
    /** The title of [step] in [state] at [stage]. */
    fun title(step: MonarchDomainStep, state: MonarchDomainStepState, stage: MonarchDomainStage): String =
        labels.title?.invoke(step, state, stage) ?: resources.getString(
            when (step) {
                MonarchDomainStep.Dns -> when {
                    state == MonarchDomainStepState.Done -> R.string.monarch_domainprogress_dns_title_done
                    stage == MonarchDomainStage.Ownership -> R.string.monarch_domainprogress_dns_title_ownership
                    else -> R.string.monarch_domainprogress_dns_title_waiting
                }
                MonarchDomainStep.Certificate -> if (state == MonarchDomainStepState.Done) {
                    R.string.monarch_domainprogress_certificate_title_done
                } else {
                    R.string.monarch_domainprogress_certificate_title
                }
                MonarchDomainStep.Live -> R.string.monarch_domainprogress_live_title
            },
        )

    /** The detail of [step] in [state]. */
    fun detail(step: MonarchDomainStep, state: MonarchDomainStepState, host: String): String =
        labels.detail?.invoke(step, state, host) ?: when (step) {
            MonarchDomainStep.Dns -> resources.getString(
                if (state == MonarchDomainStepState.Done) {
                    R.string.monarch_domainprogress_dns_detail_done
                } else {
                    R.string.monarch_domainprogress_dns_detail_waiting
                },
            )
            MonarchDomainStep.Certificate -> resources.getString(
                when (state) {
                    MonarchDomainStepState.Done -> R.string.monarch_domainprogress_certificate_detail_done
                    MonarchDomainStepState.Todo -> R.string.monarch_domainprogress_certificate_detail_todo
                    else -> R.string.monarch_domainprogress_certificate_detail_current
                },
            )
            MonarchDomainStep.Live -> resources.getString(
                when (state) {
                    MonarchDomainStepState.Done -> R.string.monarch_domainprogress_live_detail_done
                    MonarchDomainStepState.Current -> R.string.monarch_domainprogress_live_detail_current
                    else -> R.string.monarch_domainprogress_live_detail_todo
                },
                host,
            )
        }

    /** The spoken name of [value]. */
    fun state(value: MonarchDomainStepState): String = labels.state?.invoke(value) ?: resources.getString(
        when (value) {
            MonarchDomainStepState.Done -> R.string.monarch_domainprogress_state_done
            MonarchDomainStepState.Current -> R.string.monarch_domainprogress_state_current
            MonarchDomainStepState.Error -> R.string.monarch_domainprogress_state_error
            MonarchDomainStepState.Todo -> R.string.monarch_domainprogress_state_todo
        },
    )

    /** The spoken name of the steps. */
    fun listLabel(host: String): String =
        labels.listLabel?.invoke(host) ?: resources.getString(R.string.monarch_domainprogress_list_label, host)

    /** The announcement of the active step. */
    fun announcement(position: Int, count: Int, title: String, state: String): String =
        labels.announcement?.invoke(position, count, title, state)
            ?: resources.getString(R.string.monarch_domainprogress_announcement, position, count, title, state)

    /** The DNS line. */
    fun dnsSeen(host: String, seen: String): String =
        labels.dnsSeen?.invoke(host, seen) ?: resources.getString(R.string.monarch_domainprogress_dns_seen, host, seen)

    /** Used when public DNS shows no record. */
    val noRecord: String get() = labels.noRecord ?: resources.getString(R.string.monarch_domainprogress_no_record)

    /** The "keeps checking" sentence. */
    fun rechecking(notify: MonarchDomainNotify?): String = labels.rechecking?.invoke(notify) ?: resources.getString(
        when (notify) {
            MonarchDomainNotify.Email -> R.string.monarch_domainprogress_rechecking_email
            MonarchDomainNotify.InApp -> R.string.monarch_domainprogress_rechecking_in_app
            null -> R.string.monarch_domainprogress_rechecking
        },
    )

    /** The last check line. */
    fun checked(time: String): String =
        labels.checked?.invoke(time) ?: resources.getString(R.string.monarch_domainprogress_checked, time)

    /** The three steps at [stage] with [problems], with their states and text. */
    fun items(host: String, stage: MonarchDomainStage, problems: List<MonarchDomainProblem>):
        List<MonarchDomainProgressItem> {
        val states = MonarchDomainProgressRules.states(stage, problems)
        return MonarchDomainProgressRules.Steps.map { step ->
            val state = states.getValue(step)
            MonarchDomainProgressItem(step, state, title(step, state, stage), detail(step, state, host))
        }
    }

    /** The announcement of the active step of [items]. */
    fun announcement(items: List<MonarchDomainProgressItem>): String {
        if (items.isEmpty()) return ""
        val index = MonarchDomainProgressRules.activeIndex(items)
        val item = items[index]
        return announcement(index + 1, items.size, item.title, state(item.state))
    }
}

/** The strings of a domain progress, from [labels] and the built-in resources. */
@Composable
internal fun monarchDomainProgressStrings(labels: MonarchDomainProgressLabels): MonarchDomainProgressStrings =
    MonarchDomainProgressStrings(labels, LocalResources.current)
