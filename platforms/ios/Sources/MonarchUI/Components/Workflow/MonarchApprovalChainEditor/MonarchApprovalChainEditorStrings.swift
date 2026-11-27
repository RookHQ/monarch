/// Built-in strings of the approval chain editor, from the `MonarchApprovalChainEditor` strings table.
enum MonarchApprovalChainEditorStrings {
    private static let table = "MonarchApprovalChainEditor"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.approvalChainEditor.\(key)", table: table)
    }

    private static func string(_ key: String, _ number: Int) -> String {
        MonarchStrings.string("monarch.approvalChainEditor.\(key)", table: table, number)
    }

    static var legend: String { string("legend") }
    static var approver: String { string("approver") }
    static var target: String { string("target") }
    static var targetPlaceholder: String { string("targetPlaceholder") }
    static var label: String { string("label") }
    static var labelPlaceholder: String { string("labelPlaceholder") }
    static var addStep: String { string("addStep") }
    static var targetRequired: String { string("targetRequired") }
    static var moveUp: String { string("moveUp") }
    static var moveDown: String { string("moveDown") }

    static func step(_ position: Int) -> String { string("step", position) }
    static func removeStep(_ position: Int) -> String { string("removeStep", position) }
    static func maxReached(_ count: Int) -> String { string("maxReached", count) }
    static func reorder(_ position: Int) -> String { string("reorder", position) }
    static func moved(_ position: Int) -> String { string("moved", position) }
}
