import SwiftUI

/// One goal of the tree, with its children when it is open. Children are
/// type-erased because the node contains nodes of its own type.
struct MonarchGoalTreeNode<Actions: View>: View {
    let goal: MonarchGoal
    let depth: Int
    let trail: Set<String>
    let children: [String: [MonarchGoal]]
    @Binding var expanded: Set<String>
    let isQuietChildren: Bool
    let labels: MonarchGoalTreeLabels
    let canAddChild: ((MonarchGoal, Int) -> Bool)?
    let onAddChild: (@MainActor (MonarchGoal) -> Void)?
    let actions: (MonarchGoal) -> Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.layoutDirection) private var layoutDirection

    private typealias Tokens = MonarchGoalTreeTokens

    private var kids: [MonarchGoal] { Tokens.children(of: goal, in: children, trail: trail) }
    private var adds: Bool {
        Tokens.addsChild(goal, depth: depth, hasHandler: onAddChild != nil, canAddChild: canAddChild)
    }
    private var isExpanded: Binding<Bool> {
        Binding(
            get: { expanded.contains(goal.id) },
            set: { expanded = Tokens.toggled(expanded, id: goal.id, isExpanded: $0) }
        )
    }

    var body: some View {
        let kids = kids
        let opens = !kids.isEmpty || adds
        VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            HStack(alignment: .top, spacing: Tokens.columnSpacing) {
                if opens {
                    toggle
                } else {
                    Color.clear
                        .frame(width: MonarchAccessibility.minimumHitTarget, height: 1)
                        .accessibilityHidden(true)
                }
                summary(childCount: kids.count)
                actions(goal)
            }
            if opens && isExpanded.wrappedValue {
                VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
                    ForEach(kids) { kid in
                        AnyView(
                            MonarchGoalTreeNode(
                                goal: kid,
                                depth: depth + 1,
                                trail: trail.union([goal.id]),
                                children: children,
                                expanded: $expanded,
                                isQuietChildren: isQuietChildren,
                                labels: labels,
                                canAddChild: canAddChild,
                                onAddChild: onAddChild,
                                actions: actions
                            )
                        )
                    }
                    if adds, let onAddChild {
                        Button {
                            onAddChild(goal)
                        } label: {
                            Label(labels.addChild, systemImage: Tokens.addSymbol)
                        }
                        .buttonStyle(.plain)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors.textLink)
                        .monarchMinimumHitTarget()
                        .accessibilityLabel(labels.addChildTo(goal.title))
                        .padding(.leading, Tokens.addButtonIndent)
                    }
                }
                .padding(.leading, Tokens.childIndent)
            }
        }
    }

    private var toggle: some View {
        let open = isExpanded.wrappedValue
        return Button {
            isExpanded.wrappedValue.toggle()
        } label: {
            MonarchDisclosureChevron(isExpanded: open)
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(open ? labels.collapse(goal.title) : labels.expand(goal.title))
    }

    private func summary(childCount: Int) -> some View {
        let showsBadge = Tokens.showsBadge(goal.status, depth: depth, isQuietChildren: isQuietChildren)
        return VStack(alignment: .leading, spacing: Tokens.lineSpacing) {
            MonarchFlowLayout(spacing: Tokens.headingSpacing) {
                if let owner = goal.ownerName, !owner.isEmpty {
                    MonarchAvatar(url: goal.ownerAvatarURL, name: owner, size: .sm, isDecorative: true)
                }
                Text(goal.title)
                    .monarchTextStyle(depth == 0 ? .bodyStrong : .body)
                    .foregroundStyle(theme.colors.textDefault)
                    .multilineTextAlignment(.leading)
                if showsBadge {
                    // A tinted badge with the status in words, no icon, as on the web.
                    MonarchBadge(labels.status(goal.status), tone: Tokens.tone(goal.status), size: .sm)
                }
                if let meta = goal.meta, !meta.isEmpty {
                    caption(meta)
                }
                if !isExpanded.wrappedValue, childCount > 0 {
                    caption(labels.childCount(childCount))
                }
            }
            if let aligned = goal.alignedTo, !aligned.isEmpty {
                HStack(alignment: .firstTextBaseline, spacing: Tokens.columnSpacing) {
                    MonarchIcon(systemName: Tokens.alignedSymbol, size: .sm)
                    Text("\(labels.alignedTo) \(aligned)").monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.textMuted)
            }
            HStack(spacing: Tokens.progressSpacing) {
                ProgressView(value: Tokens.fraction(goal.progress))
                    .progressViewStyle(.monarchLinear(tone: Tokens.barTone(goal.status)))
                caption(Tokens.accessibilityValue(goal.progress, locale: locale))
                    .monospacedDigit()
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Tokens.accessibilityLabel(
                goal,
                childCount: childCount,
                isExpanded: isExpanded.wrappedValue,
                labels: labels
            )
        )
        .accessibilityValue(Tokens.accessibilityValue(goal.progress, locale: locale))
    }

    private func caption(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)
    }
}
