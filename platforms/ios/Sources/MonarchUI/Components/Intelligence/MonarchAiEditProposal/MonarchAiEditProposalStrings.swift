/// Built-in strings of the edit proposal, from the `MonarchAiEditProposal` strings table.
enum MonarchAiEditProposalStrings {
    private static let table = "MonarchAiEditProposal"

    private static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.aiEditProposal." + key, table: table)
    }

    /// The default title.
    static var title: String { string("title") }
    /// The provenance badge.
    static var badge: String { string("badge") }
    /// Built in: "Accept all".
    static var acceptAll: String { string("acceptAll") }
    /// Built in: "Reject all".
    static var rejectAll: String { string("rejectAll") }
    /// Built in: "Accept".
    static var accept: String { string("accept") }
    /// Built in: "Reject".
    static var reject: String { string("reject") }
    /// Built in: "Revert".
    static var revert: String { string("revert") }
    /// Read before removed text.
    static var deleted: String { string("deleted") }
    /// Read before added text.
    static var inserted: String { string("inserted") }
    /// Shown after a change that changes nothing.
    static var unchanged: String { string("unchanged") }
    /// Announced after every change was accepted.
    static var acceptedAll: String { string("announce.acceptedAll") }
    /// Announced after every change was rejected.
    static var rejectedAll: String { string("announce.rejectedAll") }
    /// Announced after one change was accepted.
    static var acceptedOne: String { string("announce.accepted") }
    /// Announced after one change was rejected.
    static var rejectedOne: String { string("announce.rejected") }
    /// Announced when no changes are left.
    static var noneLeft: String { string("announce.noneLeft") }

    /// The name of a proposal state.
    static func state(_ state: MonarchAiEditProposalState) -> String { string("state." + state.rawValue) }
    /// The name of a decision.
    static func hunkState(_ status: MonarchAiEditHunkStatus) -> String { string("hunkState." + status.rawValue) }
    /// The note once the proposal is no longer proposed.
    static func readOnly(_ state: MonarchAiEditProposalState) -> String { string("readOnly." + state.rawValue) }

    /// "3 changes" or "1 change".
    static func count(_ total: Int) -> String {
        let key = total == 1 ? "monarch.aiEditProposal.countOne" : "monarch.aiEditProposal.count"
        return MonarchStrings.string(key, table: table, total)
    }

    /// "1 of 3 changes left".
    static func left(_ pending: Int, of total: Int) -> String {
        let key = total == 1 ? "monarch.aiEditProposal.leftOne" : "monarch.aiEditProposal.left"
        return MonarchStrings.string(key, table: table, pending, total)
    }

    /// "Change 2 of 3".
    static func hunkName(_ index: Int, _ total: Int) -> String {
        MonarchStrings.string("monarch.aiEditProposal.hunkName", table: table, index, total)
    }

    /// "2 left."
    static func remaining(_ count: Int) -> String {
        MonarchStrings.string("monarch.aiEditProposal.announce.left", table: table, count)
    }

    /// A per-change button's spoken name: "Accept, Change 2 of 3".
    static func action(_ action: String, _ hunk: String) -> String {
        MonarchStrings.string("monarch.aiEditProposal.action", table: table, action, hunk)
    }
}
