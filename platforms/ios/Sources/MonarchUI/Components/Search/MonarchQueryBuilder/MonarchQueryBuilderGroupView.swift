import SwiftUI

/// One group of a ``MonarchQueryBuilder``: its switch, rules, nested groups and add buttons.
struct MonarchQueryBuilderGroupView: View {
    @Binding var group: MonarchQueryGroup
    let path: [Int]
    let context: MonarchQueryBuilderContext
    let onRemove: (() -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    private var depth: Int { path.count }
    private var name: String { context.text.group(path) }

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchQueryBuilderTokens.groupSpacing) {
            header
            if group.rules.isEmpty && group.groups.isEmpty {
                Text(context.text.emptyGroup)
                    .monarchTextStyle(.body)
                    .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
            }
            ForEach(Array(group.rules.enumerated()), id: \.element.id) { index, rule in
                MonarchQueryBuilderRuleView(
                    rule: ruleBinding(rule.id, fallback: rule),
                    groupName: name,
                    position: index + 1,
                    context: context,
                    onRemove: { group.rules.removeAll { $0.id == rule.id } }
                )
            }
            ForEach(Array(group.groups.enumerated()), id: \.element.id) { index, child in
                MonarchQueryBuilderGroupView(
                    group: groupBinding(child.id, fallback: child),
                    path: path + [index + 1],
                    context: context,
                    onRemove: { group.groups.removeAll { $0.id == child.id } }
                )
            }
            addButtons
        }
        .padding(depth > 1 ? MonarchQueryBuilderTokens.padding : 0)
        .background {
            if let fill = MonarchQueryBuilderTokens.groupFill(depth: depth) {
                MonarchShapes.container.fill(theme.colors[role: fill])
                MonarchShapes.container.strokeBorder(
                    theme.colors[role: MonarchQueryBuilderTokens.border(hasProblem: false)],
                    lineWidth: MonarchShapes.borderWidth
                )
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(name)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: MonarchQueryBuilderTokens.ruleSpacing) {
            Text(name)
                .monarchTextStyle(.subheading)
                .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                .accessibilityAddTraits(.isHeader)
            Text(context.text.combinatorBefore)
                .monarchTextStyle(.caption)
                .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
                .accessibilityHidden(true)
            Picker(context.text.combinatorLabel(name), selection: $group.combinator) {
                ForEach(MonarchQueryCombinator.allCases, id: \.self) { combinator in
                    Text(context.text.combinator(combinator)).tag(combinator)
                }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .fixedSize()
            .accessibilityLabel(context.text.combinatorLabel(name))
            .monarchMinimumHitTarget()
            Spacer(minLength: 0)
            if let onRemove {
                Button(action: onRemove) {
                    Label(context.text.removeGroup(name), systemImage: MonarchQueryBuilderTokens.removeSymbol)
                        .labelStyle(.iconOnly)
                }
                .buttonStyle(.monarch(.ghost, size: .sm))
            }
        }
    }

    @ViewBuilder private var addButtons: some View {
        if group.rules.count < context.maxRulesPerGroup {
            Button(context.text.addRule, systemImage: MonarchQueryBuilderTokens.addSymbol) {
                group.rules.append(MonarchCondition(id: context.makeID(), field: context.fields.first))
            }
            .buttonStyle(.monarch(.secondary, size: .sm))
            .disabled(context.fields.isEmpty)
        } else {
            note(context.text.maxRulesReached(context.maxRulesPerGroup))
        }
        if MonarchQueryBuilderTokens.canAddGroup(depth: depth, maxDepth: context.maxDepth) {
            Button(context.text.addGroup, systemImage: MonarchQueryBuilderTokens.addSymbol) {
                let combinator: MonarchQueryCombinator = group.combinator == .and ? .or : .and
                group.groups.append(MonarchQueryGroup(id: context.makeID(), combinator: combinator))
            }
            .buttonStyle(.monarch(.ghost, size: .sm))
            .disabled(context.fields.isEmpty)
        } else if MonarchQueryBuilderTokens.showsDepthNote(depth: depth, maxDepth: context.maxDepth) {
            note(context.text.maxDepthReached)
        }
    }

    private func note(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
    }

    private func ruleBinding(_ id: String, fallback: MonarchCondition) -> Binding<MonarchCondition> {
        Binding(
            get: { group.rules.first { $0.id == id } ?? fallback },
            set: { new in
                if let index = group.rules.firstIndex(where: { $0.id == id }) { group.rules[index] = new }
            }
        )
    }

    private func groupBinding(_ id: String, fallback: MonarchQueryGroup) -> Binding<MonarchQueryGroup> {
        Binding(
            get: { group.groups.first { $0.id == id } ?? fallback },
            set: { new in
                if let index = group.groups.firstIndex(where: { $0.id == id }) { group.groups[index] = new }
            }
        )
    }
}
