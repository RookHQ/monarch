package com.rookhq.monarch.components.workflow.runningtimer

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchRunningTimer`. Every
 * `null` keeps the built-in string `monarch_runningtimer_<key>`.
 *
 * @property start the panel's start button, "Start timer".
 * @property stop the stop button, "Stop".
 * @property stopAndLog what TalkBack reads for the indicator's stop button, "Stop timer and log time".
 * @property more the visible text of the indicator's menu button when no menu icon is given, "More".
 * @property moreActions what TalkBack reads for the indicator's menu button, "More timer actions".
 * @property discard the discard action, "Discard without logging".
 * @property discardTitle the title of the discard confirmation, "Discard the running timer?".
 * @property discardMessage the message of the discard confirmation, "The tracked time will not be saved.".
 * @property discardConfirm the confirm button of the discard confirmation, "Discard".
 * @property stoppedEarlier the action that stops at an earlier time, "I stopped earlier".
 * @property stoppedEarlierTitle the title of its dialog, "When did you stop?".
 * @property stoppedEarlierMessage the explanation in its dialog.
 * @property started the start time in its dialog, given the formatted time: "Started 14 Nov 2026, 09:00".
 * @property workDuration the length of work that will be saved, given "2h 05m": "That is 2h 05m of work.".
 * @property save the dialog's save button, "Save this time".
 * @property cancel the cancel button of both dialogs, "Cancel".
 * @property stoppedAtFuture the error for a time after now, "The time cannot be in the future.".
 * @property stoppedAtTooEarly the error for a time less than a minute after the start.
 * @property failed shown when an action throws without a message, "That did not work. Try again.".
 * @property running the state of a running timer, read by TalkBack and announced, "Timer running".
 * @property stopped the state of a stopped timer, read by TalkBack and announced, "Timer stopped".
 * @property defaultLabel what the indicator shows and TalkBack reads when no label is given, "Timer".
 * @property open the TalkBack action name of the indicator's clock when it opens the item, "Open".
 * @property longRunning the warning after `warnAfterMillis`, given "12h".
 */
@Immutable
public class MonarchRunningTimerLabels(
    public val start: String? = null,
    public val stop: String? = null,
    public val stopAndLog: String? = null,
    public val more: String? = null,
    public val moreActions: String? = null,
    public val discard: String? = null,
    public val discardTitle: String? = null,
    public val discardMessage: String? = null,
    public val discardConfirm: String? = null,
    public val stoppedEarlier: String? = null,
    public val stoppedEarlierTitle: String? = null,
    public val stoppedEarlierMessage: String? = null,
    public val started: ((String) -> String)? = null,
    public val workDuration: ((String) -> String)? = null,
    public val save: String? = null,
    public val cancel: String? = null,
    public val stoppedAtFuture: String? = null,
    public val stoppedAtTooEarly: String? = null,
    public val failed: String? = null,
    public val running: String? = null,
    public val stopped: String? = null,
    public val defaultLabel: String? = null,
    public val open: String? = null,
    public val longRunning: ((String) -> String)? = null,
)
