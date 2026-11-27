import MonarchTokens
import SwiftUI

/// The pages of a multi-page document in named groups, with running page
/// numbers. Read only it is navigation; with editing handlers it is the
/// organiser: reorder pages, move them between groups, reorder, rename and
/// delete groups, and add pages and groups.
///
/// ```swift
/// MonarchPageTree(
///     guide.groups,
///     selection: $pageID,
///     onReorder: { groups, _ in guide.groups = groups },
///     onAddPage: { groupID in guide.addPage(to: groupID) },
///     onDeletePage: { pageID in guide.deletePage(pageID) }
/// )
/// .environment(\.editMode, $editMode)
/// ```
///
/// It is a standard `List` with a `Section` per group, so selection,
/// scrolling, the sidebar style and VoiceOver navigation are the system's;
/// put it in the sidebar of a `NavigationSplitView`. With `onReorder`,
/// pages are dragged within their group in edit mode (`EditButton` or the
/// `editMode` environment), moved to another group from their context
/// menu, and groups move up and down from their menu. Each handler shows
/// its control only when given. Renaming and adding ask for a name in an
/// alert; deleting asks for confirmation, and a group with pages asks for
/// its name to be typed. The app updates `groups` in the handlers.
///
/// Accessibility: the list is named "Pages". Each page is read by its title
/// (the number is visual only), the open page has the selected trait, and
/// group names are headings. Pages have "Move up" and "Move down"
/// accessibility actions while reordering is on, and every move is
/// announced ("Welcome moved to position 2 of 4."). Group menus and add
/// buttons have spoken names with the group and 44-point touch targets.
public struct MonarchPageTree: View {
    private let groups: [MonarchPageTreeGroup]
    @Binding private var selection: String?
    private let isNumbered: Bool
    private let labels: MonarchPageTreeLabels
    private let onReorder: (@MainActor ([MonarchPageTreeGroup], MonarchPageTreeMove) -> Void)?
    private let onAddPage: (@MainActor (String) -> Void)?
    private let onAddGroup: (@MainActor (String) -> Void)?
    private let onRenameGroup: (@MainActor (String, String) -> Void)?
    private let onDeleteGroup: (@MainActor (String) -> Void)?
    private let onDeletePage: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @State private var dialog: MonarchPageTreeDialog?
    @State private var text = ""

    /// A page tree.
    ///
    /// - Parameters:
    ///   - groups: The groups and their pages, in order.
    ///   - selection: The id of the page being viewed.
    ///   - isNumbered: Shows page numbers (01, 02, …) running across groups.
    ///   - labels: Replacements for the built-in strings.
    ///   - onReorder: Makes the order editable. Receives the new groups and what moved.
    ///   - onAddPage: Shows an add button on each group. Receives the group id.
    ///   - onAddGroup: Shows "Add group". Receives the name typed in the dialog.
    ///   - onRenameGroup: Shows "Rename group". Receives the group id and the new name.
    ///   - onDeleteGroup: Shows "Delete group" behind a confirmation, while there is more than one group.
    ///   - onDeletePage: Shows "Delete page" behind a confirmation, while there is more than one page.
    public init(
        _ groups: [MonarchPageTreeGroup],
        selection: Binding<String?>,
        isNumbered: Bool = true,
        labels: MonarchPageTreeLabels = MonarchPageTreeLabels(),
        onReorder: (@MainActor ([MonarchPageTreeGroup], MonarchPageTreeMove) -> Void)? = nil,
        onAddPage: (@MainActor (String) -> Void)? = nil,
        onAddGroup: (@MainActor (String) -> Void)? = nil,
        onRenameGroup: (@MainActor (String, String) -> Void)? = nil,
        onDeleteGroup: (@MainActor (String) -> Void)? = nil,
        onDeletePage: (@MainActor (String) -> Void)? = nil
    ) {
        self.groups = groups
        _selection = selection
        self.isNumbered = isNumbered
        self.labels = labels
        self.onReorder = onReorder
        self.onAddPage = onAddPage
        self.onAddGroup = onAddGroup
        self.onRenameGroup = onRenameGroup
        self.onDeleteGroup = onDeleteGroup
        self.onDeletePage = onDeletePage
    }

    /// The content of the view.
    public var body: some View {
        let numbers = MonarchPageTreeTokens.pageNumbers(groups)
        List(selection: $selection) {
            Group {
                ForEach(Array(groups.enumerated()), id: \.element.id) { index, group in
                    Section {
                        if group.pages.isEmpty {
                            Text(labels.noPages ?? MonarchPageTreeStrings.noPages)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(
                                    theme.colors[role: MonarchPageTreeTokens.mutedColor(isEnabled: isEnabled)]
                                )
                        }
                        ForEach(group.pages) { page in
                            row(page, number: numbers[page.id] ?? 0, in: group)
                        }
                        .onMove(perform: onReorder == nil ? nil : { reorder(group.id, from: $0, to: $1) })
                    } header: {
                        header(group, index: index)
                    }
                }
                if onAddGroup != nil {
                    Button {
                        open(.addGroup, text: labels.newGroupName ?? MonarchPageTreeStrings.newGroupName)
                    } label: {
                        Label(
                            labels.addGroup ?? MonarchPageTreeStrings.addGroup,
                            systemImage: MonarchPageTreeTokens.addSymbol
                        )
                        .monarchTextStyle(.body)
                    }
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
        .accessibilityLabel(labels.label ?? MonarchPageTreeStrings.label)
        .alert(
            dialogTitle,
            isPresented: Binding(get: { dialog != nil }, set: { if !$0 { dialog = nil } }),
            presenting: dialog,
            actions: dialogActions,
            message: dialogMessage
        )
    }

    // MARK: Rows

    private func row(_ page: MonarchPageTreePage, number: Int, in group: MonarchPageTreeGroup) -> some View {
        let title = MonarchPageTreeTokens.title(page, labels: labels)
        let canDelete = onDeletePage != nil && MonarchPageTreeTokens.canDeletePage(groups)
        let targets = onReorder == nil ? [] : MonarchPageTreeTokens.moveTargets(groups, from: group.id)
        return HStack(alignment: .firstTextBaseline, spacing: MonarchPageTreeTokens.numberSpacing) {
            if isNumbered {
                Text(MonarchPageTreeTokens.numberText(number, locale: locale))
                    .monarchTextStyle(.codeSmall)
                    .foregroundStyle(theme.colors[role: MonarchPageTreeTokens.numberColor(isEnabled: isEnabled)])
                    .accessibilityHidden(true)
            }
            Text(title)
                .monarchTextStyle(selection == page.id ? .bodyStrong : .body)
                .foregroundStyle(
                    selection == page.id
                        ? theme.colors.accentStrong
                        : theme.colors[role: MonarchPageTreeTokens.titleColor(isEnabled: isEnabled)]
                )
        }
        .accessibilityElement(children: .combine)
        .tag(page.id)
        .listRowSeparator(.hidden)
        .listRowBackground(
            RoundedRectangle(cornerRadius: MonarchTokens.Radius.md, style: .continuous)
                .fill(selection == page.id ? theme.colors.accentSubtle : Color.clear)
                .padding(.horizontal, MonarchTokens.Space.space1)
        )
        .contextMenu {
            if !targets.isEmpty {
                Section(labels.moveTo ?? MonarchPageTreeStrings.moveTo) {
                    ForEach(targets) { target in
                        Button(target.label, systemImage: MonarchPageTreeTokens.moveSymbol) {
                            apply(MonarchPageTreeTokens.movingPage(groups, pageID: page.id, toGroup: target.id))
                        }
                    }
                }
            }
            if canDelete {
                Button(
                    labels.deletePage ?? MonarchPageTreeStrings.deletePage,
                    systemImage: MonarchPageTreeTokens.deleteSymbol,
                    role: .destructive
                ) { open(.deletePage(page)) }
            }
        }
        .swipeActions(edge: .trailing) {
            if canDelete {
                Button(
                    labels.deletePage ?? MonarchPageTreeStrings.deletePage,
                    systemImage: MonarchPageTreeTokens.deleteSymbol,
                    role: .destructive
                ) { open(.deletePage(page)) }
            }
        }
        .accessibilityActions {
            if onReorder != nil {
                Button(labels.movePageUp ?? MonarchPageTreeStrings.movePageUp) {
                    apply(MonarchPageTreeTokens.movingPage(groups, pageID: page.id, by: -1))
                }
                Button(labels.movePageDown ?? MonarchPageTreeStrings.movePageDown) {
                    apply(MonarchPageTreeTokens.movingPage(groups, pageID: page.id, by: 1))
                }
            }
        }
    }

    private func header(_ group: MonarchPageTreeGroup, index: Int) -> some View {
        let actions = MonarchPageTreeTokens.groupActions(
            index: index,
            groupCount: groups.count,
            canRename: onRenameGroup != nil,
            canReorder: onReorder != nil,
            canDelete: onDeleteGroup != nil
        )
        return HStack(spacing: MonarchPageTreeTokens.headerSpacing) {
            Text(group.label)
                .monarchTextStyle(.caption, weight: .semibold)
                .textCase(.uppercase)
                .tracking(MonarchPageTreeTokens.headerTracking)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
            if let onAddPage {
                Button {
                    onAddPage(group.id)
                } label: {
                    MonarchIcon(
                        systemName: MonarchPageTreeTokens.addSymbol,
                        label: labels.addPage?(group.label) ?? MonarchPageTreeStrings.addPage(group.label),
                        size: .sm
                    )
                    .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            }
            if !actions.isEmpty {
                Menu {
                    ForEach(Array(actions.enumerated()), id: \.offset) { _, action in
                        menuItem(action, group: group)
                    }
                } label: {
                    MonarchIcon(
                        systemName: MonarchPageTreeTokens.menuSymbol,
                        label: labels.groupActions?(group.label) ?? MonarchPageTreeStrings.groupActions(group.label),
                        size: .sm
                    )
                    .monarchMinimumHitTarget()
                }
                .buttonStyle(.plain)
            }
        }
        .foregroundStyle(theme.colors[role: MonarchPageTreeTokens.mutedColor(isEnabled: isEnabled)])
    }

    @ViewBuilder
    private func menuItem(_ action: MonarchPageTreeTokens.GroupAction, group: MonarchPageTreeGroup) -> some View {
        switch action {
        case .rename:
            Button(labels.renameGroup ?? MonarchPageTreeStrings.renameGroup) { open(.rename(group), text: group.label) }
        case .moveUp:
            Button(labels.moveGroupUp ?? MonarchPageTreeStrings.moveGroupUp) {
                apply(MonarchPageTreeTokens.movingGroup(groups, groupID: group.id, by: -1))
            }
        case .moveDown:
            Button(labels.moveGroupDown ?? MonarchPageTreeStrings.moveGroupDown) {
                apply(MonarchPageTreeTokens.movingGroup(groups, groupID: group.id, by: 1))
            }
        case .delete:
            Divider()
            Button(labels.deleteGroup ?? MonarchPageTreeStrings.deleteGroup, role: .destructive) {
                open(.deleteGroup(group))
            }
        }
    }

    // MARK: Dialogs

    private var dialogTitle: String {
        switch dialog {
        case .rename: labels.renameGroup ?? MonarchPageTreeStrings.renameGroup
        case .addGroup: labels.addGroupTitle ?? MonarchPageTreeStrings.addGroupTitle
        case .deleteGroup(let group):
            labels.deleteGroupTitle?(group.label) ?? MonarchPageTreeStrings.deleteGroupTitle(group.label)
        case .deletePage(let page):
            labels.deletePageTitle?(MonarchPageTreeTokens.title(page, labels: labels))
                ?? MonarchPageTreeStrings.deletePageTitle(MonarchPageTreeTokens.title(page, labels: labels))
        case nil: ""
        }
    }

    @ViewBuilder
    private func dialogActions(_ dialog: MonarchPageTreeDialog) -> some View {
        if dialog.hasTextField {
            TextField(labels.groupName ?? MonarchPageTreeStrings.groupName, text: $text)
        }
        switch dialog {
        case .rename(let group):
            Button(labels.renameSubmit ?? MonarchPageTreeStrings.renameSubmit) {
                if let name = MonarchPageTreeTokens.cleanedName(text), name != group.label {
                    onRenameGroup?(group.id, name)
                }
            }
            .disabled(MonarchPageTreeTokens.cleanedName(text) == nil)
        case .addGroup:
            Button(labels.addGroup ?? MonarchPageTreeStrings.addGroup) {
                if let name = MonarchPageTreeTokens.cleanedName(text) { onAddGroup?(name) }
            }
            .disabled(MonarchPageTreeTokens.cleanedName(text) == nil)
        case .deleteGroup(let group):
            Button(labels.deleteGroup ?? MonarchPageTreeStrings.deleteGroup, role: .destructive) {
                if MonarchPageTreeTokens.canDelete(group, typed: text) { onDeleteGroup?(group.id) }
            }
            .disabled(!MonarchPageTreeTokens.canDelete(group, typed: text))
        case .deletePage(let page):
            Button(labels.deletePage ?? MonarchPageTreeStrings.deletePage, role: .destructive) {
                onDeletePage?(page.id)
            }
        }
        Button(labels.cancel ?? MonarchPageTreeStrings.cancel, role: .cancel) {}
    }

    @ViewBuilder
    private func dialogMessage(_ dialog: MonarchPageTreeDialog) -> some View {
        switch dialog {
        case .deleteGroup(let group): Text(MonarchPageTreeTokens.deleteGroupMessage(group, labels: labels))
        case .deletePage: Text(labels.cannotUndo ?? MonarchPageTreeStrings.cannotUndo)
        case .rename, .addGroup: EmptyView()
        }
    }

    // MARK: Editing

    private func open(_ dialog: MonarchPageTreeDialog, text: String = "") {
        self.text = text
        self.dialog = dialog
    }

    private func reorder(_ groupID: String, from offsets: IndexSet, to destination: Int) {
        apply(MonarchPageTreeTokens.reordering(groups, groupID: groupID, from: offsets, to: destination))
    }

    private func apply(_ result: (groups: [MonarchPageTreeGroup], move: MonarchPageTreeMove)?) {
        guard let result, let onReorder else { return }
        onReorder(result.groups, result.move)
        if let message = MonarchPageTreeTokens.announcement(for: result.move, in: result.groups, labels: labels) {
            AccessibilityNotification.Announcement(message).post()
        }
    }
}

#if DEBUG
    #Preview("MonarchPageTree") {
        MonarchPreviewMatrix {
            MonarchPageTree(
                [
                    MonarchPageTreeGroup(
                        id: "basics",
                        label: "Basics",
                        pages: [
                            MonarchPageTreePage(id: "welcome", title: "Welcome"),
                            MonarchPageTreePage(id: "setup", title: "Set up"),
                        ]
                    ),
                    MonarchPageTreeGroup(id: "guides", label: "Guides", pages: []),
                ],
                selection: .constant("setup"),
                onReorder: { _, _ in },
                onAddPage: { _ in }
            )
            .frame(minHeight: MonarchAccessibility.minimumHitTarget * 8)
        }
    }
#endif
