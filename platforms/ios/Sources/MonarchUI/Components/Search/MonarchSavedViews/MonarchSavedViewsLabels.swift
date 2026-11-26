/// Replacements for the built-in strings of ``MonarchSavedViews``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchSavedViewsLabels: Sendable {
    /// The spoken name of the menu button. Built in: "View".
    public var view: String?
    /// The button's text when no view is selected. Built in: "All items".
    public var noView: String?
    /// The marker for unsaved changes. Built in: "Edited".
    public var modified: String?
    /// The marker of a shared view. Built in: "Shared".
    public var shared: String?
    /// The marker of the default view. Built in: "Default view".
    public var isDefault: String?
    /// The heading above views without a group. Built in: "Views".
    public var views: String?
    /// The save menu item. Receives the view's name. Built in: "Save changes to “Open deals”".
    public var saveChanges: (@Sendable (String) -> String)?
    /// Built in: "Save as new view…".
    public var saveAsNew: String?
    /// Built in: "Discard changes".
    public var discardChanges: String?
    /// Built in: "Rename…".
    public var rename: String?
    /// Built in: "Set as default".
    public var setDefault: String?
    /// Built in: "Delete view…".
    public var delete: String?
    /// The title of the save alert. Built in: "Save view".
    public var saveTitle: String?
    /// The title of the rename alert. Built in: "Rename view".
    public var renameTitle: String?
    /// The name field's placeholder. Built in: "Name".
    public var name: String?
    /// Built in: "Save".
    public var save: String?
    /// Built in: "Cancel".
    public var cancel: String?
    /// Shown when the name is empty. Built in: "Enter a name."
    public var nameRequired: String?
    /// Shown when another view has the name. Built in: "A view with this name already exists."
    public var nameTaken: String?
    /// The title of the delete alert. Receives the view's name. Built in: "Delete “Open deals”?".
    public var deleteTitle: (@Sendable (String) -> String)?
    /// The message of the delete alert. Built in: "The view is removed for everyone who can see
    /// it. Items are not affected."
    public var deleteMessage: String?
    /// The destructive button of the delete alert. Built in: "Delete view".
    public var deleteConfirm: String?

    /// Replacements for built-in strings. Every parameter is optional.
    ///
    /// - Parameters:
    ///   - view: The spoken name of the menu button.
    ///   - noView: The button's text when no view is selected.
    ///   - modified: The marker for unsaved changes.
    ///   - shared: The marker of a shared view.
    ///   - isDefault: The marker of the default view.
    ///   - views: The heading above views without a group.
    ///   - saveChanges: The save menu item, from the view's name.
    ///   - saveAsNew: The save-as-new menu item.
    ///   - discardChanges: The discard menu item.
    ///   - rename: The rename menu item.
    ///   - setDefault: The set-as-default menu item.
    ///   - delete: The delete menu item.
    ///   - saveTitle: The title of the save alert.
    ///   - renameTitle: The title of the rename alert.
    ///   - name: The name field's placeholder.
    ///   - save: The save button of the alerts.
    ///   - cancel: The cancel button of the alerts.
    ///   - nameRequired: Shown when the name is empty.
    ///   - nameTaken: Shown when another view has the name.
    ///   - deleteTitle: The title of the delete alert, from the view's name.
    ///   - deleteMessage: The message of the delete alert.
    ///   - deleteConfirm: The destructive button of the delete alert.
    public init(
        view: String? = nil,
        noView: String? = nil,
        modified: String? = nil,
        shared: String? = nil,
        isDefault: String? = nil,
        views: String? = nil,
        saveChanges: (@Sendable (String) -> String)? = nil,
        saveAsNew: String? = nil,
        discardChanges: String? = nil,
        rename: String? = nil,
        setDefault: String? = nil,
        delete: String? = nil,
        saveTitle: String? = nil,
        renameTitle: String? = nil,
        name: String? = nil,
        save: String? = nil,
        cancel: String? = nil,
        nameRequired: String? = nil,
        nameTaken: String? = nil,
        deleteTitle: (@Sendable (String) -> String)? = nil,
        deleteMessage: String? = nil,
        deleteConfirm: String? = nil
    ) {
        self.view = view
        self.noView = noView
        self.modified = modified
        self.shared = shared
        self.isDefault = isDefault
        self.views = views
        self.saveChanges = saveChanges
        self.saveAsNew = saveAsNew
        self.discardChanges = discardChanges
        self.rename = rename
        self.setDefault = setDefault
        self.delete = delete
        self.saveTitle = saveTitle
        self.renameTitle = renameTitle
        self.name = name
        self.save = save
        self.cancel = cancel
        self.nameRequired = nameRequired
        self.nameTaken = nameTaken
        self.deleteTitle = deleteTitle
        self.deleteMessage = deleteMessage
        self.deleteConfirm = deleteConfirm
    }
}
