import MonarchTokens
import SwiftUI

/// Chooses the scopes (permissions) to grant, grouped by resource, such as
/// the APIs an API key or an OAuth app may call.
///
/// ```swift
/// @State private var scopes: Set<String> = ["projects:read"]
///
/// MonarchScopeSelector(
///     "Scopes",
///     selection: $scopes,
///     groups: [
///         MonarchScopeGroup(id: "projects", label: "Projects API", scopes: [
///             MonarchScopeOption("projects:read", description: "List and read projects."),
///             MonarchScopeOption("projects:write", description: "Create and update projects."),
///         ]),
///     ],
///     initiallyExpanded: ["projects"]
/// )
/// ```
///
/// Each group is a standard `DisclosureGroup` with the Monarch style whose
/// header shows how many of its scopes are chosen; inside, a standard
/// `Toggle(sources:isOn:)` selects all and a checkbox `Toggle` stands for
/// each scope. Long lists page in steps.
///
/// Accessibility: the selector is a container named by `label`. Each group
/// header reads its name, description, summary ("1 of 2 scopes") and
/// Expanded or Collapsed. Each scope reads its label (or the scope string)
/// and description; select all reads mixed when some are chosen. In
/// read-only mode the checkboxes are disabled and the groups still open for
/// review. Touch targets are at least 44 points.
public struct MonarchScopeSelector: View {
    private let label: String
    @Binding private var selection: Set<String>
    private let groups: [MonarchScopeGroup]
    private let isLabelHidden: Bool
    private let isReadOnly: Bool
    private let pageSize: Int
    private let autoSelect: MonarchScopeSelectorAutoSelect?
    private let labels: MonarchScopeSelectorLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @State private var expanded: Set<String>
    @State private var limit: Int
    @State private var seen: Set<String>?

    /// A scope selector.
    ///
    /// - Parameters:
    ///   - label: The visible title, already localised, such as "Scopes". It also names the selector.
    ///   - selection: The selected scope strings.
    ///   - groups: The groups, in order.
    ///   - initiallyExpanded: The ids of the groups open at first.
    ///   - isLabelHidden: Hides the title; VoiceOver still reads it.
    ///   - isReadOnly: Disables every checkbox; groups still open for review.
    ///   - pageSize: How many groups show before "Show more", and how many each tap adds.
    ///   - autoSelect: Selects every scope of a group that loads with none selected.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String,
        selection: Binding<Set<String>>,
        groups: [MonarchScopeGroup],
        initiallyExpanded: Set<String> = [],
        isLabelHidden: Bool = false,
        isReadOnly: Bool = false,
        pageSize: Int = 10,
        autoSelect: MonarchScopeSelectorAutoSelect? = nil,
        labels: MonarchScopeSelectorLabels = MonarchScopeSelectorLabels()
    ) {
        self.label = label
        _selection = selection
        self.groups = groups
        self.isLabelHidden = isLabelHidden
        self.isReadOnly = isReadOnly
        self.pageSize = max(pageSize, 1)
        self.autoSelect = autoSelect
        self.labels = labels
        _expanded = State(initialValue: initiallyExpanded)
        _limit = State(initialValue: max(pageSize, 1))
    }

    private var text: MonarchScopeSelectorText { MonarchScopeSelectorText(labels: labels) }

    /// The content of the view.
    public var body: some View {
        let page = MonarchScopeSelectorTokens.page(groups, limit: limit, pageSize: pageSize)
        VStack(alignment: .leading, spacing: MonarchScopeSelectorTokens.groupSpacing) {
            if !isLabelHidden {
                Text(label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                    .accessibilityAddTraits(.isHeader)
            }
            if groups.isEmpty {
                Text(text.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(
                        theme.colors[role: MonarchScopeSelectorTokens.secondaryColor(isEnabled: isEnabled)]
                    )
            }
            ForEach(page.visible) { group in
                groupView(group)
            }
            if page.remaining > 0 {
                Button(text.showMore(page.next, page.remaining)) { limit += pageSize }
                    .buttonStyle(.monarch(.ghost, size: .sm))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
        .onChange(of: groups, initial: true) { _, new in
            let result = MonarchScopeSelectorTokens.autoSelect(new, selection: selection, seen: seen, mode: autoSelect)
            seen = result.seen
            if result.selection != selection { selection = result.selection }
        }
    }

    private func groupView(_ group: MonarchScopeGroup) -> some View {
        let isOpen = expanded.contains(group.id)
        let enabled = MonarchScopeSelectorTokens.enabledValues(group)
        let allOn = !enabled.isEmpty && enabled.allSatisfy { selection.contains($0) }
        let shape = RoundedRectangle(cornerRadius: MonarchCardTokens.cornerRadius, style: .continuous)
        // As the web: a bordered card per group, a chevron before the name, "Select all" at the end of the header
        // and a rule above the scopes.
        return VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .center, spacing: MonarchScopeSelectorTokens.iconSpacing) {
                Button {
                    if isOpen { expanded.remove(group.id) } else { expanded.insert(group.id) }
                } label: {
                    groupHeading(group, isOpen: isOpen)
                        .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityElement(children: .combine)
                .accessibilityValue(isOpen ? MonarchSectionCardStrings.expanded : MonarchSectionCardStrings.collapsed)
                if !isReadOnly && !enabled.isEmpty && !group.isLoading {
                    Button(allOn ? text.clearAll : text.selectAll) {
                        if allOn {
                            selection.subtract(enabled)
                        } else {
                            selection.formUnion(enabled)
                        }
                    }
                    .buttonStyle(.monarch(.ghost, size: .sm))
                    .accessibilityLabel("\(allOn ? text.clearAll : text.selectAll), \(group.label)")
                }
            }
            .padding(.horizontal, MonarchScopeSelectorTokens.iconSpacing)
            .padding(.vertical, MonarchScopeSelectorTokens.lineSpacing * 2)
            if isOpen {
                MonarchDivider()
                groupContent(group)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchCardTokens.background]))
        .overlay(
            shape.strokeBorder(theme.colors[role: MonarchCardTokens.border], lineWidth: MonarchCardTokens.borderWidth)
        )
        .clipShape(shape)
    }

    private func groupHeading(_ group: MonarchScopeGroup, isOpen: Bool) -> some View {
        let selected = MonarchScopeSelectorTokens.selectedCount(group, selection: selection)
        return HStack(alignment: .center, spacing: MonarchScopeSelectorTokens.iconSpacing) {
            MonarchIcon(systemName: isOpen ? "chevron.down" : "chevron.right", size: .sm)
                .foregroundStyle(theme.colors.textMuted)
                .frame(width: MonarchTokens.Space.space4)
            if let systemImage = group.systemImage {
                MonarchIcon(systemName: systemImage)
            }
            VStack(alignment: .leading, spacing: MonarchScopeSelectorTokens.lineSpacing) {
                Text(group.label)
                    .monarchTextStyle(.label)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                if let description = group.description {
                    secondary(description)
                }
                Text(MonarchScopeSelectorTokens.summary(group, selection: selection, text: text))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(
                        theme.colors[
                            role: MonarchScopeSelectorTokens.summaryColor(selected: selected, isEnabled: isEnabled)
                        ]
                    )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    @ViewBuilder private func groupContent(_ group: MonarchScopeGroup) -> some View {
        if group.isLoading {
            ProgressView()
                .accessibilityLabel(text.loading)
                .frame(maxWidth: .infinity)
                .padding(MonarchTokens.Space.space3)
        } else if !group.scopes.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(Array(group.scopes.enumerated()), id: \.element.id) { index, scope in
                    if index > 0 { MonarchDivider() }
                    Toggle(isOn: binding(scope.value)) {
                        VStack(alignment: .leading, spacing: MonarchScopeSelectorTokens.lineSpacing) {
                            if let label = scope.label {
                                Text(label)
                            } else {
                                Text(scope.value).monarchTextStyle(.code)
                            }
                            if let description = scope.description {
                                secondary(description)
                            }
                        }
                    }
                    .toggleStyle(.monarchCheckbox)
                    .disabled(isReadOnly || scope.isDisabled)
                    .padding(.horizontal, MonarchTokens.Space.space3)
                    .padding(.vertical, MonarchTokens.Space.space2)
                    .frame(minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
                }
            }
        }
    }

    private func secondary(_ string: String) -> some View {
        Text(string)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchScopeSelectorTokens.secondaryColor(isEnabled: isEnabled)])
    }

    private func binding(_ value: String) -> Binding<Bool> {
        Binding(
            get: { selection.contains(value) },
            set: { selection = MonarchScopeSelectorTokens.setting(value, isOn: $0, in: selection) }
        )
    }
}

#if DEBUG
    #Preview("MonarchScopeSelector") {
        let groups = [
            MonarchScopeGroup(
                id: "projects",
                label: "Projects API",
                description: "Example Inc.",
                systemImage: "folder",
                scopes: [
                    MonarchScopeOption("projects:read", description: "List and read projects."),
                    MonarchScopeOption("projects:write", label: "Write projects", description: "Create and update."),
                    MonarchScopeOption("projects:admin", isDisabled: true),
                ]
            ),
            MonarchScopeGroup(id: "reports", label: "Reports API", scopes: [], isLoading: true),
            MonarchScopeGroup(id: "billing", label: "Billing API", scopes: []),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchScopeSelector(
                    "Scopes",
                    selection: .constant(["projects:read"]),
                    groups: groups,
                    initiallyExpanded: ["projects"]
                )
                MonarchScopeSelector(
                    "Read only",
                    selection: .constant(["projects:read"]),
                    groups: groups,
                    initiallyExpanded: ["projects"],
                    isReadOnly: true,
                    pageSize: 1
                )
            }
        }
    }
#endif
