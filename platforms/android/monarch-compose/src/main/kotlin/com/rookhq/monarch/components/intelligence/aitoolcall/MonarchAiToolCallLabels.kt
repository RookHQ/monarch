package com.rookhq.monarch.components.intelligence.aitoolcall

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiToolCallState

/**
 * Replacements for the built-in strings of a `MonarchAiToolCall`. Every
 * `null` keeps the built-in string `monarch_aitoolcall_<key>`.
 *
 * @property state the state badge text for a state, such as "Done". Return `null` to keep the built-in text.
 * @property formatDuration writes how long the call took, from milliseconds. The built-in form is "0.4s" under a
 * second, "12s" under a minute, then "2m 05s".
 * @property input the heading of the arguments block, "Input".
 * @property output the heading of the result block, "Output".
 * @property error read before the error message, "Error".
 * @property tool names the tool in the details, "Tool".
 * @property empty shown when the call has no input and no output yet, "No details yet."
 * @property stateChanged the announcement of a new state, from the tool label and the state: "Search documents: Done".
 * @property showMore the button that shows the rest of a long value, "Show all".
 * @property truncated how much of a long value is shown, from the shown and total character counts.
 * @property expanded the state TalkBack reads on the header while the details are shown, "Expanded".
 * @property collapsed the state TalkBack reads on the header while the details are hidden, "Collapsed".
 */
@Immutable
public class MonarchAiToolCallLabels(
    public val state: ((MonarchAiToolCallState) -> String?)? = null,
    public val formatDuration: ((millis: Long) -> String)? = null,
    public val input: String? = null,
    public val output: String? = null,
    public val error: String? = null,
    public val tool: String? = null,
    public val empty: String? = null,
    public val stateChanged: ((label: String, state: String) -> String)? = null,
    public val showMore: String? = null,
    public val truncated: ((shown: Int, total: Int) -> String)? = null,
    public val expanded: String? = null,
    public val collapsed: String? = null,
)
