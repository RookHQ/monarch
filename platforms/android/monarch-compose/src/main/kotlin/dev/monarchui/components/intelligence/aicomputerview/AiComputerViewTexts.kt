package dev.monarchui.components.intelligence.aicomputerview

import android.content.res.Resources
import dev.monarchui.R

/** The strings of MonarchAiComputerView: the app's [labels] where given, the built-in resources otherwise. */
internal class AiComputerViewTexts(
    private val labels: MonarchAiComputerViewLabels,
    private val resources: Resources,
    private val surface: MonarchAiComputerViewSurface,
) {
    private val browser = surface == MonarchAiComputerViewSurface.Browser

    private fun pick(override: String?, id: Int): String = override ?: resources.getString(id)

    private fun bySurface(override: ((MonarchAiComputerViewSurface) -> String)?, browserId: Int, computerId: Int) =
        override?.invoke(surface) ?: resources.getString(if (browser) browserId else computerId)

    val label: String
        get() = bySurface(
            labels.label,
            R.string.monarch_aicomputerview_label_browser,
            R.string.monarch_aicomputerview_label_computer,
        )
    val agentInControl: String
        get() = bySurface(
            labels.agentInControl,
            R.string.monarch_aicomputerview_agent_in_control_browser,
            R.string.monarch_aicomputerview_agent_in_control_computer,
        )
    val youInControl: String
        get() = bySurface(
            labels.youInControl,
            R.string.monarch_aicomputerview_you_in_control_browser,
            R.string.monarch_aicomputerview_you_in_control_computer,
        )
    val frameAlt: String
        get() = bySurface(
            labels.frameAlt,
            R.string.monarch_aicomputerview_frame_alt_browser,
            R.string.monarch_aicomputerview_frame_alt_computer,
        )
    val liveAlt: String
        get() = bySurface(
            labels.liveAlt,
            R.string.monarch_aicomputerview_live_alt_browser,
            R.string.monarch_aicomputerview_live_alt_computer,
        )
    val announceTakenOver: String
        get() = bySurface(
            labels.announceTakenOver,
            R.string.monarch_aicomputerview_announce_taken_over_browser,
            R.string.monarch_aicomputerview_announce_taken_over_computer,
        )
    val announceReleased: String
        get() = bySurface(
            labels.announceReleased,
            R.string.monarch_aicomputerview_announce_released_browser,
            R.string.monarch_aicomputerview_announce_released_computer,
        )
    val waitingTitle: String get() = pick(labels.waitingTitle, R.string.monarch_aicomputerview_waiting_title)
    val waitingFallback: String get() = pick(labels.waitingFallback, R.string.monarch_aicomputerview_waiting_fallback)
    val pause: String get() = pick(labels.pause, R.string.monarch_aicomputerview_pause)
    val resume: String get() = pick(labels.resume, R.string.monarch_aicomputerview_resume)
    val takeOver: String get() = pick(labels.takeOver, R.string.monarch_aicomputerview_take_over)
    val handBack: String get() = pick(labels.handBack, R.string.monarch_aicomputerview_hand_back)
    val stop: String get() = pick(labels.stop, R.string.monarch_aicomputerview_stop)
    val stopQuestion: String get() = pick(labels.stopQuestion, R.string.monarch_aicomputerview_stop_question)
    val stopConfirm: String get() = pick(labels.stopConfirm, R.string.monarch_aicomputerview_stop_confirm)
    val stopCancel: String get() = pick(labels.stopCancel, R.string.monarch_aicomputerview_stop_cancel)
    val log: String get() = pick(labels.log, R.string.monarch_aicomputerview_log)
    val empty: String get() = pick(labels.empty, R.string.monarch_aicomputerview_empty)
    val hidden: String get() = pick(labels.hidden, R.string.monarch_aicomputerview_hidden)
    val sensitiveLabel: String get() = pick(labels.sensitiveLabel, R.string.monarch_aicomputerview_sensitive_label)
    val noFrame: String get() = pick(labels.noFrame, R.string.monarch_aicomputerview_no_frame)
    val frameUnavailable: String
        get() = pick(labels.frameUnavailable, R.string.monarch_aicomputerview_frame_unavailable)
    val frameHidden: String get() = pick(labels.frameHidden, R.string.monarch_aicomputerview_frame_hidden)
    val live: String get() = pick(labels.live, R.string.monarch_aicomputerview_live)
    val address: String get() = pick(labels.address, R.string.monarch_aicomputerview_address)
    val viewLarger: String get() = pick(labels.viewLarger, R.string.monarch_aicomputerview_view_larger)
    val expanded: String get() = pick(labels.expanded, R.string.monarch_aicomputerview_expanded)
    val collapsed: String get() = pick(labels.collapsed, R.string.monarch_aicomputerview_collapsed)

    fun status(status: MonarchAiComputerViewStatus): String = labels.status?.invoke(status) ?: resources.getString(
        when (status) {
            MonarchAiComputerViewStatus.Starting -> R.string.monarch_aicomputerview_status_starting
            MonarchAiComputerViewStatus.Working -> R.string.monarch_aicomputerview_status_working
            MonarchAiComputerViewStatus.Paused -> R.string.monarch_aicomputerview_status_paused
            MonarchAiComputerViewStatus.WaitingForYou -> R.string.monarch_aicomputerview_status_waiting_for_you
            MonarchAiComputerViewStatus.TakenOver -> R.string.monarch_aicomputerview_status_taken_over
            MonarchAiComputerViewStatus.Done -> R.string.monarch_aicomputerview_status_done
            MonarchAiComputerViewStatus.Failed -> R.string.monarch_aicomputerview_status_failed
            MonarchAiComputerViewStatus.Stopped -> R.string.monarch_aicomputerview_status_stopped
        },
    )

    fun kind(kind: MonarchAiComputerActionKind): String = labels.kind?.invoke(kind) ?: resources.getString(
        when (kind) {
            MonarchAiComputerActionKind.Click -> R.string.monarch_aicomputerview_kind_click
            MonarchAiComputerActionKind.Type -> R.string.monarch_aicomputerview_kind_type
            MonarchAiComputerActionKind.Scroll -> R.string.monarch_aicomputerview_kind_scroll
            MonarchAiComputerActionKind.Navigate -> R.string.monarch_aicomputerview_kind_navigate
            MonarchAiComputerActionKind.Key -> R.string.monarch_aicomputerview_kind_key
            MonarchAiComputerActionKind.Wait -> R.string.monarch_aicomputerview_kind_wait
            MonarchAiComputerActionKind.Read -> R.string.monarch_aicomputerview_kind_read
            MonarchAiComputerActionKind.Other -> R.string.monarch_aicomputerview_kind_other
        },
    )

    fun logToggle(count: Int): String =
        labels.logToggle?.invoke(count) ?: resources.getString(R.string.monarch_aicomputerview_log_toggle, count)

    fun showEarlier(count: Int): String = labels.showEarlier?.invoke(count)
        ?: resources.getQuantityString(R.plurals.monarch_aicomputerview_show_earlier, count, count)

    fun capturedAt(time: String): String =
        labels.capturedAt?.invoke(time) ?: resources.getString(R.string.monarch_aicomputerview_captured_at, time)

    fun frameDescription(alt: String, latest: String?): String = when (latest) {
        null -> alt
        else -> labels.frameDescription?.invoke(alt, latest)
            ?: resources.getString(R.string.monarch_aicomputerview_frame_description, alt, latest)
    }

    fun announceStatus(status: String): String = labels.announceStatus?.invoke(status)
        ?: resources.getString(R.string.monarch_aicomputerview_announce_status, status)

    /** One new action is announced by its label; several by their number and the latest label. */
    fun announceActions(count: Int, latest: String): String = when {
        count == 1 -> latest
        else -> labels.announceActions?.invoke(count, latest)
            ?: resources.getQuantityString(R.plurals.monarch_aicomputerview_announce_actions, count, count, latest)
    }
}
