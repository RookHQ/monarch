import SwiftUI

/// Where a person chooses which connected apps and data sources an assistant
/// may use in this conversation. Present it in a sheet.
///
/// ```swift
/// .sheet(isPresented: $showsConnectors) {
///     MonarchAiConnectorPicker(
///         connectors: $connectors,
///         onConnect: { id in startSignIn(for: id) }
///     )
/// }
/// ```
///
/// A searchable `List` in a `NavigationStack`: each connector has a switch,
/// its connection status, Sign in or Reconnect when the connection needs it,
/// and a disclosure group of its tools, each with an Allow, Ask first or
/// Never picker. A summary line counts the connectors on and the tools that
/// ask first. The picker only changes the bound values; the app signs in,
/// stores the choices and enforces them. A tool that can delete data is not
/// offered Allow unless `allowsDestructiveAutoApprove` is `true`. A Done
/// button closes the sheet when the picker is presented.
///
/// Accessibility: each switch is a standard toggle named "Use <name> in this
/// conversation"; the status and, when it cannot be turned on, the reason
/// are its hint and are also written in the row. Status and risk are words
/// as well as icons and colour. Row buttons are named with the action and the
/// connector ("Sign in: Calendar"). The number of results is announced when
/// the search changes.
public struct MonarchAiConnectorPicker: View {
    private let title: String?
    @Binding private var connectors: [MonarchAiConnector]
    private let allowsDestructiveAutoApprove: Bool
    private let isSearchable: Bool
    private let labels: MonarchAiConnectorPickerLabels
    private let onConnect: (@MainActor (String) -> Void)?
    private let onManage: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.isPresented) private var isPresented
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var expanded: Set<String>

    /// A connector picker, the content of a sheet.
    ///
    /// - Parameters:
    ///   - title: Replaces the built-in navigation title "Connectors".
    ///   - connectors: The connectors. The switches and permission pickers change them.
    ///   - allowsDestructiveAutoApprove: Lets a tool that can delete data be set to Allow.
    ///   - isSearchable: Shows the search field.
    ///   - expandedIDs: The ids of the connectors whose tools are open when the picker appears. None by default.
    ///   - labels: Replacements for the built-in strings.
    ///   - onConnect: Runs the sign-in for a connector that needs it or reconnects one with a problem, given its id.
    ///     Shows "Sign in" and "Reconnect".
    ///   - onManage: Opens a connector's settings, given its id. Shows "Manage" on every row.
    public init(
        _ title: String? = nil,
        connectors: Binding<[MonarchAiConnector]>,
        allowsDestructiveAutoApprove: Bool = false,
        isSearchable: Bool = true,
        expandedIDs: Set<String> = [],
        labels: MonarchAiConnectorPickerLabels = MonarchAiConnectorPickerLabels(),
        onConnect: (@MainActor (String) -> Void)? = nil,
        onManage: (@MainActor (String) -> Void)? = nil
    ) {
        self.title = title
        self._connectors = connectors
        self.allowsDestructiveAutoApprove = allowsDestructiveAutoApprove
        self.isSearchable = isSearchable
        _expanded = State(initialValue: expandedIDs)
        self.labels = labels
        self.onConnect = onConnect
        self.onManage = onManage
    }

    /// The content of the view.
    public var body: some View {
        NavigationStack {
            list
                .navigationTitle(title ?? labels.title ?? MonarchAiConnectorPickerStrings.title)
                #if os(iOS)
                    .navigationBarTitleDisplayMode(.inline)
                #endif
                .toolbar {
                    if isPresented {
                        ToolbarItem(placement: .confirmationAction) {
                            Button(labels.done ?? MonarchAiConnectorPickerStrings.done) { dismiss() }
                        }
                    }
                }
                .modifier(Search(isOn: isSearchable, query: $query, prompt: labels.searchPrompt))
        }
        .onChange(of: query) {
            let count = MonarchAiConnectorPickerTokens.filtered(connectors, query: query, locale: locale).count
            AccessibilityNotification.Announcement(
                labels.results?(count) ?? MonarchAiConnectorPickerStrings.results(count)
            ).post()
        }
    }

    private var list: some View {
        let visible = MonarchAiConnectorPickerTokens.filtered(connectors, query: query, locale: locale)
        return List {
            Group {
                if visible.isEmpty {
                    emptyState
                } else {
                    Section {
                        ForEach(visible) { connector in
                            row(connector)
                        }
                    } header: {
                        Text(
                            MonarchAiConnectorPickerTokens.summary(
                                MonarchAiConnectorPickerTokens.counts(connectors),
                                labels: labels
                            )
                        )
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .textCase(nil)
                    }
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
    }

    @ViewBuilder private var emptyState: some View {
        let searching = !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        if searching {
            MonarchEmptyState(
                labels.noResults?(query) ?? MonarchAiConnectorPickerStrings.noResults(query),
                systemImage: MonarchAiConnectorPickerTokens.searchSymbol,
                kind: .filtered,
                size: .sm,
                actionTitle: labels.clearSearch ?? MonarchAiConnectorPickerStrings.clearSearch
            ) {
                query = ""
            }
        } else {
            MonarchEmptyState(
                labels.empty ?? MonarchAiConnectorPickerStrings.empty,
                systemImage: MonarchAiConnectorPickerTokens.emptySymbol,
                size: .sm
            )
        }
    }

    private func row(_ connector: MonarchAiConnector) -> some View {
        let reason = MonarchAiConnectorPickerTokens.disabledReason(connector.status, labels: labels)
        let status = MonarchAiConnectorPickerTokens.statusText(connector, labels: labels)
        return VStack(alignment: .leading, spacing: MonarchAiConnectorPickerTokens.rowSpacing) {
            Toggle(isOn: enabled(connector.id)) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiConnectorPickerTokens.iconSpacing) {
                    MonarchFavicon(url: connector.iconURL, name: connector.name)
                    VStack(alignment: .leading, spacing: MonarchAiConnectorPickerTokens.textSpacing) {
                        Text(connector.name)
                            .monarchTextStyle(.bodyStrong)
                            .foregroundStyle(theme.colors.textDefault)
                        if let description = connector.description, !description.isEmpty {
                            Text(description)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors.textMuted)
                        }
                    }
                }
            }
            .disabled(connector.status != .connected)
            .accessibilityLabel(
                labels.enable?(connector.name) ?? MonarchAiConnectorPickerStrings.enable(connector.name)
            )
            .accessibilityHint([status, reason].compactMap { $0 }.joined(separator: ". "))
            VStack(alignment: .leading, spacing: MonarchAiConnectorPickerTokens.textSpacing) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiConnectorPickerTokens.iconSpacing) {
                    MonarchIcon(systemName: MonarchAiConnectorPickerTokens.statusSymbol(connector.status), size: .sm)
                        .foregroundStyle(
                            theme.colors[role: MonarchAiConnectorPickerTokens.statusRole(connector.status)]
                        )
                    Text(status)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if let reason {
                    Text(reason)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
            .accessibilityHidden(true)
            actions(connector)
            if !connector.tools.isEmpty {
                tools(connector)
            }
        }
        .padding(MonarchAiConnectorPickerTokens.rowSpacing + MonarchAiConnectorPickerTokens.textSpacing)
        .background(theme.colors.bgSurface, in: MonarchShapes.container)
        .overlay(
            // An open connector has the AI-tinted border, as on the web.
            MonarchShapes.container.strokeBorder(
                expanded.contains(connector.id) ? theme.colors.aiBorder : theme.colors.borderSubtle,
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .listRowSeparator(.hidden)
        .listRowBackground(Color.clear)
    }

    @ViewBuilder private func actions(_ connector: MonarchAiConnector) -> some View {
        let connectTitle =
            onConnect == nil ? nil : MonarchAiConnectorPickerTokens.connectTitle(connector.status, labels: labels)
        if connectTitle != nil || onManage != nil {
            HStack(spacing: MonarchAiConnectorPickerTokens.buttonSpacing) {
                if let connectTitle, let onConnect {
                    MonarchButton(connectTitle, variant: .secondary, size: .sm) { onConnect(connector.id) }
                        .accessibilityLabel(actionLabel(connectTitle, connector.name))
                }
                if let onManage {
                    let manage = labels.manage ?? MonarchAiConnectorPickerStrings.manage
                    MonarchButton(manage, variant: .ghost, size: .sm) { onManage(connector.id) }
                        .accessibilityLabel(actionLabel(manage, connector.name))
                }
            }
        }
    }

    private func tools(_ connector: MonarchAiConnector) -> some View {
        let title = labels.tools?(connector.tools.count) ?? MonarchAiConnectorPickerStrings.tools(connector.tools.count)
        return DisclosureGroup(
            isExpanded: Binding(
                get: { expanded.contains(connector.id) },
                set: { isOpen in
                    if isOpen { expanded.insert(connector.id) } else { expanded.remove(connector.id) }
                }
            ),
            content: {
                ForEach(connector.tools) { tool in
                    toolRow(tool, in: connector)
                }
            },
            label: {
                Text(title)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityLabel(actionLabel(title, connector.name))
            }
        )
        .disclosureGroupStyle(.monarch)
    }

    private func toolRow(_ tool: MonarchAiConnectorTool, in connector: MonarchAiConnector) -> some View {
        let isLocked = MonarchAiConnectorPickerTokens.isLocked(
            tool,
            allowsDestructiveAutoApprove: allowsDestructiveAutoApprove
        )
        let risk = labels.risk?(tool.risk) ?? MonarchAiConnectorPickerStrings.risk(tool.risk)
        return VStack(alignment: .leading, spacing: MonarchAiConnectorPickerTokens.textSpacing) {
            MonarchFlowLayout(spacing: MonarchAiConnectorPickerTokens.iconSpacing) {
                Text(tool.name)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textDefault)
                MonarchBadge(
                    risk,
                    tone: MonarchAiConnectorPickerTokens.tone(tool.risk),
                    systemImage: MonarchAiConnectorPickerTokens.riskSymbol(tool.risk)
                )
            }
            .accessibilityElement(children: .combine)
            if let description = tool.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
            permissionChoices(tool, in: connector)
            if isLocked {
                HStack(alignment: .firstTextBaseline, spacing: MonarchAiConnectorPickerTokens.iconSpacing) {
                    MonarchIcon(systemName: MonarchAiConnectorPickerTokens.lockSymbol, size: .sm)
                    Text(labels.destructiveLocked ?? MonarchAiConnectorPickerStrings.destructiveLocked)
                        .monarchTextStyle(.caption)
                }
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityElement(children: .combine)
            }
        }
    }

    /// Allow, Ask first and Never as a row of radios, as on the web; a choice that is not offered shows disabled.
    private func permissionChoices(_ tool: MonarchAiConnectorTool, in connector: MonarchAiConnector) -> some View {
        let offered = MonarchAiConnectorPickerTokens.options(
            tool,
            allowsDestructiveAutoApprove: allowsDestructiveAutoApprove
        )
        let selection = permission(tool.id, in: connector.id)
        let isActive = MonarchAiConnectorPickerTokens.isActive(connector)
        return ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchAiConnectorPickerTokens.buttonSpacing) { choices(offered, selection, isActive) }
            VStack(alignment: .leading, spacing: 0) { choices(offered, selection, isActive) }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(
            labels.permissionLabel?(tool.name) ?? MonarchAiConnectorPickerStrings.permissionLabel(tool.name)
        )
    }

    @ViewBuilder private func choices(
        _ offered: [MonarchAiConnectorToolPermission],
        _ selection: Binding<MonarchAiConnectorToolPermission>,
        _ isActive: Bool
    ) -> some View {
        ForEach(MonarchAiConnectorToolPermission.allCases, id: \.self) { option in
            Toggle(
                labels.permission?(option) ?? MonarchAiConnectorPickerStrings.permission(option),
                isOn: Binding(
                    get: { selection.wrappedValue == option },
                    set: { if $0 { selection.wrappedValue = option } }
                )
            )
            .toggleStyle(.monarchRadio)
            .fixedSize()
            .disabled(!isActive || !offered.contains(option))
        }
    }

    private func actionLabel(_ action: String, _ name: String) -> String {
        labels.actionLabel?(action, name) ?? MonarchAiConnectorPickerStrings.actionLabel(action, name)
    }

    private func enabled(_ id: String) -> Binding<Bool> {
        Binding(
            get: { connectors.first { $0.id == id }?.isEnabled ?? false },
            set: { connectors = MonarchAiConnectorPickerTokens.setting(connectors, id: id, isEnabled: $0) }
        )
    }

    private func permission(_ tool: String, in id: String) -> Binding<MonarchAiConnectorToolPermission> {
        Binding(
            get: {
                connectors.first { $0.id == id }?.tools.first { $0.id == tool }?.permission ?? .ask
            },
            set: {
                connectors = MonarchAiConnectorPickerTokens.setting(
                    connectors,
                    id: id,
                    tool: tool,
                    to: $0,
                    allowsDestructiveAutoApprove: allowsDestructiveAutoApprove
                )
            }
        )
    }
}

/// Adds the system search field when the picker is searchable.
private struct Search: ViewModifier {
    let isOn: Bool
    @Binding var query: String
    let prompt: String?

    func body(content: Content) -> some View {
        if isOn {
            content.monarchSearchable(
                text: $query,
                prompt: Text(prompt ?? MonarchAiConnectorPickerStrings.searchPrompt)
            )
        } else {
            content
        }
    }
}

#if DEBUG
    #Preview("MonarchAiConnectorPicker") {
        MonarchPreviewMatrix {
            MonarchAiConnectorPicker(
                connectors: .constant([
                    MonarchAiConnector(
                        id: "calendar",
                        name: "Calendar",
                        description: "Read and create events.",
                        status: .connected,
                        account: "ada@example.com",
                        isEnabled: true,
                        tools: [
                            MonarchAiConnectorTool(id: "list", name: "List events", risk: .read, permission: .allow),
                            MonarchAiConnectorTool(
                                id: "delete",
                                name: "Delete event",
                                risk: .destructive,
                                permission: .ask
                            ),
                        ]
                    ),
                    MonarchAiConnector(id: "drive", name: "Drive", status: .needsSignIn, isEnabled: false),
                ]),
                onConnect: { _ in }
            )
        }
    }
#endif
