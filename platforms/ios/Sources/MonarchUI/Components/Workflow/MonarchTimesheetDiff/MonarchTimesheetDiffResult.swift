/// The difference between two versions of a timesheet: entries added,
/// removed and changed.
public struct MonarchTimesheetDiffResult: Sendable, Hashable {
    /// Entries only in the new version.
    public let added: [MonarchTimesheetDiffEntry]
    /// Entries only in the old version.
    public let removed: [MonarchTimesheetDiffEntry]
    /// Entries in both versions with different values.
    public let changed: [MonarchTimesheetDiffChange]

    /// A difference the product computed.
    ///
    /// - Parameters:
    ///   - added: Entries only in the new version.
    ///   - removed: Entries only in the old version.
    ///   - changed: Entries in both versions with different values.
    public init(
        added: [MonarchTimesheetDiffEntry] = [],
        removed: [MonarchTimesheetDiffEntry] = [],
        changed: [MonarchTimesheetDiffChange] = []
    ) {
        self.added = added
        self.removed = removed
        self.changed = changed
    }

    /// The difference between two versions, by entry identifier: entries
    /// only in `after` are added, only in `before` removed, and in both
    /// with a different date, time, project, activity, task, billable flag
    /// or note changed. Order follows the versions.
    ///
    /// - Parameters:
    ///   - before: The entries of the old version.
    ///   - after: The entries of the new version.
    public init(before: [MonarchTimesheetDiffEntry], after: [MonarchTimesheetDiffEntry]) {
        let old = Dictionary(before.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        let new = Set(after.map(\.id))
        var changed: [MonarchTimesheetDiffChange] = []
        for entry in after {
            guard let previous = old[entry.id] else { continue }
            let fields = MonarchTimesheetDiffTokens.changedFields(previous, entry)
            if !fields.isEmpty {
                changed.append(MonarchTimesheetDiffChange(id: entry.id, fields: fields, before: previous, after: entry))
            }
        }
        self.init(
            added: after.filter { old[$0.id] == nil },
            removed: before.filter { !new.contains($0.id) },
            changed: changed
        )
    }

    /// Whether nothing was added, removed or changed.
    public var isEmpty: Bool {
        added.isEmpty && removed.isEmpty && changed.isEmpty
    }

    /// The minutes gained or lost, in total and billable.
    public var totals: MonarchTimesheetDiffTotals {
        func billable(_ entry: MonarchTimesheetDiffEntry) -> Int {
            entry.isBillable == false ? 0 : entry.minutes
        }
        var total = 0
        var billed = 0
        for entry in added {
            total += entry.minutes
            billed += billable(entry)
        }
        for entry in removed {
            total -= entry.minutes
            billed -= billable(entry)
        }
        for change in changed {
            total += change.after.minutes - change.before.minutes
            billed += billable(change.after) - billable(change.before)
        }
        return MonarchTimesheetDiffTotals(total: total, billable: billed)
    }
}
