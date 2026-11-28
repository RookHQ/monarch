package com.rookhq.monarch.components.intelligence.aibackgroundtasks

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiBackgroundTasks`.
 * Every `null` keeps the built-in string `monarch_aibackgroundtasks_<key>`.
 *
 * @property title the button text and the panel heading, "Background tasks".
 * @property runningCount the button badge: how many tasks run, "2 running".
 * @property queuedCount the button badge while nothing runs but tasks wait to start, "1 queued".
 * @property needsInputCount the button badge: how many tasks wait on the person, "1 needs you".
 * @property needsInputGroup the heading of tasks waiting on the person, "Needs you".
 * @property runningGroup the heading of running and queued tasks, "Running".
 * @property finishedGroup the heading of finished tasks, "Finished".
 * @property groupHeading a heading with its count, given the heading and the count: "Running (2)".
 * @property status the word for each status: "Queued", "Running", "Needs your input", "Done", "Failed",
 * "Cancelled".
 * @property elapsed the time a running task has run, given the formatted duration: "Running for 3 min 12 sec".
 * @property ended when a task ended, given relative text: "Ended 5 minutes ago".
 * @property progressLabel the name of a task's progress bar, given its title.
 * @property open the Open button.
 * @property cancel the Cancel button.
 * @property retry the Retry button.
 * @property respond the Respond button.
 * @property dismiss the Dismiss button.
 * @property clearFinished the button that removes every finished task.
 * @property actionLabel what TalkBack reads for a task's button, given the action and the title: "Open: Summarize".
 * @property empty shown when there are no tasks.
 * @property announce announced when a task succeeds, fails or needs input, given the title and the status word.
 */
@Immutable
public class MonarchAiBackgroundTasksLabels(
    public val title: String? = null,
    public val runningCount: ((Int) -> String)? = null,
    public val queuedCount: ((Int) -> String)? = null,
    public val needsInputCount: ((Int) -> String)? = null,
    public val needsInputGroup: String? = null,
    public val runningGroup: String? = null,
    public val finishedGroup: String? = null,
    public val groupHeading: ((label: String, count: Int) -> String)? = null,
    public val status: ((MonarchAiBackgroundTaskStatus) -> String)? = null,
    public val elapsed: ((duration: String) -> String)? = null,
    public val ended: ((relative: String) -> String)? = null,
    public val progressLabel: ((title: String) -> String)? = null,
    public val open: String? = null,
    public val cancel: String? = null,
    public val retry: String? = null,
    public val respond: String? = null,
    public val dismiss: String? = null,
    public val clearFinished: String? = null,
    public val actionLabel: ((action: String, title: String) -> String)? = null,
    public val empty: String? = null,
    public val announce: ((title: String, status: String) -> String)? = null,
)
