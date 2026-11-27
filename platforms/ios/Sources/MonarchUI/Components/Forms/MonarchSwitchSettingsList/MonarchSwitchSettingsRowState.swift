/// The save state of one ``MonarchSwitchSettingsRow``. Pure, so the
/// optimistic toggle, roll back and retry are tested without a view.
struct MonarchSwitchSettingsRowState: Equatable, Sendable {
    /// Where the save stands.
    private(set) var status: MonarchSwitchSettingsStatus = .idle
    /// The value shown while a save runs.
    private(set) var pending: Bool?
    /// The value that failed, which the retry button saves again.
    private(set) var retryValue: Bool?
    /// The failed save's own message.
    private(set) var message: String?
    /// Counts saves, so a late timer never clears a newer "Saved".
    private(set) var generation = 0

    /// The value the switch shows: the pending one while saving, otherwise
    /// the saved one.
    func shown(saved: Bool) -> Bool {
        pending ?? saved
    }

    /// Starts saving `value`. Returns `false`, and changes nothing, while a
    /// save runs or when the row is disabled.
    mutating func begin(_ value: Bool, isEnabled: Bool) -> Bool {
        guard isEnabled, status != .saving else { return false }
        status = .saving
        pending = value
        retryValue = nil
        message = nil
        generation += 1
        return true
    }

    /// The save finished. The caller has already stored the new value.
    mutating func succeed() {
        status = .saved
        pending = nil
    }

    /// The save failed: the switch returns to the saved value.
    mutating func fail(_ value: Bool, message: String?) {
        status = .error
        pending = nil
        retryValue = value
        self.message = message
    }

    /// Ends "Saved" after its time, unless another save started since.
    mutating func clearSaved(generation: Int) {
        guard status == .saved, generation == self.generation else { return }
        status = .idle
    }
}
