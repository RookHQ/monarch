/// Replacements for the built-in strings of ``MonarchPageTree``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchPageTreeLabels: Sendable {
    /// The spoken name of the tree. Built in: "Pages".
    public var label: String?
    /// The title of a page with a blank title. Built in: "Untitled page".
    public var untitled: String?
    /// Shown in a group without pages. Built in: "No pages yet."
    public var noPages: String?
    /// The spoken name of a group's add button, from the group. Built in: "Add page to Basics".
    public var addPage: (@Sendable (String) -> String)?
    /// The add group button and its dialog's button. Built in: "Add group".
    public var addGroup: String?
    /// The spoken name of a group's menu, from the group. Built in: "Actions for Basics".
    public var groupActions: (@Sendable (String) -> String)?
    /// Built in: "Rename group".
    public var renameGroup: String?
    /// Built in: "Move group up".
    public var moveGroupUp: String?
    /// Built in: "Move group down".
    public var moveGroupDown: String?
    /// The menu item and its confirm button. Built in: "Delete group".
    public var deleteGroup: String?
    /// The heading of the groups a page can move to. Built in: "Move to".
    public var moveTo: String?
    /// The accessibility action that moves a page up in its group. Built in: "Move up".
    public var movePageUp: String?
    /// The accessibility action that moves a page down in its group. Built in: "Move down".
    public var movePageDown: String?
    /// The menu item and its confirm button. Built in: "Delete page".
    public var deletePage: String?
    /// The title of the add group dialog. Built in: "Add a group".
    public var addGroupTitle: String?
    /// The label of the name field. Built in: "Group name".
    public var groupName: String?
    /// The suggested name of a new group. Built in: "New group".
    public var newGroupName: String?
    /// The confirm button of the rename dialog. Built in: "Rename".
    public var renameSubmit: String?
    /// The cancel button of every dialog. Built in: "Cancel".
    public var cancel: String?
    /// The title of the delete group dialog, from the group. Built in: "Delete the “Basics” group?"
    public var deleteGroupTitle: (@Sendable (String) -> String)?
    /// What deleting a group with pages also deletes, from the count. Built in: "Its 3 pages are deleted too."
    public var deleteGroupConsequence: (@Sendable (Int) -> String)?
    /// Asks for the group name before deleting it, from the name. Built in: "Type “Basics” to confirm."
    public var typeToConfirm: (@Sendable (String) -> String)?
    /// The title of the delete page dialog, from the page. Built in: "Delete “Welcome”?"
    public var deletePageTitle: (@Sendable (String) -> String)?
    /// Built in: "This cannot be undone."
    public var cannotUndo: String?
    /// Announced when a page or group moves to a position, from its name, the position and the count.
    /// Built in: "Basics moved to position 2 of 3."
    public var moved: (@Sendable (String, Int, Int) -> String)?
    /// Announced when a page moves to another group, from the page and the group. Built in: "Welcome moved to Guides."
    public var movedTo: (@Sendable (String, String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        label: String? = nil,
        untitled: String? = nil,
        noPages: String? = nil,
        addPage: (@Sendable (String) -> String)? = nil,
        addGroup: String? = nil,
        groupActions: (@Sendable (String) -> String)? = nil,
        renameGroup: String? = nil,
        moveGroupUp: String? = nil,
        moveGroupDown: String? = nil,
        deleteGroup: String? = nil,
        moveTo: String? = nil,
        movePageUp: String? = nil,
        movePageDown: String? = nil,
        deletePage: String? = nil,
        addGroupTitle: String? = nil,
        groupName: String? = nil,
        newGroupName: String? = nil,
        renameSubmit: String? = nil,
        cancel: String? = nil,
        deleteGroupTitle: (@Sendable (String) -> String)? = nil,
        deleteGroupConsequence: (@Sendable (Int) -> String)? = nil,
        typeToConfirm: (@Sendable (String) -> String)? = nil,
        deletePageTitle: (@Sendable (String) -> String)? = nil,
        cannotUndo: String? = nil,
        moved: (@Sendable (String, Int, Int) -> String)? = nil,
        movedTo: (@Sendable (String, String) -> String)? = nil
    ) {
        self.label = label
        self.untitled = untitled
        self.noPages = noPages
        self.addPage = addPage
        self.addGroup = addGroup
        self.groupActions = groupActions
        self.renameGroup = renameGroup
        self.moveGroupUp = moveGroupUp
        self.moveGroupDown = moveGroupDown
        self.deleteGroup = deleteGroup
        self.moveTo = moveTo
        self.movePageUp = movePageUp
        self.movePageDown = movePageDown
        self.deletePage = deletePage
        self.addGroupTitle = addGroupTitle
        self.groupName = groupName
        self.newGroupName = newGroupName
        self.renameSubmit = renameSubmit
        self.cancel = cancel
        self.deleteGroupTitle = deleteGroupTitle
        self.deleteGroupConsequence = deleteGroupConsequence
        self.typeToConfirm = typeToConfirm
        self.deletePageTitle = deletePageTitle
        self.cannotUndo = cannotUndo
        self.moved = moved
        self.movedTo = movedTo
    }
}
