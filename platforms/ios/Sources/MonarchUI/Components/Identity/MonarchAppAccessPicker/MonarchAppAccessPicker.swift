import SwiftUI

/// A checklist of apps to grant a member, with "Select all" and a count.
///
/// ```swift
/// MonarchAppAccessPicker(
///     [
///         MonarchAppAccessOption(id: "mail", name: "Mail", description: "Email for the team."),
///         MonarchAppAccessOption(id: "docs", name: "Docs", badge: "Beta"),
///         MonarchAppAccessOption(id: "home", name: "Home", isDisabled: true),
///     ],
///     selection: $apps,
///     helper: "Changes apply the next time they sign in."
/// )
/// ```
///
/// For inviting someone to a workspace or editing a member's access. Each
/// app is a standard `Toggle` in the Monarch checkbox style, with its logo,
/// name, an optional badge and a description, on a row that takes the
/// accent colours when selected. "Select all" and "Clear all" change only
/// the apps that are not disabled. Disable the whole picker with
/// `.disabled(true)`, for example while saving. ``MonarchAppAccessSummary``
/// shows the result in one line, for a member list.
///
/// Accessibility: the picker is a container named by its label. Each app is
/// a standard toggle read with its name, badge and description; the logo is
/// decorative. The count is text above the list and is announced after
/// "Select all" or "Clear all". While loading, the placeholder rows are one
/// element read as "Loading apps". Rows are at least 44 points high.
@_spi(Experimental) public struct MonarchAppAccessPicker: View {
    private let apps: [MonarchAppAccessOption]
    @Binding private var selection: Set<String>
    private let label: String?
    private let helper: String?
    private let isLoading: Bool
    private let isLabelHidden: Bool
    private let showsBulkActions: Bool
    private let labels: MonarchAppAccessPickerLabels

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// An app access picker.
    ///
    /// - Parameters:
    ///   - apps: The apps that can be granted, in order.
    ///   - selection: The ids of the granted apps.
    ///   - label: The group's label, already localised. "App access" by default.
    ///   - helper: Help text under the list, such as when the change takes effect.
    ///   - isLoading: Shows placeholder rows instead of the apps.
    ///   - isLabelHidden: Hides the label visually; it still names the group for VoiceOver.
    ///   - showsBulkActions: Shows the count and the "Select all" or "Clear all" button.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ apps: [MonarchAppAccessOption],
        selection: Binding<Set<String>>,
        label: String? = nil,
        helper: String? = nil,
        isLoading: Bool = false,
        isLabelHidden: Bool = false,
        showsBulkActions: Bool = true,
        labels: MonarchAppAccessPickerLabels = MonarchAppAccessPickerLabels()
    ) {
        self.apps = apps
        _selection = selection
        self.label = label
        self.helper = helper
        self.isLoading = isLoading
        self.isLabelHidden = isLabelHidden
        self.showsBulkActions = showsBulkActions
        self.labels = labels
    }

    private var title: String { label ?? labels.label ?? MonarchAppAccessPickerStrings.label }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAppAccessPickerTokens.stackSpacing) {
            if !isLabelHidden {
                Text(title)
                    .monarchTextStyle(.label)
                    .foregroundStyle(isEnabled ? theme.colors.textDefault : theme.colors.textDisabled)
                    .accessibilityHidden(true)
            }
            switch MonarchAppAccessPickerTokens.content(isLoading: isLoading, count: apps.count) {
            case .loading:
                loadingRows
            case .empty:
                Text(labels.empty ?? MonarchAppAccessPickerStrings.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            case .rows:
                if showsBulkActions { toolbar }
                VStack(alignment: .leading, spacing: MonarchAppAccessPickerTokens.rowSpacing) {
                    ForEach(apps) { row($0) }
                }
            }
            if let helper, !helper.isEmpty {
                Text(helper)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(isEnabled ? theme.colors.textMuted : theme.colors.textDisabled)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
    }

    private var toolbar: some View {
        let allSelected = MonarchAppAccessPickerTokens.isAllSelected(apps, selection: selection)
        let count = MonarchAppAccessPickerTokens.countText(
            selected: MonarchAppAccessPickerTokens.selectedCount(apps, selection: selection),
            total: apps.count,
            labels: labels
        )
        return HStack(spacing: MonarchAppAccessPickerTokens.stackSpacing) {
            Text(count)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .frame(maxWidth: .infinity, alignment: .leading)
            MonarchButton(
                allSelected
                    ? labels.clearAll ?? MonarchAppAccessPickerStrings.clearAll
                    : labels.selectAll ?? MonarchAppAccessPickerStrings.selectAll,
                variant: .ghost,
                size: .sm
            ) {
                let next = MonarchAppAccessPickerTokens.toggledAll(apps, selection: selection)
                selection = next
                AccessibilityNotification.Announcement(
                    MonarchAppAccessPickerTokens.countText(
                        selected: MonarchAppAccessPickerTokens.selectedCount(apps, selection: next),
                        total: apps.count,
                        labels: labels
                    )
                )
                .post()
            }
            .disabled(MonarchAppAccessPickerTokens.changeableIDs(apps).isEmpty)
        }
    }

    private func row(_ app: MonarchAppAccessOption) -> some View {
        let isSelected = selection.contains(app.id)
        let shape = RoundedRectangle(cornerRadius: MonarchAppAccessPickerTokens.radius, style: .continuous)
        return Toggle(isOn: binding(app)) {
            HStack(spacing: MonarchAppAccessPickerTokens.logoSpacing) {
                MonarchAvatar(
                    url: app.logoURL,
                    name: app.name,
                    seed: app.id,
                    shape: .square,
                    size: .md,
                    isDecorative: true
                )
                VStack(alignment: .leading, spacing: MonarchAppAccessPickerTokens.textSpacing) {
                    MonarchFlowLayout(spacing: MonarchAppAccessPickerTokens.badgeSpacing) {
                        Text(app.name).monarchTextStyle(.label)
                        if let badge = app.badge, !badge.isEmpty {
                            MonarchBadge(badge)
                        }
                    }
                    if let description = app.description, !description.isEmpty {
                        Text(description)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(
                                theme.colors[
                                    role: MonarchAppAccessPickerTokens.descriptionColor(
                                        isEnabled: isEnabled && !app.isDisabled
                                    )
                                ]
                            )
                    }
                }
            }
        }
        .toggleStyle(.monarchCheckbox)
        .disabled(app.isDisabled)
        .padding(.horizontal, MonarchAppAccessPickerTokens.rowHorizontalPadding)
        .padding(.vertical, MonarchAppAccessPickerTokens.rowVerticalPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchAppAccessPickerTokens.rowFill(isSelected: isSelected)]))
        .overlay {
            if let border = MonarchAppAccessPickerTokens.rowBorder(isSelected: isSelected) {
                shape.strokeBorder(theme.colors[role: border], lineWidth: MonarchShapes.borderWidth)
            }
        }
    }

    private var loadingRows: some View {
        let placeholder = MonarchAppAccessOption(
            id: "placeholder",
            name: "App name",
            description: "What the app is for"
        )
        return VStack(alignment: .leading, spacing: MonarchAppAccessPickerTokens.rowSpacing) {
            ForEach(0..<MonarchAppAccessPickerTokens.loadingRowCount, id: \.self) { _ in
                row(placeholder)
            }
        }
        .redacted(reason: .placeholder)
        .allowsHitTesting(false)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.loading ?? MonarchAppAccessPickerStrings.loading)
    }

    private func binding(_ app: MonarchAppAccessOption) -> Binding<Bool> {
        Binding(
            get: { selection.contains(app.id) },
            set: { selection = MonarchAppAccessPickerTokens.setting(app.id, isOn: $0, in: selection) }
        )
    }
}

#if DEBUG
    #Preview("MonarchAppAccessPicker") {
        let apps = [
            MonarchAppAccessOption(id: "mail", name: "Mail", description: "Email for the team."),
            MonarchAppAccessOption(id: "docs", name: "Docs", badge: "Beta"),
            MonarchAppAccessOption(id: "home", name: "Home", description: "Everyone has it.", isDisabled: true),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAppAccessPicker(
                    apps,
                    selection: .constant(["mail", "home"]),
                    helper: "Applies at next sign-in."
                )
                MonarchAppAccessPicker(apps, selection: .constant([]), isLoading: true)
                MonarchAppAccessSummary(apps, granted: ["mail", "docs", "home"])
            }
        }
    }
#endif
