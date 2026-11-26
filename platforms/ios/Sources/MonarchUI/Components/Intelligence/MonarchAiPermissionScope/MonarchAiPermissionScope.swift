import SwiftUI

/// What an AI agent may see and change, app by app: each row names the app
/// and the part of it the access covers, and shows No access, Can read or
/// Can read and edit, or a None / Read / Write control when editable.
///
/// ```swift
/// MonarchAiPermissionScope([
///     MonarchAiPermissionScopeItem("Calendar", access: .read),
///     MonarchAiPermissionScopeItem("Drive", resources: "Shared projects only", access: .write),
/// ])
/// MonarchAiPermissionScope(scopes: $scopes, onManage: { showSettings() })
/// ```
///
/// Pass an array for a read-only summary, or a binding to let people change
/// each app's access with the standard segmented `Picker`.
///
/// Accessibility: a container named "What the AI can access". A read-only
/// row is one element read as the app and its resources, with the access
/// as its value; the access also has its own symbol (lock, eye, pencil), so
/// it never depends on colour. An editable row's control is named "Access
/// to <app>". The control moves under the name when the row is too narrow.
public struct MonarchAiPermissionScope: View {
    private let scopes: [MonarchAiPermissionScopeItem]
    private let binding: Binding<[MonarchAiPermissionScopeItem]>?
    private let labels: MonarchAiPermissionScopeLabels
    private let onManage: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    /// A read-only permission summary.
    ///
    /// - Parameters:
    ///   - scopes: One row per app, in display order.
    ///   - labels: Replaces the built-in strings.
    ///   - onManage: Adds a "Manage permissions" button that opens the full settings.
    public init(
        _ scopes: [MonarchAiPermissionScopeItem],
        labels: MonarchAiPermissionScopeLabels = MonarchAiPermissionScopeLabels(),
        onManage: (@MainActor () -> Void)? = nil
    ) {
        self.scopes = scopes
        self.binding = nil
        self.labels = labels
        self.onManage = onManage
    }

    /// An editable permission list.
    ///
    /// - Parameters:
    ///   - scopes: One row per app, in display order. Each row's control writes its access.
    ///   - labels: Replaces the built-in strings.
    ///   - onManage: Adds a "Manage permissions" button that opens the full settings.
    public init(
        scopes: Binding<[MonarchAiPermissionScopeItem]>,
        labels: MonarchAiPermissionScopeLabels = MonarchAiPermissionScopeLabels(),
        onManage: (@MainActor () -> Void)? = nil
    ) {
        self.scopes = scopes.wrappedValue
        self.binding = scopes
        self.labels = labels
        self.onManage = onManage
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiPermissionScopeTokens.sectionSpacing) {
            if scopes.isEmpty {
                Text(labels.empty)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
            } else {
                VStack(alignment: .leading, spacing: 0) {
                    ForEach(Array(scopes.enumerated()), id: \.element.id) { index, item in
                        if index > 0 {
                            Divider().overlay(theme.colors.borderSubtle)
                        }
                        row(item, index: index)
                            .padding(.vertical, MonarchAiPermissionScopeTokens.rowPadding)
                    }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(labels.label)
            }
            if let onManage {
                Button(labels.manage, action: onManage)
                    .buttonStyle(.monarchLink(.standalone))
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder private func row(_ item: MonarchAiPermissionScopeItem, index: Int) -> some View {
        if let binding {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchAiPermissionScopeTokens.rowSpacing) {
                    app(item)
                    picker(item, index: index, binding: binding).fixedSize()
                }
                VStack(alignment: .leading, spacing: MonarchAiPermissionScopeTokens.stackSpacing) {
                    app(item)
                    picker(item, index: index, binding: binding)
                }
            }
        } else {
            let spoken = MonarchAiPermissionScopeTokens.spoken(item, labels: labels)
            ViewThatFits(in: .horizontal) {
                HStack(spacing: MonarchAiPermissionScopeTokens.rowSpacing) {
                    app(item)
                    access(item.access).fixedSize()
                }
                VStack(alignment: .leading, spacing: MonarchAiPermissionScopeTokens.stackSpacing) {
                    app(item)
                    access(item.access)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(spoken.label)
            .accessibilityValue(spoken.value)
        }
    }

    private func app(_ item: MonarchAiPermissionScopeItem) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchAiPermissionScopeTokens.rowSpacing) {
            if item.iconURL != nil {
                MonarchFavicon(url: item.iconURL, name: item.app, size: .md)
                    .alignmentGuide(.firstTextBaseline) { $0[VerticalAlignment.center] }
            }
            VStack(alignment: .leading, spacing: MonarchAiPermissionScopeTokens.textSpacing) {
                Text(item.app)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors.textDefault)
                if let resources = item.resources, !resources.isEmpty {
                    Text(resources)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func access(_ level: MonarchAiAccessLevel) -> some View {
        // A pill as on the web: AI colours to read, warning colours to edit, neutral for no access.
        MonarchBadge(
            labels.access(level),
            tone: MonarchAiPermissionScopeTokens.accessTone(level),
            systemImage: MonarchAiPermissionScopeTokens.symbol(level)
        )
    }

    private func picker(
        _ item: MonarchAiPermissionScopeItem,
        index: Int,
        binding: Binding<[MonarchAiPermissionScopeItem]>
    ) -> some View {
        Picker(
            labels.control(item.app),
            selection: Binding(
                get: {
                    binding.wrappedValue.indices.contains(index) ? binding.wrappedValue[index].access : item.access
                },
                set: {
                    binding.wrappedValue = MonarchAiPermissionScopeTokens.updating(
                        binding.wrappedValue,
                        at: index,
                        to: $0
                    )
                }
            )
        ) {
            ForEach(MonarchAiAccessLevel.allCases, id: \.self) { level in
                Text(labels.option(level)).tag(level)
            }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
        .controlSize(.small)
        .accessibilityLabel(labels.control(item.app))
    }
}

#if DEBUG
    #Preview("MonarchAiPermissionScope") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiPermissionScope(
                    [
                        MonarchAiPermissionScopeItem("Calendar", access: .read),
                        MonarchAiPermissionScopeItem("Drive", resources: "Shared projects only", access: .write),
                        MonarchAiPermissionScopeItem("Mail", access: .none),
                    ],
                    onManage: {}
                )
                MonarchAiPermissionScope(
                    scopes: .constant([MonarchAiPermissionScopeItem("Calendar", access: .read)])
                )
                MonarchAiPermissionScope([])
            }
        }
    }
#endif
