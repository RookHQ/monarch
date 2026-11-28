import SwiftUI

/// A menu that switches between named views of a list (filters, sort and
/// columns) and saves, renames, deletes and sets the default view.
///
/// ```swift
/// MonarchSavedViews(
///     views,
///     selection: $viewID,
///     isModified: filters != savedFilters,
///     onSaveNew: { name in model.saveView(named: name) },
///     onSaveChanges: { id in model.overwrite(id) },
///     onDiscardChanges: { model.reapply() }
/// )
/// ```
///
/// The app keeps the views and applies them: the menu only sets `selection`
/// and calls back. It offers only the actions the app gives a callback for,
/// and never saves over, renames or deletes a read-only view. Naming uses an
/// alert with a field whose Save button stays disabled while the name is
/// empty or taken; deleting asks first.
///
/// Accessibility: the button is named "View" and its value is the selected
/// view's name, followed by "Edited" when there are unsaved changes. The
/// Edited marker is a word, not only a colour. In the menu each view is a
/// toggle that is on when selected, with its count, Shared and Default view
/// read after the name. The button has a touch target of at least 44 points.
public struct MonarchSavedViews: View {
    private let views: [MonarchSavedView]
    @Binding private var selection: String?
    private let isModified: Bool
    private let size: MonarchButtonSize
    private let maxNameLength: Int
    private let labels: MonarchSavedViewsLabels
    private let onSaveNew: (@MainActor (String) -> Void)?
    private let onSaveChanges: (@MainActor (String) -> Void)?
    private let onDiscardChanges: (@MainActor () -> Void)?
    private let onRename: (@MainActor (String, String) -> Void)?
    private let onDelete: (@MainActor (String) -> Void)?
    private let onSetDefault: (@MainActor (String) -> Void)?

    @State private var dialog: MonarchSavedViewsDialog?
    @State private var draft = ""
    @Environment(\.locale) private var locale
    @Environment(\.monarchTheme) private var theme

    /// A saved views menu.
    ///
    /// - Parameters:
    ///   - views: The views, in menu order.
    ///   - selection: The id of the selected view, or `nil` for none.
    ///   - isModified: Whether the current filters, sort or columns differ from the selected
    ///     view. Shows "Edited" and offers to save or discard the changes.
    ///   - size: The size of the button.
    ///   - maxNameLength: The longest name allowed.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSaveNew: Stores the current state under the new name and selects it.
    ///   - onSaveChanges: Saves the current state into the view with this id.
    ///   - onDiscardChanges: Reapplies the selected view.
    ///   - onRename: Renames the view with this id to the new name.
    ///   - onDelete: Deletes the view with this id.
    ///   - onSetDefault: Makes the view with this id open by default.
    public init(
        _ views: [MonarchSavedView],
        selection: Binding<String?>,
        isModified: Bool = false,
        size: MonarchButtonSize = .md,
        maxNameLength: Int = 80,
        labels: MonarchSavedViewsLabels = MonarchSavedViewsLabels(),
        onSaveNew: (@MainActor (String) -> Void)? = nil,
        onSaveChanges: (@MainActor (String) -> Void)? = nil,
        onDiscardChanges: (@MainActor () -> Void)? = nil,
        onRename: (@MainActor (String, String) -> Void)? = nil,
        onDelete: (@MainActor (String) -> Void)? = nil,
        onSetDefault: (@MainActor (String) -> Void)? = nil
    ) {
        self.views = views
        _selection = selection
        self.isModified = isModified
        self.size = size
        self.maxNameLength = maxNameLength
        self.labels = labels
        self.onSaveNew = onSaveNew
        self.onSaveChanges = onSaveChanges
        self.onDiscardChanges = onDiscardChanges
        self.onRename = onRename
        self.onDelete = onDelete
        self.onSetDefault = onSetDefault
    }

    /// The content of the view.
    public var body: some View {
        let text = MonarchSavedViewsText(labels: labels)
        let selected = views.first { $0.id == selection }
        Menu {
            ForEach(Array(MonarchSavedViewsTokens.groups(views).enumerated()), id: \.offset) { _, group in
                Section(group.title ?? text.views) {
                    ForEach(group.views) { view in
                        Toggle(isOn: isSelected(view.id)) {
                            Text(view.name)
                            if let subtitle = MonarchSavedViewsTokens.subtitle(view, text: text, locale: locale) {
                                Text(subtitle)
                            }
                        }
                    }
                }
            }
            let actions = MonarchSavedViewsTokens.actions(
                selected: selected,
                isModified: isModified,
                available: available
            )
            if !actions.isEmpty {
                Section {
                    ForEach(actions, id: \.self) { action in
                        button(action, selected: selected, text: text)
                    }
                }
            }
        } label: {
            HStack(spacing: MonarchSavedViewsTokens.spacing) {
                Text(MonarchSavedViewsTokens.title(selected: selected, text: text))
                    .lineLimit(1)
                if let count = selected?.count {
                    Text(count, format: .number.locale(locale))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
                if isModified {
                    MonarchBadge(text.modified, tone: MonarchSavedViewsTokens.modifiedTone)
                }
                MonarchIcon(systemName: MonarchSavedViewsTokens.chevronSymbol, size: .sm)
            }
        }
        .buttonStyle(.monarch(.secondary, size: size))
        .accessibilityLabel(text.view)
        .accessibilityValue(
            MonarchSavedViewsTokens.accessibilityValue(selected: selected, isModified: isModified, text: text)
        )
        .alert(nameTitle(text), isPresented: isNaming, presenting: dialog) { presented in
            nameField(presented, text: text)
        } message: { presented in
            Text(problem(for: presented, text: text) ?? "")
        }
        .alert(deleteTitle(text), isPresented: isDeleting, presenting: dialog) { presented in
            if case .delete(let view) = presented {
                Button(text.deleteConfirm, role: .destructive) { onDelete?(view.id) }
            }
            Button(text.cancel, role: .cancel) {}
        } message: { _ in
            Text(text.deleteMessage)
        }
    }

    private var available: Set<MonarchSavedViewsAction> {
        var set: Set<MonarchSavedViewsAction> = []
        if onSaveChanges != nil { set.insert(.saveChanges) }
        if onSaveNew != nil { set.insert(.saveAsNew) }
        if onDiscardChanges != nil { set.insert(.discardChanges) }
        if onRename != nil { set.insert(.rename) }
        if onSetDefault != nil { set.insert(.setDefault) }
        if onDelete != nil { set.insert(.delete) }
        return set
    }

    private func isSelected(_ id: String) -> Binding<Bool> {
        Binding(
            get: { selection == id },
            set: { isOn in if isOn { selection = id } }
        )
    }

    @ViewBuilder
    private func button(_ action: MonarchSavedViewsAction, selected: MonarchSavedView?, text: MonarchSavedViewsText)
        -> some View
    {
        switch action {
        case .saveChanges:
            if let selected {
                Button(text.saveChanges(selected.name)) { onSaveChanges?(selected.id) }
            }
        case .saveAsNew:
            Button(text.saveAsNew) {
                draft = ""
                dialog = .saveAsNew
            }
        case .discardChanges:
            Button(text.discardChanges) { onDiscardChanges?() }
        case .rename:
            if let selected {
                Button(text.rename) {
                    draft = selected.name
                    dialog = .rename(selected)
                }
            }
        case .setDefault:
            if let selected {
                Button(text.setDefault) { onSetDefault?(selected.id) }
            }
        case .delete:
            if let selected {
                Button(text.delete, role: .destructive) { dialog = .delete(selected) }
            }
        }
    }

    @ViewBuilder
    private func nameField(_ presented: MonarchSavedViewsDialog, text: MonarchSavedViewsText) -> some View {
        TextField(text.name, text: $draft)
            .onChange(of: draft) { _, new in
                let limited = MonarchSavedViewsTokens.limit(new, to: maxNameLength)
                if limited != new { draft = limited }
            }
        Button(text.save) { submit(presented) }
            .disabled(problem(for: presented, text: text) != nil)
        Button(text.cancel, role: .cancel) {}
    }

    private func submit(_ presented: MonarchSavedViewsDialog) {
        let name = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        switch presented {
        case .saveAsNew: onSaveNew?(name)
        case .rename(let view): onRename?(view.id, name)
        case .delete: break
        }
    }

    private func problem(for presented: MonarchSavedViewsDialog, text: MonarchSavedViewsText) -> String? {
        let excluding: String? =
            if case .rename(let view) = presented { view.id } else { nil }
        return MonarchSavedViewsTokens.problem(name: draft, views: views, excluding: excluding, text: text)
    }

    private func nameTitle(_ text: MonarchSavedViewsText) -> String {
        if case .rename = dialog { return text.renameTitle }
        return text.saveTitle
    }

    private func deleteTitle(_ text: MonarchSavedViewsText) -> String {
        if case .delete(let view) = dialog { return text.deleteTitle(view.name) }
        return ""
    }

    private var isNaming: Binding<Bool> {
        Binding(
            get: {
                switch dialog {
                case .saveAsNew, .rename: true
                default: false
                }
            },
            set: { if !$0 { dialog = nil } }
        )
    }

    private var isDeleting: Binding<Bool> {
        Binding(
            get: {
                if case .delete = dialog { return true }
                return false
            },
            set: { if !$0 { dialog = nil } }
        )
    }
}

#if DEBUG
    #Preview("MonarchSavedViews") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSavedViews(
                    [
                        MonarchSavedView("All deals", id: "all", isReadOnly: true, group: "Built-in"),
                        MonarchSavedView("Open deals", id: "open", count: 12, isDefault: true),
                    ],
                    selection: .constant("open"),
                    isModified: true,
                    onSaveNew: { _ in }
                )
                MonarchSavedViews([], selection: .constant(nil), size: .sm)
            }
        }
    }
#endif
