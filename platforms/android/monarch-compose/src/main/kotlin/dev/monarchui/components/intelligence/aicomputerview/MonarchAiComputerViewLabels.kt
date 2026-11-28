package dev.monarchui.components.intelligence.aicomputerview

import androidx.compose.runtime.Immutable

/**
 * Replacements for the strings `MonarchAiComputerView` supplies. Each `null`
 * keeps the built-in English string `monarch_aicomputerview_<key>`. Strings
 * that name the surface take it, so a translation can say "browser" or
 * "computer" in its own words.
 *
 * @property label the TalkBack name of the view. Default "Assistant browser view".
 * @property status the status text, such as "Working" or "Waiting for you".
 * @property agentInControl the banner while the agent acts. Default "The assistant is controlling this browser".
 * @property youInControl the banner while the person has taken over. Default "You are controlling this browser".
 * @property waitingTitle the title of the notice while the agent waits for the person.
 * @property waitingFallback the reason shown while waiting when the app gives none.
 * @property pause the Pause button.
 * @property resume the Resume button.
 * @property takeOver the Take over button.
 * @property handBack the Hand back button.
 * @property stop the Stop button.
 * @property stopQuestion the question of the stop confirmation.
 * @property stopConfirm the button that confirms stopping. Default "Stop now".
 * @property stopCancel the button that closes the confirmation. Default "Keep going".
 * @property log the heading of the action log. Default "Actions".
 * @property logToggle the compact log button from the number of actions. Default "Actions (12)".
 * @property empty shown when there are no actions yet.
 * @property kind the name of an action kind, read and shown before each action.
 * @property hidden shown in place of a withheld detail.
 * @property sensitiveLabel shown and announced in place of a private action's label.
 * @property noFrame shown before the first screenshot.
 * @property frameUnavailable shown when the screenshot has no picture.
 * @property frameHidden shown when the screenshot may show private information.
 * @property frameAlt the screenshot's text alternative when the frame has none.
 * @property liveAlt the live picture's text alternative when the frame has none.
 * @property capturedAt the caption under the screenshot from the formatted time. Default "Screenshot from 10:02 AM".
 * @property live the caption under the live picture. Default "Live".
 * @property address read before the page address. Default "Address".
 * @property viewLarger the action of the screenshot that opens it full screen. Default "View larger".
 * @property frameDescription the screen's text alternative from the picture's text and the latest action label.
 * Default "Screenshot of the browser the assistant is using. Latest action: Clicked Sign in".
 * @property expanded the state of the compact log button while the log shows. Default "Expanded".
 * @property collapsed the state of the compact log button while the log is hidden. Default "Collapsed".
 * @property showEarlier the button that shows older actions, from their number.
 * @property announceStatus announced when the status changes, from the status text.
 * @property announceActions announced for new actions, from their number and the latest label.
 * @property announceTakenOver announced when the person takes control.
 * @property announceReleased announced when the agent has control again.
 */
@Immutable
public class MonarchAiComputerViewLabels(
    public val label: ((MonarchAiComputerViewSurface) -> String)? = null,
    public val status: ((MonarchAiComputerViewStatus) -> String)? = null,
    public val agentInControl: ((MonarchAiComputerViewSurface) -> String)? = null,
    public val youInControl: ((MonarchAiComputerViewSurface) -> String)? = null,
    public val waitingTitle: String? = null,
    public val waitingFallback: String? = null,
    public val pause: String? = null,
    public val resume: String? = null,
    public val takeOver: String? = null,
    public val handBack: String? = null,
    public val stop: String? = null,
    public val stopQuestion: String? = null,
    public val stopConfirm: String? = null,
    public val stopCancel: String? = null,
    public val log: String? = null,
    public val logToggle: ((count: Int) -> String)? = null,
    public val empty: String? = null,
    public val kind: ((MonarchAiComputerActionKind) -> String)? = null,
    public val hidden: String? = null,
    public val sensitiveLabel: String? = null,
    public val noFrame: String? = null,
    public val frameUnavailable: String? = null,
    public val frameHidden: String? = null,
    public val frameAlt: ((MonarchAiComputerViewSurface) -> String)? = null,
    public val liveAlt: ((MonarchAiComputerViewSurface) -> String)? = null,
    public val capturedAt: ((time: String) -> String)? = null,
    public val live: String? = null,
    public val address: String? = null,
    public val viewLarger: String? = null,
    public val frameDescription: ((alt: String, latest: String) -> String)? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
    public val showEarlier: ((count: Int) -> String)? = null,
    public val announceStatus: ((status: String) -> String)? = null,
    public val announceActions: ((count: Int, latest: String) -> String)? = null,
    public val announceTakenOver: ((MonarchAiComputerViewSurface) -> String)? = null,
    public val announceReleased: ((MonarchAiComputerViewSurface) -> String)? = null,
) {
    private val parts: List<Any?>
        get() = listOf(
            label, status, agentInControl, youInControl, waitingTitle, waitingFallback, pause, resume, takeOver,
            handBack, stop, stopQuestion, stopConfirm, stopCancel, log, logToggle, empty, kind, hidden,
            sensitiveLabel, noFrame, frameUnavailable, frameHidden, frameAlt, liveAlt, capturedAt, live, address,
            viewLarger, frameDescription, expanded, collapsed, showEarlier, announceStatus, announceActions,
            announceTakenOver, announceReleased,
        )

    override fun equals(other: Any?): Boolean = other is MonarchAiComputerViewLabels && other.parts == parts

    override fun hashCode(): Int = parts.hashCode()
}
