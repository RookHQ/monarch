import SwiftUI

/// One agent of the tree, with its sub-agents while it is open. Children are
/// type-erased because the row contains rows of its own type.
struct MonarchAiSubagentTreeRow: View {
    let agent: MonarchAiSubagent
    let depth: Int
    @Binding var expanded: Set<String>
    @Binding var selection: String?
    let labels: MonarchAiSubagentTreeLabels
    let onToggle: @MainActor (String) -> Void
    let onSelect: @MainActor (MonarchAiSubagent) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.timeZone) private var timeZone
    @Environment(\.layoutDirection) private var layoutDirection

    private typealias Tokens = MonarchAiSubagentTreeTokens

    private var isExpanded: Bool { expanded.contains(agent.id) }
    private var isSelected: Bool { selection == agent.id }

    var body: some View {
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            HStack(alignment: .top, spacing: 0) {
                if agent.children.isEmpty {
                    Color.clear
                        .frame(width: MonarchAccessibility.minimumHitTarget, height: 1)
                        .accessibilityHidden(true)
                } else {
                    toggle
                }
                Button {
                    onSelect(agent)
                } label: {
                    content
                }
                .buttonStyle(.plain)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(Tokens.rowLabel(agent, locale: locale, timeZone: timeZone, labels: labels))
                .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
            }
            if isExpanded, !agent.children.isEmpty {
                VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
                    ForEach(agent.children) { child in
                        AnyView(
                            MonarchAiSubagentTreeRow(
                                agent: child,
                                depth: depth + 1,
                                expanded: $expanded,
                                selection: $selection,
                                labels: labels,
                                onToggle: onToggle,
                                onSelect: onSelect
                            )
                        )
                    }
                }
                .padding(.leading, Tokens.childIndent)
            }
        }
    }

    private var toggle: some View {
        Button {
            onToggle(agent.id)
        } label: {
            MonarchIcon(systemName: Tokens.chevronSymbol, size: .sm)
                .foregroundStyle(theme.colors.textMuted)
                .rotationEffect(Tokens.chevronRotation(isExpanded: isExpanded, layoutDirection: layoutDirection))
                .monarchAnimation(.fast, value: isExpanded)
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            isExpanded
                ? labels.collapse?(agent.name) ?? MonarchAiSubagentTreeStrings.collapse(agent.name)
                : labels.expand?(agent.name) ?? MonarchAiSubagentTreeStrings.expand(agent.name)
        )
    }

    private var content: some View {
        let look = Tokens.look(agent.status)
        let meta = Tokens.meta(agent, locale: locale, timeZone: timeZone, labels: labels)
        let shape = MonarchShapes.control
        return VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            MonarchFlowLayout(spacing: Tokens.itemSpacing) {
                MonarchAiAgentAvatar(
                    MonarchAiAgentRef(id: agent.id, name: agent.name),
                    state: Tokens.avatarState(agent.status),
                    size: .sm,
                    isDecorative: true
                )
                Text(agent.name)
                    .monarchTextStyle(depth == 0 ? .bodyStrong : .body)
                    .foregroundStyle(theme.colors.textDefault)
                MonarchStatusBadge(
                    MonarchStatusDefinition(
                        Tokens.statusName(agent.status, labels: labels),
                        tone: look.tone,
                        systemImage: look.symbol
                    ),
                    size: .sm
                )
            }
            if let role = agent.role, !role.isEmpty {
                caption(role)
            }
            if agent.status == .working, let activity = agent.activity, !activity.isEmpty {
                Text(activity)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.aiStrong)
            }
            if !meta.isEmpty {
                caption(meta.joined(separator: " · "))
                    .monospacedDigit()
            }
            if Tokens.hasEnded(agent.status), let summary = agent.summary, !summary.isEmpty {
                Text(summary)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textDefault)
            }
        }
        .multilineTextAlignment(.leading)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Tokens.rowPadding)
        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
        .background(shape.fill(isSelected ? theme.colors[role: Tokens.selectedFill] : .clear))
        .overlay(alignment: .leading) {
            if isSelected {
                Capsule()
                    .fill(theme.colors[role: Tokens.selectionBar])
                    .frame(width: Tokens.selectionBarWidth)
                    .padding(.vertical, Tokens.rowPadding)
            }
        }
        .contentShape(Rectangle())
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
