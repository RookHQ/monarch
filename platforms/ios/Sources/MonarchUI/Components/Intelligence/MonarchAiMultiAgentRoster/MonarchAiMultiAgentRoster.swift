import SwiftUI

/// Every agent taking part in a task and what each is doing now: a list with
/// state and current task, or a compact stack of pictures for a header.
///
/// ```swift
/// MonarchAiMultiAgentRoster([
///     MonarchAiMultiAgentRosterMember(coordinator, state: .working, task: "Splitting the request"),
///     MonarchAiMultiAgentRosterMember(research, state: .waiting, task: "Needs access to the CRM"),
/// ])
/// MonarchAiMultiAgentRoster(members, variant: .stack, maxVisible: 3)
/// ```
///
/// The list shows a row per agent with its role, task and state; rows of
/// agents that wait for a person are tinted warning and rows with an error
/// danger. The stack is ``MonarchAvatarGroup`` of ``MonarchAiAgentAvatar``s,
/// with the agents that need attention first so they are never hidden
/// behind "+N". The summary ("3 agents · 2 working") follows both.
///
/// Accessibility: a list row is one element read as "Coordinator (AI
/// agent), Plans the work, Splitting the request, Working"; its picture is
/// hidden because the text says the same. In the stack each picture is an
/// image named by the agent and its state, and "+N" reads the hidden agents
/// and states. Every state is text as well as a ring and a tint. When a
/// state changes it is announced ("Research agent: Error"); the first state
/// is not.
@_spi(Experimental) public struct MonarchAiMultiAgentRoster: View {
    private let members: [MonarchAiMultiAgentRosterMember]
    private let variant: MonarchAiMultiAgentRosterVariant
    private let maxVisible: Int
    private let showsSummary: Bool
    private let announcesChanges: Bool
    private let labels: MonarchAiMultiAgentRosterLabels

    @Environment(\.monarchTheme) private var theme
    @State private var previous: [String: MonarchAiAgentState]?

    /// A multi-agent roster.
    ///
    /// - Parameters:
    ///   - members: The agents and their states. Keep each agent's id stable between updates.
    ///   - variant: A list of rows, or a stack of pictures.
    ///   - maxVisible: In the stack, the pictures shown before "+N".
    ///   - showsSummary: Whether the "3 agents · 2 working" line is shown.
    ///   - announcesChanges: Whether state changes are announced to VoiceOver.
    ///     Turn it off when the screen announces progress itself.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ members: [MonarchAiMultiAgentRosterMember],
        variant: MonarchAiMultiAgentRosterVariant = .list,
        maxVisible: Int = 4,
        showsSummary: Bool = true,
        announcesChanges: Bool = true,
        labels: MonarchAiMultiAgentRosterLabels = MonarchAiMultiAgentRosterLabels()
    ) {
        self.members = members
        self.variant = variant
        self.maxVisible = maxVisible
        self.showsSummary = showsSummary
        self.announcesChanges = announcesChanges
        self.labels = labels
    }

    private typealias Tokens = MonarchAiMultiAgentRosterTokens

    private var avatarLabels: MonarchAiAgentAvatarLabels {
        MonarchAiAgentAvatarLabels(kind: labels.agentKind, state: labels.state)
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if members.isEmpty {
                Text(labels.empty ?? MonarchAiMultiAgentRosterStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                switch variant {
                case .list: list
                case .stack: stack
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: Tokens.snapshot(members), initial: true) { _, snapshot in
            if announcesChanges,
                let text = Tokens.announcement(previous: previous, current: members, labels: labels)
            {
                AccessibilityNotification.Announcement(text).post()
            }
            previous = snapshot
        }
    }

    private var summary: some View {
        Text(Tokens.summary(members, labels: labels))
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }

    private var list: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            VStack(alignment: .leading, spacing: Tokens.spacing) {
                ForEach(members) { member in
                    row(member)
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.label ?? MonarchAiMultiAgentRosterStrings.label)
            if showsSummary { summary }
        }
    }

    private func row(_ member: MonarchAiMultiAgentRosterMember) -> some View {
        let shape = RoundedRectangle(cornerRadius: Tokens.rowCornerRadius, style: .continuous)
        return HStack(alignment: .center, spacing: Tokens.rowSpacing) {
            MonarchAiAgentAvatar(
                member.agent,
                state: member.state,
                size: Tokens.listAvatarSize,
                isDecorative: true,
                labels: avatarLabels
            )
            VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                Text(member.agent.name)
                    .monarchTextStyle(.body, weight: Tokens.nameWeight)
                    .foregroundStyle(theme.colors.textDefault)
                if let role = member.agent.role, !role.isEmpty {
                    Text(role)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if let task = member.task, !task.isEmpty {
                    Text(task)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textDefault)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text(Tokens.stateName(member.state, labels: labels))
                .monarchTextStyle(.caption, weight: Tokens.nameWeight)
                .foregroundStyle(theme.colors[role: Tokens.stateRole(member.state)])
                .fixedSize()
        }
        .padding(.horizontal, Tokens.rowHorizontalPadding)
        .padding(.vertical, Tokens.rowVerticalPadding)
        .background {
            if let role = Tokens.rowBackground(member.state) {
                shape.fill(theme.colors[role: role])
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.rowLabel(member, labels: labels))
    }

    private var stack: some View {
        let sorted = Tokens.sortedForStack(members)
        let hiddenText = Tokens.hiddenLabel(Tokens.hidden(sorted, maxVisible: maxVisible), labels: labels)
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: Tokens.stackSpacing) {
                avatars(sorted, hiddenText: hiddenText)
                if showsSummary { summary }
            }
            VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
                avatars(sorted, hiddenText: hiddenText)
                if showsSummary { summary }
            }
        }
    }

    private func avatars(_ sorted: [MonarchAiMultiAgentRosterMember], hiddenText: String) -> some View {
        MonarchAvatarGroup(
            labels.label ?? MonarchAiMultiAgentRosterStrings.label,
            sorted,
            maxVisible: max(1, maxVisible),
            size: Tokens.stackAvatarSize,
            shape: .square,
            overflowLabel: { _ in hiddenText },
            content: { member in
                MonarchAiAgentAvatar(
                    member.agent,
                    state: member.state,
                    size: Tokens.stackAvatarSize,
                    labels: avatarLabels
                )
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiMultiAgentRoster") {
        let members = [
            MonarchAiMultiAgentRosterMember(
                MonarchAiAgentRef(id: "c", name: "Coordinator", role: "Plans the work"),
                state: .working,
                task: "Splitting the request"
            ),
            MonarchAiMultiAgentRosterMember(
                MonarchAiAgentRef(id: "r", name: "Research agent"),
                state: .waiting,
                task: "Needs access to the CRM"
            ),
            MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "w", name: "Writing agent"), state: .error),
            MonarchAiMultiAgentRosterMember(MonarchAiAgentRef(id: "q", name: "Review agent")),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiMultiAgentRoster(members)
                MonarchAiMultiAgentRoster(members, variant: .stack, maxVisible: 2)
                MonarchAiMultiAgentRoster([])
            }
        }
    }
#endif
