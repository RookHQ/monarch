import SwiftUI

/// Switches between the workspaces a person belongs to: a button with the
/// workspace's logo, name and handle that opens a menu for a short list or a
/// sheet with a searchable list for a long one, with Create workspace and
/// All workspaces.
///
/// ```swift
/// MonarchWorkspaceSwitcher(
///     selection: $workspaceID,
///     workspaces: workspaces,
///     manageURL: URL(string: "https://id.example.com/workspaces"),
///     onCreate: { showNewWorkspace = true }
/// )
/// ```
///
/// Up to five loaded workspaces are a system `Menu` with an inline `Picker`,
/// which checks the current one; more, or a list still loading, open a
/// sheet with a `List`, the system search field and the logos. The app
/// supplies the workspaces and does the switch when `selection` changes;
/// choosing the current workspace again changes nothing. Create workspace
/// runs `onCreate` (after closing the sheet); All workspaces opens
/// `manageURL` with the environment's `openURL`.
///
/// Accessibility: the button is named "Workspace" and its value is the
/// workspace's name and handle, also when only the logo shows. In the sheet
/// each row is a button named by the workspace and its handle, the current
/// one with the selected trait and a check mark; in the menu the picker
/// gives the same. Logos are decorative.
public struct MonarchWorkspaceSwitcher: View {
    @Binding private var selection: String?
    private let workspaces: [MonarchWorkspaceOption]
    private let size: MonarchButtonSize
    private let isLoading: Bool
    private let isIconOnly: Bool
    private let manageURL: URL?
    private let labels: MonarchWorkspaceSwitcherLabels
    private let onCreate: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @State private var isPresented = false
    @Environment(\.monarchStartsPresented) private var startsPresented

    private typealias Tokens = MonarchWorkspaceSwitcherTokens

    /// A workspace switcher.
    ///
    /// - Parameters:
    ///   - selection: The current workspace id, `nil` when none is chosen.
    ///   - workspaces: The workspaces this person belongs to, in display order.
    ///   - size: The height and padding of the button.
    ///   - isLoading: The workspaces are still loading; the sheet says so.
    ///   - isIconOnly: Shows only the logo and the chevron, for a narrow bar. The name is still spoken.
    ///   - manageURL: Adds "All workspaces", which opens this URL.
    ///   - labels: Replacements for the built-in strings.
    ///   - onCreate: Adds "Create workspace", which runs this.
    public init(
        selection: Binding<String?>,
        workspaces: [MonarchWorkspaceOption],
        size: MonarchButtonSize = .md,
        isLoading: Bool = false,
        isIconOnly: Bool = false,
        manageURL: URL? = nil,
        labels: MonarchWorkspaceSwitcherLabels = MonarchWorkspaceSwitcherLabels(),
        onCreate: (@MainActor () -> Void)? = nil
    ) {
        _selection = selection
        self.workspaces = workspaces
        self.size = size
        self.isLoading = isLoading
        self.isIconOnly = isIconOnly
        self.manageURL = manageURL
        self.labels = labels
        self.onCreate = onCreate
    }

    /// The content of the view.
    public var body: some View {
        let current = Tokens.current(selection, in: workspaces)
        Button {
            isPresented = true
        } label: {
            triggerLabel(current)
        }
        .buttonStyle(.monarch(.ghost, size: size))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(labels.label ?? MonarchWorkspaceSwitcherStrings.label)
        .accessibilityValue(current.map(Tokens.spokenName) ?? Tokens.triggerName(nil, labels: labels))
        .accessibilityAddTraits(.isButton)
        .onAppear { if startsPresented { isPresented = true } }
        .popover(isPresented: $isPresented, arrowEdge: .top) {
            MonarchWorkspaceSwitcherList(
                workspaces: workspaces,
                selection: current?.id,
                isLoading: isLoading,
                canCreate: onCreate != nil,
                manageURL: manageURL,
                labels: labels,
                onChoose: choose,
                onCreate: create,
                onCancel: { isPresented = false }
            )
            .presentationCompactAdaptation(.popover)
        }
    }

    private func triggerLabel(_ current: MonarchWorkspaceOption?) -> some View {
        HStack(spacing: Tokens.spacing) {
            MonarchWorkspaceSwitcherLogo(workspace: current, labels: labels)
            if !isIconOnly {
                // The name over the handle, as on the web, and the chevron after them.
                VStack(alignment: .leading, spacing: 0) {
                    Text(Tokens.triggerName(current, labels: labels))
                        .monarchTextStyle(.label)
                        .fontWeight(.semibold)
                        .lineLimit(1)
                    if let handle = Tokens.handleText(current) {
                        Text(handle)
                            .monarchTextStyle(.caption)
                            .lineLimit(1)
                            .foregroundStyle(theme.colors.textMuted)
                    }
                }
            }
            MonarchIcon(systemName: Tokens.chevronSymbol, size: .sm)
                .foregroundStyle(theme.colors.textMuted)
        }
    }

    private func choose(_ id: String) {
        isPresented = false
        guard Tokens.accepts(id, current: Tokens.current(selection, in: workspaces)?.id) else { return }
        selection = id
    }

    private func create() {
        isPresented = false
        onCreate?()
    }
}

/// The square logo of a workspace, or a building symbol when none is chosen. Decorative.
struct MonarchWorkspaceSwitcherLogo: View {
    let workspace: MonarchWorkspaceOption?
    let labels: MonarchWorkspaceSwitcherLabels

    var body: some View {
        if let workspace {
            MonarchAvatar(url: workspace.avatarURL, name: workspace.name, shape: .square, size: .sm, isDecorative: true)
        } else {
            MonarchAvatar(
                image: nil,
                name: labels.label ?? MonarchWorkspaceSwitcherStrings.label,
                systemImage: MonarchWorkspaceSwitcherTokens.placeholderSymbol,
                shape: .square,
                size: .sm,
                isDecorative: true
            )
        }
    }
}

#if DEBUG
    #Preview("MonarchWorkspaceSwitcher") {
        let workspaces = [
            MonarchWorkspaceOption(id: "acme", name: "Acme", handle: "acme"),
            MonarchWorkspaceOption(id: "rook", name: "Rook Labs", handle: "rooklabs"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchWorkspaceSwitcher(selection: .constant("acme"), workspaces: workspaces)
                MonarchWorkspaceSwitcher(selection: .constant(nil), workspaces: workspaces, size: .sm)
                MonarchWorkspaceSwitcher(selection: .constant("rook"), workspaces: workspaces, isIconOnly: true)
                MonarchWorkspaceSwitcherList(
                    workspaces: workspaces,
                    selection: "acme",
                    isLoading: true,
                    canCreate: true,
                    manageURL: URL(string: "https://example.com/workspaces"),
                    labels: MonarchWorkspaceSwitcherLabels(),
                    onChoose: { _ in },
                    onCreate: {},
                    onCancel: {}
                )
            }
        }
    }
#endif
