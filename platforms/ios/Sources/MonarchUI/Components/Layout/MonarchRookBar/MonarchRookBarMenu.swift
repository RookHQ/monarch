import SwiftUI

/// The full-height menu of a ``MonarchRookBar``: each entry as a collapsible group of groups and links, then the
/// account controls.
struct MonarchRookBarMenu: View {
    let brand: String
    let items: [MonarchRookBarItem]
    let account: MonarchRookBarAccount?
    let labels: MonarchRookBarLabels
    let onSignOut: (@MainActor () -> Void)?
    let onClose: () -> Void

    @State private var expanded: Set<String> = []
    @Environment(\.monarchTheme) private var theme
    @Environment(\.openURL) private var openURL

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(items) { item in
                        if item.groups.isEmpty {
                            Button {
                                open(item.destination)
                            } label: {
                                Text(item.label)
                                    .monarchTextStyle(.bodyStrong)
                                    .foregroundStyle(theme.colors.textDefault)
                            }
                            .accessibilityAddTraits(.isLink)
                        } else {
                            DisclosureGroup(isExpanded: binding(item.id)) {
                                ForEach(item.groups) { group in
                                    sectionHeader(group)
                                    ForEach(group.links) { link in row(link) }
                                }
                            } label: {
                                Text(item.label)
                                    .monarchTextStyle(.bodyStrong)
                                    .foregroundStyle(theme.colors.textDefault)
                            }
                        }
                    }
                }
                if let account { accountSection(account) }
            }
            .navigationTitle(brand)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: onClose) {
                        Label(
                            labels.closeMenu ?? MonarchRookBarStrings.closeMenu,
                            systemImage: MonarchRookBarTokens.closeSymbol
                        )
                        .labelStyle(.iconOnly)
                    }
                }
            }
        }
    }

    private func binding(_ id: String) -> Binding<Bool> {
        Binding(
            get: { expanded.contains(id) },
            set: { isOn in
                if isOn { expanded.insert(id) } else { expanded.remove(id) }
            }
        )
    }

    private func open(_ url: URL?) {
        onClose()
        if let url { openURL(url) }
    }

    @ViewBuilder private func sectionHeader(_ group: MonarchRookBarGroup) -> some View {
        if group.title != nil || group.description != nil {
            VStack(alignment: .leading, spacing: MonarchRookBarTokens.rowSpacing) {
                if let title = group.title {
                    Text(title)
                        .monarchTextStyle(.label, weight: .semibold)
                        .foregroundStyle(theme.colors.textDefault)
                }
                if let description = group.description {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: MonarchRookBarTokens.supportingColor])
                }
            }
            .accessibilityElement(children: .combine)
            .accessibilityAddTraits(.isHeader)
        }
    }

    private func row(_ link: MonarchRookBarLink) -> some View {
        Button {
            open(link.destination)
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: MonarchRookBarTokens.rowSpacing) {
                    Text(link.label)
                        .monarchTextStyle(.body)
                        .foregroundStyle(theme.colors.textDefault)
                    if let description = link.description {
                        Text(description)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: MonarchRookBarTokens.supportingColor])
                    }
                    if let meta = link.meta {
                        Text(meta)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: MonarchRookBarTokens.supportingColor])
                    }
                }
                Spacer(minLength: 0)
                MonarchIcon(
                    systemName: link.isExternal
                        ? MonarchRookBarTokens.externalSymbol : MonarchRookBarTokens.chevronSymbol,
                    size: .sm
                )
                .flipsForRightToLeftLayoutDirection(true)
                .foregroundStyle(theme.colors[role: MonarchRookBarTokens.supportingColor])
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(MonarchRookBarTokens.accessibilityLabel(link))
        .accessibilityAddTraits([.isButton, .isLink])
    }

    @ViewBuilder private func accountSection(_ account: MonarchRookBarAccount) -> some View {
        Section {
            switch account {
            case .signedOut(let signInURL, let signUpURL):
                if let signUpURL {
                    Button(labels.signUp ?? MonarchRookBarStrings.signUp) { open(signUpURL) }
                        .buttonStyle(.monarch(.primary, fullWidth: true))
                }
                if let signInURL {
                    Button(labels.signIn ?? MonarchRookBarStrings.signIn) { open(signInURL) }
                        .buttonStyle(.monarch(.secondary, fullWidth: true))
                }
            case .signedIn(let name, let dashboardURL, let accountURL):
                Text(name).monarchTextStyle(.bodyStrong).foregroundStyle(theme.colors.textDefault)
                if let dashboardURL {
                    Button(labels.dashboard ?? MonarchRookBarStrings.dashboard) { open(dashboardURL) }
                }
                if let accountURL {
                    Button(labels.account ?? MonarchRookBarStrings.account) { open(accountURL) }
                }
                if let onSignOut {
                    Button(labels.signOut ?? MonarchRookBarStrings.signOut, role: .destructive) {
                        onClose()
                        onSignOut()
                    }
                }
            }
        }
    }
}
