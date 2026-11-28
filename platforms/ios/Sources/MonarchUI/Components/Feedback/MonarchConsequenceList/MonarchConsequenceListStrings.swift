/// Built-in strings of the consequence list, from the
/// `MonarchConsequenceList` strings table.
enum MonarchConsequenceListStrings {
    private static let table = "MonarchConsequenceList"

    /// The heading of the group of one kind.
    static func group(_ kind: MonarchConsequenceKind) -> String {
        switch kind {
        case .removed: MonarchStrings.string("monarch.consequenceList.group.removed", table: table)
        case .changed: MonarchStrings.string("monarch.consequenceList.group.changed", table: table)
        case .warning: MonarchStrings.string("monarch.consequenceList.group.warning", table: table)
        case .kept: MonarchStrings.string("monarch.consequenceList.group.kept", table: table)
        }
    }

    /// The spoken name of a kind, read before an item that is not in a group.
    static func kind(_ kind: MonarchConsequenceKind) -> String {
        switch kind {
        case .removed: MonarchStrings.string("monarch.consequenceList.kind.removed", table: table)
        case .changed: MonarchStrings.string("monarch.consequenceList.kind.changed", table: table)
        case .warning: MonarchStrings.string("monarch.consequenceList.kind.warning", table: table)
        case .kept: MonarchStrings.string("monarch.consequenceList.kind.kept", table: table)
        }
    }

    /// Spoken while the consequences are being worked out.
    static var loading: String { MonarchStrings.string("monarch.consequenceList.loading", table: table) }
}
