package dev.monarchui.components.intelligence.aisubagenttree

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchAiSubagentTree`. Every
 * `null` keeps the built-in string `monarch_aisubagenttree_<key>`. Counts
 * reach the functions as numbers; the built-in strings write them in the
 * tree's locale.
 *
 * @property label the spoken name of the tree, "Agents".
 * @property status the name of a status: "Working", "Needs input", "Waiting", "Done", "Failed", "Stopped".
 * @property agentCount the total in the summary, "4 agents".
 * @property statusCount one part of the summary per status, such as "2 working" or "1 needs you".
 * @property empty shown instead of the tree without agents, "No agents yet".
 * @property steps the progress of an agent, "3 of 7 steps", or "3 steps" when the total is `null`.
 * @property toolCalls the tool calls of an agent, "12 tool calls".
 * @property started the start of an agent that has not ended, "Started 12:01 PM", given the time.
 * @property ran the start and end of an agent that has ended, "12:01 PM to 12:04 PM".
 * @property actionsFor the spoken name of the selected agent's actions, "Actions for Researcher".
 * @property stop the Stop button.
 * @property open the Open button.
 * @property respond the Respond button.
 * @property openPart the button that shows the agent's part of the conversation, "Show in conversation".
 * @property announce announced when an agent's status changes, "Researcher: Done".
 * @property expand the spoken name of the button that opens an agent, "Expand Researcher".
 * @property collapse the spoken name of the button that closes an agent, "Collapse Researcher".
 */
@Immutable
public class MonarchAiSubagentTreeLabels(
    public val label: String? = null,
    public val status: ((MonarchAiSubagentStatus) -> String)? = null,
    public val agentCount: ((count: Int) -> String)? = null,
    public val statusCount: ((status: MonarchAiSubagentStatus, count: Int) -> String)? = null,
    public val empty: String? = null,
    public val steps: ((done: Int, total: Int?) -> String)? = null,
    public val toolCalls: ((count: Int) -> String)? = null,
    public val started: ((time: String) -> String)? = null,
    public val ran: ((start: String, end: String) -> String)? = null,
    public val actionsFor: ((name: String) -> String)? = null,
    public val stop: String? = null,
    public val open: String? = null,
    public val respond: String? = null,
    public val openPart: String? = null,
    public val announce: ((name: String, status: String) -> String)? = null,
    public val expand: ((name: String) -> String)? = null,
    public val collapse: ((name: String) -> String)? = null,
)
