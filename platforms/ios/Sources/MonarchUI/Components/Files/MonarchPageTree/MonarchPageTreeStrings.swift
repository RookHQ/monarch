/// Built-in strings of ``MonarchPageTree``, from the `MonarchPageTree` strings table.
enum MonarchPageTreeStrings {
    private static let table = "MonarchPageTree"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.pageTree.\(key)", table: table)
    }

    private static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    static var label: String { string("label") }
    static var untitled: String { string("untitled") }
    static var noPages: String { string("noPages") }
    static func addPage(_ group: String) -> String { string("addPage", group) }
    static var addGroup: String { string("addGroup") }
    static func groupActions(_ group: String) -> String { string("groupActions", group) }
    static var renameGroup: String { string("renameGroup") }
    static var moveGroupUp: String { string("moveGroupUp") }
    static var moveGroupDown: String { string("moveGroupDown") }
    static var deleteGroup: String { string("deleteGroup") }
    static var moveTo: String { string("moveTo") }
    static var movePageUp: String { string("movePageUp") }
    static var movePageDown: String { string("movePageDown") }
    static var deletePage: String { string("deletePage") }
    static var addGroupTitle: String { string("addGroupTitle") }
    static var groupName: String { string("groupName") }
    static var newGroupName: String { string("newGroupName") }
    static var renameSubmit: String { string("renameSubmit") }
    static var cancel: String { string("cancel") }
    static func deleteGroupTitle(_ group: String) -> String { string("deleteGroupTitle", group) }
    static func deleteGroupConsequence(_ count: Int) -> String {
        count == 1 ? string("deleteGroupConsequence.one") : string("deleteGroupConsequence.other", count)
    }
    static func typeToConfirm(_ group: String) -> String { string("typeToConfirm", group) }
    static func deletePageTitle(_ page: String) -> String { string("deletePageTitle", page) }
    static var cannotUndo: String { string("cannotUndo") }
    static func moved(_ name: String, _ position: Int, _ count: Int) -> String {
        string("moved", name, position, count)
    }
    static func movedTo(_ page: String, _ group: String) -> String { string("movedTo", page, group) }
}
