import SwiftUI

/// A lead AI agent and the sub-agents it handed work to, as a tree: each
/// agent's status, what it is doing, how far it is and what it produced.
///
/// ```swift
/// MonarchAiSubagentTree(
///     run.agents,
///     selection: $selectedAgent,
///     onStop: { agent in run.stop(agent.id) },
///     onRespond: { agent in reply(to: agent) }
/// )
/// ```
///
/// The header sums the run up ("4 agents: 2 working, 1 needs you, 1 done").
/// Each agent is a row that selects it; an agent with sub-agents has a
/// chevron that shows or hides them. At first every branch with an active
/// agent is open, and a branch that becomes active later opens once, unless
/// the person closed it. Selecting an agent shows a bar with Respond (while
/// it needs input), Open, Show in conversation (when it has a part or
/// message) and Stop (while it is waiting or working), each only when its
/// handler is given. Nothing stops until Stop is pressed.
///
/// Accessibility: the tree is a container named "Agents". Each row is a
/// button read as the name, the status, the role, the activity, the steps,
/// the tool calls, the times and the summary; the selected row has the
/// selected trait and a bar beside it, not colour alone. The chevron is a
/// separate button, "Show agents under Researcher". A change of status
/// after the tree appears is announced ("Researcher: Done"), and so is a
/// new agent that already needs input or has failed. Times use the
/// environment's locale and time zone.
@_spi(Experimental) public struct MonarchAiSubagentTree: View {
    private let agents: [MonarchAiSubagent]
    private let label: String?
    private let selectionBinding: Binding<String?>?
    private let expandedBinding: Binding<Set<String>>?
    private let labels: MonarchAiSubagentTreeLabels
    private let onSelect: (@MainActor (MonarchAiSubagent) -> Void)?
    private let onRespond: (@MainActor (MonarchAiSubagent) -> Void)?
    private let onOpen: (@MainActor (MonarchAiSubagent) -> Void)?
    private let onOpenPart: (@MainActor (MonarchAiSubagent) -> Void)?
    private let onStop: (@MainActor (MonarchAiSubagent) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var ownSelection: String?
    @State private var ownExpanded: Set<String>?
    @State private var opened: Set<String> = []
    @State private var known: [String: MonarchAiSubagentStatus]?

    /// A tree of agents.
    ///
    /// - Parameters:
    ///   - agents: The top-level agents, usually one lead, each with the agents it handed work to.
    ///   - label: Replaces the built-in spoken name "Agents".
    ///   - selection: The id of the selected agent. Kept inside when `nil`.
    ///   - expandedIDs: The ids of open agents. Kept inside, with active branches open, when `nil`.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Runs when the person selects an agent.
    ///   - onRespond: Adds Respond for a selected agent that needs input.
    ///   - onOpen: Adds Open for the selected agent, for example to show its transcript.
    ///   - onOpenPart: Adds Show in conversation for a selected agent with a `partId` or `messageId`.
    ///   - onStop: Adds Stop for a selected agent that is waiting or working.
    public init(
        _ agents: [MonarchAiSubagent],
        label: String? = nil,
        selection: Binding<String?>? = nil,
        expandedIDs: Binding<Set<String>>? = nil,
        labels: MonarchAiSubagentTreeLabels = MonarchAiSubagentTreeLabels(),
        onSelect: (@MainActor (MonarchAiSubagent) -> Void)? = nil,
        onRespond: (@MainActor (MonarchAiSubagent) -> Void)? = nil,
        onOpen: (@MainActor (MonarchAiSubagent) -> Void)? = nil,
        onOpenPart: (@MainActor (MonarchAiSubagent) -> Void)? = nil,
        onStop: (@MainActor (MonarchAiSubagent) -> Void)? = nil
    ) {
        self.agents = agents
        self.label = label
        self.selectionBinding = selection
        self.expandedBinding = expandedIDs
        self.labels = labels
        self.onSelect = onSelect
        self.onRespond = onRespond
        self.onOpen = onOpen
        self.onOpenPart = onOpenPart
        self.onStop = onStop
    }

    private var selection: Binding<String?> { selectionBinding ?? $ownSelection }

    private var expanded: Binding<Set<String>> {
        if let expandedBinding { return expandedBinding }
        return Binding(
            get: { ownExpanded ?? MonarchAiSubagentTreeTokens.defaultExpanded(agents) },
            set: { ownExpanded = $0 }
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiSubagentTreeTokens.spacing) {
            Text(MonarchAiSubagentTreeTokens.summary(agents, locale: locale, labels: labels))
                .monarchTextStyle(.label)
                .foregroundStyle(theme.colors.textMuted)
            if !agents.isEmpty {
                VStack(alignment: .leading, spacing: MonarchAiSubagentTreeTokens.rowSpacing) {
                    ForEach(agents) { agent in
                        MonarchAiSubagentTreeRow(
                            agent: agent,
                            depth: 0,
                            expanded: expanded,
                            selection: selection,
                            labels: labels,
                            onToggle: toggle,
                            onSelect: select
                        )
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(label ?? labels.label ?? MonarchAiSubagentTreeStrings.label)
            }
            if let selected = MonarchAiSubagentTreeTokens.agent(selection.wrappedValue, in: agents) {
                actionBar(selected)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onAppear {
            known = MonarchAiSubagentTreeTokens.statuses(agents)
            if expandedBinding == nil, ownExpanded == nil {
                ownExpanded = MonarchAiSubagentTreeTokens.defaultExpanded(agents)
            }
            opened = expanded.wrappedValue
        }
        .onChange(of: agents) { _, newAgents in
            update(for: newAgents)
        }
    }

    @ViewBuilder private func actionBar(_ agent: MonarchAiSubagent) -> some View {
        let actions = MonarchAiSubagentTreeTokens.actions(
            for: agent,
            hasRespond: onRespond != nil,
            hasOpen: onOpen != nil,
            hasOpenPart: onOpenPart != nil,
            hasStop: onStop != nil
        )
        if !actions.isEmpty {
            VStack(alignment: .leading, spacing: MonarchAiSubagentTreeTokens.itemSpacing) {
                Text(agent.name)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityHidden(true)
                MonarchFlowLayout(spacing: MonarchAiSubagentTreeTokens.itemSpacing) {
                    if actions.respond, let onRespond {
                        MonarchButton(
                            labels.respond ?? MonarchAiSubagentTreeStrings.respond,
                            variant: .primary,
                            size: .sm
                        ) {
                            onRespond(agent)
                        }
                    }
                    if actions.open, let onOpen {
                        MonarchButton(labels.open ?? MonarchAiSubagentTreeStrings.open, size: .sm) {
                            onOpen(agent)
                        }
                    }
                    if actions.openPart, let onOpenPart {
                        MonarchButton(labels.openPart ?? MonarchAiSubagentTreeStrings.openPart, size: .sm) {
                            onOpenPart(agent)
                        }
                    }
                    if actions.stop, let onStop {
                        MonarchButton(labels.stop ?? MonarchAiSubagentTreeStrings.stop, variant: .danger, size: .sm) {
                            onStop(agent)
                        }
                    }
                }
            }
            .padding(MonarchAiSubagentTreeTokens.barPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                MonarchShapes.container.fill(theme.colors[role: MonarchAiSubagentTreeTokens.barFill])
            )
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.actionsFor?(agent.name) ?? MonarchAiSubagentTreeStrings.actionsFor(agent.name))
        }
    }

    private func toggle(_ id: String) {
        let isOpen = expanded.wrappedValue.contains(id)
        opened.insert(id)
        expanded.wrappedValue = MonarchAiSubagentTreeTokens.toggled(expanded.wrappedValue, id: id, isExpanded: !isOpen)
    }

    private func select(_ agent: MonarchAiSubagent) {
        selection.wrappedValue = agent.id
        onSelect?(agent)
    }

    private func update(for newAgents: [MonarchAiSubagent]) {
        if let text = MonarchAiSubagentTreeTokens.announcement(
            MonarchAiSubagentTreeTokens.changes(from: known, to: newAgents),
            labels: labels
        ) {
            AccessibilityNotification.Announcement(text).post()
        }
        known = MonarchAiSubagentTreeTokens.statuses(newAgents)
        guard expandedBinding == nil else { return }
        let fresh = MonarchAiSubagentTreeTokens.defaultExpanded(newAgents).subtracting(opened)
        guard !fresh.isEmpty else { return }
        opened.formUnion(fresh)
        ownExpanded = (ownExpanded ?? []).union(fresh)
    }
}

#if DEBUG
    #Preview("MonarchAiSubagentTree") {
        MonarchPreviewMatrix {
            MonarchAiSubagentTree(
                [
                    MonarchAiSubagent(
                        id: "lead",
                        name: "Lead",
                        role: "Plan the market report",
                        status: .working,
                        activity: "Waiting for two researchers",
                        stepsDone: 2,
                        stepsTotal: 5,
                        children: [
                            MonarchAiSubagent(id: "r1", name: "Researcher", status: .done, summary: "Found 3 filings"),
                            MonarchAiSubagent(id: "r2", name: "Analyst", status: .needsInput),
                        ]
                    )
                ],
                selection: .constant("r2"),
                onRespond: { _ in },
                onStop: { _ in }
            )
        }
    }
#endif
