import MonarchTokens
import SwiftUI

/// The searchable list of ``MonarchSpaceSwitcher``, shown in a sheet.
struct MonarchSpaceSwitcherList: View {
    let spaces: [MonarchSpaceOption]
    let selection: String?
    let allowsAll: Bool
    let isLoading: Bool
    let canCreate: Bool
    let manageURL: URL?
    let labels: MonarchSpaceSwitcherLabels
    let onChoose: @MainActor (String?) -> Void
    let onCreate: @MainActor () -> Void
    let onCancel: @MainActor () -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""

    private typealias Tokens = MonarchSpaceSwitcherTokens

    var body: some View {
        NavigationStack {
            list
                .navigationTitle(labels.label ?? MonarchSpaceSwitcherStrings.label)
                .monarchInlineNavigationTitle()
                #if os(iOS)
                    .searchable(
                        text: $query,
                        placement: .navigationBarDrawer(displayMode: .always),
                        prompt: Text(labels.search ?? MonarchSpaceSwitcherStrings.search)
                    )
                #else
                    .searchable(text: $query, prompt: Text(labels.search ?? MonarchSpaceSwitcherStrings.search))
                #endif
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(labels.cancel ?? MonarchSpaceSwitcherStrings.cancel, action: onCancel)
                    }
                }
        }
    }

    private var list: some View {
        let results = Tokens.filter(spaces, query: query)
        return List {
            Group {
                Section {
                    if Tokens.showsAll(allowsAll: allowsAll, query: query) {
                        row(
                            title: labels.all ?? MonarchSpaceSwitcherStrings.all,
                            description: nil,
                            systemImage: Tokens.allSymbol,
                            isSelected: selection == nil
                        ) { onChoose(nil) }
                    }
                    ForEach(results) { space in
                        row(
                            title: space.name,
                            description: space.description,
                            systemImage: space.systemImage,
                            isSelected: space.id == selection
                        ) { onChoose(space.id) }
                    }
                    if isLoading {
                        HStack(spacing: Tokens.spacing) {
                            ProgressView()
                            Text(labels.loading ?? MonarchSpaceSwitcherStrings.loading).monarchTextStyle(.body)
                        }
                        .foregroundStyle(theme.colors.textMuted)
                        .accessibilityElement(children: .combine)
                    } else if results.isEmpty {
                        Text(labels.empty ?? MonarchSpaceSwitcherStrings.empty)
                            .monarchTextStyle(.body)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
                if canCreate || manageURL != nil {
                    Section {
                        if canCreate {
                            Button(action: onCreate) {
                                Label(
                                    labels.create ?? MonarchSpaceSwitcherStrings.create,
                                    systemImage: Tokens.createSymbol
                                )
                                .monarchTextStyle(.body)
                                .foregroundStyle(theme.colors.textDefault)
                            }
                            .monarchMinimumHitTarget()
                        }
                        if let manageURL {
                            Link(destination: manageURL) {
                                Label(
                                    labels.manage ?? MonarchSpaceSwitcherStrings.manage,
                                    systemImage: Tokens.manageSymbol
                                )
                                .monarchTextStyle(.body)
                                .foregroundStyle(theme.colors.textDefault)
                            }
                            .monarchMinimumHitTarget()
                        }
                    }
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
    }

    private func row(
        title: String,
        description: String?,
        systemImage: String?,
        isSelected: Bool,
        action: @escaping @MainActor () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: Tokens.spacing) {
                if let systemImage {
                    MonarchIcon(systemName: systemImage)
                        .foregroundStyle(theme.colors.textMuted)
                }
                VStack(alignment: .leading, spacing: Tokens.textSpacing) {
                    Text(title)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                    if let description, !description.isEmpty {
                        Text(description)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if isSelected {
                    MonarchIcon(systemName: Tokens.checkSymbol, size: .sm)
                        .foregroundStyle(theme.colors.accentStrong)
                }
            }
            .monarchMinimumHitTarget()
            .background {
                if isSelected {
                    RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                        .fill(theme.colors.bgMuted)
                        .padding(.horizontal, -Tokens.spacing)
                }
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.rowLabel(title: title, description: description))
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}
