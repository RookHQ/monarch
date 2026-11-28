import SwiftUI

/// Lets a person pick what grounds the assistant's answers: a tree of
/// spaces, documents and apps with checkboxes. Checking a group selects
/// everything in it; a partly selected group shows a dash.
///
/// ```swift
/// @State private var scope = ["handbook"]
///
/// MonarchAiKnowledgeScope(sources, selection: $scope, startsExpanded: ["handbook"])
/// ```
///
/// The selection lists every fully selected source, groups included, in tree
/// order; a group's id stands for everything in it. Ids that are not in the
/// tree are kept. Each source is a standard `Toggle` in the
/// ``MonarchCheckboxToggleStyle``; a group is a `Toggle(sources:isOn:)`, so
/// its mixed state is the system's. Checking a checked or partly selected
/// group clears it; checking an unchecked one selects all of it. Disable the
/// whole tree with `.disabled(_:)`.
///
/// Accessibility: the tree is a container named "Knowledge sources". Each
/// source is a checkbox named "Handbook, Space" whose value is on, off or
/// mixed, with its description as the hint. A group's chevron is a separate
/// button named "Show contents of Handbook" or "Hide contents of Handbook".
/// The summary under the tree is plain text. Every control has a 44-point
/// touch target and the rows wrap at every text size.
public struct MonarchAiKnowledgeScope: View {
    private let sources: [MonarchAiKnowledgeSource]
    @Binding private var selection: [String]
    private let showsSummary: Bool
    private let labels: MonarchAiKnowledgeScopeLabels

    @Environment(\.monarchTheme) private var theme
    @State private var expanded: Set<String>

    /// A knowledge scope tree.
    ///
    /// - Parameters:
    ///   - sources: The sources to choose from, as a tree.
    ///   - selection: The ids of the selected sources. Written after every change.
    ///   - startsExpanded: The ids of the groups that start open.
    ///   - showsSummary: Whether "3 sources selected" is shown under the tree.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ sources: [MonarchAiKnowledgeSource],
        selection: Binding<[String]>,
        startsExpanded: Set<String> = [],
        showsSummary: Bool = true,
        labels: MonarchAiKnowledgeScopeLabels = MonarchAiKnowledgeScopeLabels()
    ) {
        self.sources = sources
        _selection = selection
        self.showsSummary = showsSummary
        self.labels = labels
        _expanded = State(initialValue: startsExpanded)
    }

    /// The content of the view.
    public var body: some View {
        if sources.isEmpty {
            MonarchEmptyState(
                labels.empty ?? MonarchAiKnowledgeScopeStrings.empty,
                systemImage: MonarchAiKnowledgeScopeTokens.emptySymbol,
                size: .sm
            )
        } else {
            let states = MonarchAiKnowledgeScopeTokens.states(sources, selection: selection)
            VStack(alignment: .leading, spacing: MonarchAiKnowledgeScopeTokens.summarySpacing) {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(MonarchAiKnowledgeScopeTokens.visibleRows(sources, expanded: expanded)) { row in
                        rowView(row, state: states[row.id] ?? .unchecked)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.label ?? MonarchAiKnowledgeScopeStrings.label)
                if showsSummary {
                    Text(
                        MonarchAiKnowledgeScopeTokens.summary(
                            count: MonarchAiKnowledgeScopeTokens.selectedCount(sources, states: states),
                            labels: labels
                        )
                    )
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchAiKnowledgeScopeTokens.secondary])
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func rowView(_ row: MonarchAiKnowledgeScopeRow, state: MonarchAiKnowledgeScopeCheck) -> some View {
        let source = row.source
        return HStack(alignment: .center, spacing: MonarchAiKnowledgeScopeTokens.rowSpacing) {
            if row.isGroup {
                disclosureButton(source)
            } else {
                Color.clear
                    .frame(width: MonarchAiKnowledgeScopeTokens.disclosureWidth, height: 1)
                    .accessibilityHidden(true)
            }
            Toggle(sources: checkBindings(for: source, state: state), isOn: \.self) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiKnowledgeScopeTokens.rowSpacing) {
                    VStack(alignment: .leading, spacing: MonarchAiKnowledgeScopeTokens.textSpacing) {
                        Text(source.label)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors[role: MonarchAiKnowledgeScopeTokens.label])
                        if let description = source.description, !description.isEmpty {
                            Text(description)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors[role: MonarchAiKnowledgeScopeTokens.secondary])
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    Text(MonarchAiKnowledgeScopeTokens.kindName(source.kind, labels: labels))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchAiKnowledgeScopeTokens.secondary])
                }
            }
            .toggleStyle(.monarchCheckbox)
            .environment(\.monarchAiTone, true)
            .accessibilityLabel(MonarchAiKnowledgeScopeTokens.itemLabel(source, labels: labels))
            .accessibilityHint(source.description ?? "")
        }
        .padding(.leading, MonarchAiKnowledgeScopeTokens.indent(level: row.level))
    }

    private func disclosureButton(_ source: MonarchAiKnowledgeSource) -> some View {
        let isExpanded = expanded.contains(source.id)
        return Button {
            if isExpanded { expanded.remove(source.id) } else { expanded.insert(source.id) }
        } label: {
            MonarchIcon(
                systemName: isExpanded
                    ? MonarchAiKnowledgeScopeTokens.expandedSymbol : MonarchAiKnowledgeScopeTokens.collapsedSymbol,
                size: .sm
            )
            .foregroundStyle(theme.colors[role: MonarchAiKnowledgeScopeTokens.chevron])
            .frame(width: MonarchAiKnowledgeScopeTokens.disclosureWidth)
            .monarchMinimumHitTarget()
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            MonarchAiKnowledgeScopeTokens.disclosureLabel(source, isExpanded: isExpanded, labels: labels)
        )
    }

    /// The bindings behind one checkbox. A mixed group gets one on and one off
    /// binding, which makes the standard toggle mixed. Every setter applies the
    /// same decision, taken from the state when the row was drawn, so the
    /// toggle writing several sources changes the selection once.
    private func checkBindings(
        for source: MonarchAiKnowledgeSource,
        state: MonarchAiKnowledgeScopeCheck
    ) -> [Binding<Bool>] {
        let isOn = state == .unchecked
        let sources = sources
        let selection = $selection
        let apply: @Sendable (Bool) -> Void = { _ in
            selection.wrappedValue = MonarchAiKnowledgeScopeTokens.setting(
                sources,
                selection: selection.wrappedValue,
                id: source.id,
                isOn: isOn
            )
        }
        switch state {
        case .mixed:
            return [Binding(get: { true }, set: apply), Binding(get: { false }, set: apply)]
        case .checked, .unchecked:
            return [Binding(get: { state == .checked }, set: apply)]
        }
    }
}

#if DEBUG
    #Preview("MonarchAiKnowledgeScope") {
        MonarchPreviewMatrix {
            MonarchAiKnowledgeScope(
                [
                    MonarchAiKnowledgeSource(
                        id: "handbook",
                        label: "Handbook",
                        kind: .space,
                        description: "3 documents",
                        children: [
                            MonarchAiKnowledgeSource(id: "policies", label: "Policies", kind: .document),
                            MonarchAiKnowledgeSource(id: "benefits", label: "Benefits", kind: .document),
                            MonarchAiKnowledgeSource(id: "travel", label: "Travel", kind: .document),
                        ]
                    ),
                    MonarchAiKnowledgeSource(id: "crm", label: "Sales CRM", kind: .app, description: "Connected"),
                ],
                selection: .constant(["policies", "crm"]),
                startsExpanded: ["handbook"]
            )
        }
    }
#endif
