package com.rookhq.monarch.components.intelligence.aisubagenttree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchTimeZone
import com.rookhq.monarch.theme.MonarchTheme
import java.util.Locale

/**
 * A lead AI agent and the sub-agents it handed work to, as a tree: each
 * agent's status, what it is doing, how far it is and what it produced. The
 * person follows the run, selects an agent, and stops, opens or answers it.
 *
 * A Monarch pattern drawn as rows in a `Column` (put it in a scrolling
 * parent), with `MonarchStatusBadge` for statuses and `MonarchButton` for the
 * actions. A summary ("4 agents: 2 working, 1 needs you, 1 done") comes first.
 * Each agent is one selectable TalkBack element read as its name, status,
 * role, activity, facts and summary; its open and close button is a separate
 * element. The actions of the selected agent follow the tree as a group
 * named "Actions for …": Respond while it needs input, Open, Show in
 * conversation, and Stop while it waits or works. Status changes after the
 * first look are announced through a polite live region. Agents with
 * children that are active (or have active agents under them) start open,
 * and an agent that becomes active later opens once, unless [expanded] is
 * kept by the app.
 *
 * @param agents the top-level agents (usually one lead), each with the agents it handed work to.
 * @param statusIcon an icon in each status badge, by status: a spinner for Working, a check for Done and so on.
 * Monarch bundles no icons.
 * @param expandIcon a chevron that points to the end, such as a `MonarchIcon` of size `Sm` that mirrors in right
 * to left. It turns to point down when the agent is open. Monarch bundles no icons.
 * @param modifier the modifier of the tree.
 * @param selection the id of the selected agent. The tree keeps the person's choice and starts again from this
 * value whenever it changes.
 * @param expanded the ids of the open agents. `null` keeps them inside the tree.
 * @param onExpandedChange called with the new set of open ids.
 * @param locale the locale of numbers and times. `null` uses the configuration's locale.
 * @param timeZone the time zone of start and end times. An IANA id such as `"Europe/Berlin"`. `null`, or an id the
 * device does not know, is the device's zone.
 * @param labels replacements for the built-in strings.
 * @param onSelect called with the agent the person selects.
 * @param onRespond shows Respond for a selected agent that needs input.
 * @param onOpen shows Open for the selected agent, for example to show its transcript.
 * @param onOpenPart shows Show in conversation for a selected agent with a part or message id.
 * @param onStop shows Stop for a selected agent that is waiting or working. Nothing stops until it is pressed.
 */
@Suppress("LongParameterList", "LongMethod")
@ExperimentalMonarchApi
@Composable
public fun MonarchAiSubagentTree(
    agents: List<MonarchAiSubagent>,
    expandIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selection: String? = null,
    expanded: Set<String>? = null,
    onExpandedChange: ((Set<String>) -> Unit)? = null,
    locale: Locale? = null,
    timeZone: String? = null,
    statusIcon: (@Composable (MonarchAiSubagentStatus) -> Unit)? = null,
    labels: MonarchAiSubagentTreeLabels = MonarchAiSubagentTreeLabels(),
    onSelect: ((MonarchAiSubagent) -> Unit)? = null,
    onRespond: ((MonarchAiSubagent) -> Unit)? = null,
    onOpen: ((MonarchAiSubagent) -> Unit)? = null,
    onOpenPart: ((MonarchAiSubagent) -> Unit)? = null,
    onStop: ((MonarchAiSubagent) -> Unit)? = null,
) {
    val tokens = MonarchAiSubagentTreeTokens
    val shownLocale = locale ?: LocalConfiguration.current.locales[0]
    val zone = monarchTimeZone(timeZone)
    val texts = rememberMonarchAiSubagentTreeTexts(labels, shownLocale, zone)
    var selectedId by rememberSaveable(selection) { mutableStateOf(selection) }
    var inside by rememberSaveable(saver = idSetSaver) { mutableStateOf(tokens.defaultExpanded(agents)) }
    val seen = remember { mutableSetOf<String>().apply { addAll(inside) } }
    LaunchedEffect(agents, expanded == null) {
        if (expanded != null) return@LaunchedEffect
        val fresh = tokens.defaultExpanded(agents) - seen
        if (fresh.isNotEmpty()) {
            seen += fresh
            inside = inside + fresh
        }
    }
    val open = expanded ?: inside
    val setOpen: (Set<String>) -> Unit = { next ->
        seen += next
        if (expanded == null) inside = next
        onExpandedChange?.invoke(next)
    }
    var announcement by remember { mutableStateOf("") }
    var known by remember { mutableStateOf<Map<String, MonarchAiSubagentStatus>?>(null) }
    LaunchedEffect(agents) {
        val news = tokens.news(known, agents)
        known = tokens.flatten(agents).associate { it.id to it.status }
        if (news.isNotEmpty()) announcement = news.joinToString(" ") { texts.announce(it) }
    }
    val all = remember(agents) { tokens.flatten(agents) }
    val selected = all.firstOrNull { it.id == selectedId }
    Column(
        modifier = modifier.semantics { isTraversalGroup = true },
        verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
    ) {
        Text(
            text = texts.summary(agents),
            style = MonarchTheme.typography.body,
            color = MonarchTheme.colors.textMuted,
        )
        if (all.isNotEmpty()) {
            Column(
                modifier = Modifier.semantics {
                    contentDescription = texts.label
                    isTraversalGroup = true
                },
            ) {
                tokens.lines(agents, open).forEach { line ->
                    MonarchAiSubagentTreeRow(
                        line = line,
                        selected = line.agent.id == selectedId,
                        expandIcon = expandIcon,
                        statusIcon = statusIcon,
                        texts = texts,
                        onSelect = {
                            selectedId = line.agent.id
                            onSelect?.invoke(line.agent)
                        },
                        onToggle = { setOpen(tokens.toggled(open, line.agent.id)) },
                    )
                }
            }
        }
        if (selected != null) {
            Actions(selected, texts, onRespond, onOpen, onOpenPart, onStop)
        }
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

private val idSetSaver = listSaver<MutableState<Set<String>>, String>(
    save = { it.value.toList() },
    restore = { mutableStateOf(it.toSet()) },
)

/** The buttons for the selected agent, as a group named after it. Nothing when no button applies. */
@Suppress("LongParameterList")
@Composable
private fun Actions(
    agent: MonarchAiSubagent,
    texts: MonarchAiSubagentTreeTexts,
    onRespond: ((MonarchAiSubagent) -> Unit)?,
    onOpen: ((MonarchAiSubagent) -> Unit)?,
    onOpenPart: ((MonarchAiSubagent) -> Unit)?,
    onStop: ((MonarchAiSubagent) -> Unit)?,
) {
    val tokens = MonarchAiSubagentTreeTokens
    val rules = MonarchAiSubagentTreeRules
    val respond = onRespond?.takeIf { rules.canRespond(agent) }
    val openPart = onOpenPart?.takeIf { rules.canOpenPart(agent) }
    val stop = onStop?.takeIf { rules.canStop(agent) }
    if (listOf(respond, onOpen, openPart, stop).all { it == null }) return
    Column(
        modifier = Modifier.semantics {
            contentDescription = texts.actionsFor(agent.name)
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Text(text = agent.name, style = MonarchTheme.typography.bodyStrong, color = MonarchTheme.colors.textDefault)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            val size = MonarchButtonSize.Sm
            respond?.let {
                MonarchButton(texts.respond, { it(agent) }, variant = MonarchButtonVariant.Primary, size = size)
            }
            onOpen?.let { MonarchButton(texts.open, { it(agent) }, size = size) }
            openPart?.let { MonarchButton(texts.openPart, { it(agent) }, size = size) }
            stop?.let { MonarchButton(texts.stop, { it(agent) }, variant = MonarchButtonVariant.Danger, size = size) }
        }
    }
}
