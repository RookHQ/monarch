import SwiftUI

/// The searchable list of ``MonarchWorkspaceSwitcher``, shown in a sheet for a long list.
struct MonarchWorkspaceSwitcherList: View {
    let workspaces: [MonarchWorkspaceOption]
    let selection: String?
    let isLoading: Bool
    let canCreate: Bool
    let manageURL: URL?
    let labels: MonarchWorkspaceSwitcherLabels
    let onChoose: @MainActor (String) -> Void
    let onCreate: @MainActor () -> Void
    let onCancel: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""

    private typealias Tokens = MonarchWorkspaceSwitcherTokens

    var body: some View {
        let results = Tokens.filter(workspaces, query: query)
        VStack(alignment: .leading, spacing: 0) {
            searchField
            divider
            VStack(alignment: .leading, spacing: theme.spacing.space1) {
                ForEach(results) { workspace in
                    row(workspace, isSelected: workspace.id == selection)
                }
                if isLoading {
                    HStack(spacing: Tokens.spacing) {
                        ProgressView()
                        Text(labels.loading ?? MonarchWorkspaceSwitcherStrings.loading).monarchTextStyle(.body)
                    }
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(theme.spacing.space2)
                    .accessibilityElement(children: .combine)
                } else if results.isEmpty {
                    Text(labels.empty ?? MonarchWorkspaceSwitcherStrings.empty)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textMuted)
                        .padding(theme.spacing.space2)
                }
            }
            .padding(theme.spacing.space1)
            if canCreate || manageURL != nil {
                divider
                VStack(alignment: .leading, spacing: 0) {
                    if canCreate {
                        Button(action: onCreate) {
                            actionLabel(labels.create ?? MonarchWorkspaceSwitcherStrings.create, Tokens.createSymbol)
                        }
                        .buttonStyle(.plain)
                    }
                    if let manageURL {
                        Link(destination: manageURL) {
                            actionLabel(labels.manage ?? MonarchWorkspaceSwitcherStrings.manage, Tokens.manageSymbol)
                        }
                    }
                }
                .padding(theme.spacing.space1)
            }
        }
        .frame(width: theme.spacing.space24 * 3)
        .background(theme.colors.bgSurface)
    }

    private var divider: some View {
        Rectangle().fill(theme.colors.borderSubtle).frame(height: 1)
    }

    /// The search row: a magnifier and a plain field, as the web popover's.
    private var searchField: some View {
        HStack(spacing: theme.spacing.space2) {
            MonarchIcon(systemName: "magnifyingglass", size: .sm)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityHidden(true)
            TextField(labels.search ?? MonarchWorkspaceSwitcherStrings.search, text: $query)
                .textFieldStyle(.plain)
                .monarchFieldText()
                .autocorrectionDisabled()
        }
        .padding(.horizontal, theme.spacing.space3)
        .frame(minHeight: 44)
    }

    private func actionLabel(_ title: String, _ symbol: String) -> some View {
        HStack(spacing: theme.spacing.space2) {
            MonarchIcon(systemName: symbol, size: .sm)
                .foregroundStyle(theme.colors.textMuted)
            Text(title)
                .monarchTextStyle(.body)
                .foregroundStyle(theme.colors.textDefault)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, theme.spacing.space2)
        .monarchMinimumHitTarget()
        .contentShape(Rectangle())
    }

    private func row(_ workspace: MonarchWorkspaceOption, isSelected: Bool) -> some View {
        Button {
            onChoose(workspace.id)
        } label: {
            HStack(spacing: Tokens.spacing) {
                MonarchWorkspaceSwitcherLogo(workspace: workspace, labels: labels)
                Text(workspace.name)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let handle = Tokens.handleText(workspace) {
                    Text(handle)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if isSelected {
                    MonarchIcon(systemName: Tokens.checkSymbol, size: .sm)
                        .foregroundStyle(theme.colors.accentStrong)
                }
            }
            .padding(.horizontal, theme.spacing.space2)
            .background(isSelected ? theme.colors.bgMuted : Color.clear, in: MonarchShapes.control)
            .monarchMinimumHitTarget()
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.spokenName(workspace))
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

extension View {
    /// An inline navigation title on iOS; other platforms have one style.
    @ViewBuilder
    fileprivate func monarchWorkspaceSwitcherInlineTitle() -> some View {
        #if os(iOS)
            navigationBarTitleDisplayMode(.inline)
        #else
            self
        #endif
    }
}
