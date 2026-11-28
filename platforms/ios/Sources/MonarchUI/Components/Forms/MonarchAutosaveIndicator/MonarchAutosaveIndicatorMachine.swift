import Foundation

/// The rules of ``MonarchAutosave`` as a value: what each event changes and
/// which timers and saves follow. It never waits or saves by itself, so
/// every rule is tested without a clock.
struct MonarchAutosaveIndicatorMachine<Value: Equatable> {
    /// What the controller does after an event.
    enum Effect: Equatable {
        /// Cancel the pending timer, then fire ``timerFired()`` after the duration.
        case schedule(Duration)
        /// Cancel the pending timer.
        case cancelTimer
        /// Save this value, then report ``saveSucceeded(at:)`` or ``saveFailed()``.
        case save(Value)
    }

    let delay: Duration
    let retries: Int
    let retryDelay: Duration

    private(set) var status: MonarchSaveStatus
    private(set) var savedAt: Date?
    /// The newest value.
    private(set) var latest: Value
    /// The last value saved, or the initial value.
    private(set) var lastSaved: Value
    /// The value being saved.
    private(set) var inFlight: Value?
    /// Whether a timer is pending.
    private(set) var isTimerArmed = false
    /// Failed attempts since the last success, edit or manual retry.
    private(set) var attempt = 0
    /// Whether changes are saved automatically.
    private(set) var isEnabled = true
    /// Whether a save was asked for while another was in flight.
    private(set) var savesAfterInFlight = false

    init(value: Value, savedAt: Date?, delay: Duration, retries: Int, retryDelay: Duration) {
        self.delay = max(.zero, delay)
        self.retries = max(0, retries)
        self.retryDelay = max(.zero, retryDelay)
        self.status = savedAt == nil ? .idle : .saved
        self.savedAt = savedAt
        self.latest = value
        self.lastSaved = value
    }

    /// Whether there are changes that are not saved yet.
    var isDirty: Bool { status == .unsaved || status == .saving || status == .error }

    /// The value changed. A change back to the saved value cancels the wait;
    /// any other change waits `delay` from now before saving.
    mutating func change(to value: Value) -> [Effect] {
        let previous = latest
        latest = value
        if value == lastSaved {
            var effects: [Effect] = []
            if isTimerArmed {
                isTimerArmed = false
                effects.append(.cancelTimer)
            }
            if inFlight == nil, status == .unsaved { status = savedAt == nil ? .idle : .saved }
            return effects
        }
        guard isEnabled else {
            if inFlight == nil, status != .error { status = .unsaved }
            return []
        }
        if isTimerArmed, value == previous { return [] }
        attempt = 0
        if inFlight == nil, status != .error { status = .unsaved }
        isTimerArmed = true
        return [.schedule(delay)]
    }

    /// Pauses or resumes saving. Paused, changes still mark the value unsaved.
    mutating func setEnabled(_ enabled: Bool) -> [Effect] {
        guard enabled != isEnabled else { return [] }
        isEnabled = enabled
        if !enabled {
            guard isTimerArmed else { return [] }
            isTimerArmed = false
            return [.cancelTimer]
        }
        guard latest != lastSaved, inFlight == nil else { return [] }
        isTimerArmed = true
        return [.schedule(delay)]
    }

    /// The wait is over: save the newest value.
    mutating func timerFired() -> [Effect] {
        isTimerArmed = false
        return startSave()
    }

    /// Save now after a failure, with the automatic retries reset.
    mutating func retry() -> [Effect] {
        attempt = 0
        return cancellingTimer() + startSave()
    }

    /// Save pending changes now, without waiting.
    mutating func flush() -> [Effect] {
        attempt = 0
        return cancellingTimer() + startSave()
    }

    /// The save in flight finished.
    mutating func saveSucceeded(at date: Date) -> [Effect] {
        guard let saved = inFlight else { return [] }
        inFlight = nil
        lastSaved = saved
        attempt = 0
        savedAt = date
        if latest == saved {
            status = .saved
            savesAfterInFlight = false
            return []
        }
        // Edited during the save.
        status = .unsaved
        if savesAfterInFlight {
            savesAfterInFlight = false
            return cancellingTimer() + startSave()
        }
        guard isEnabled else { return [] }
        isTimerArmed = true
        return [.schedule(delay)]
    }

    /// The save in flight failed. It is retried after `retryDelay`, doubling
    /// each time, up to `retries` times; then the status is `error`.
    mutating func saveFailed() -> [Effect] {
        guard inFlight != nil else { return [] }
        inFlight = nil
        savesAfterInFlight = false
        attempt += 1
        if attempt <= retries {
            isTimerArmed = true
            return [.schedule(Self.backoff(retryDelay, attempt: attempt))]
        }
        status = .error
        return []
    }

    /// The wait before automatic retry `attempt` (1 is the first).
    static func backoff(_ base: Duration, attempt: Int) -> Duration {
        base * (1 << max(0, min(attempt - 1, 30)))
    }

    private mutating func cancellingTimer() -> [Effect] {
        guard isTimerArmed else { return [] }
        isTimerArmed = false
        return [.cancelTimer]
    }

    private mutating func startSave() -> [Effect] {
        if inFlight != nil {
            savesAfterInFlight = true
            return []
        }
        guard latest != lastSaved else { return [] }
        inFlight = latest
        status = .saving
        return [.save(latest)]
    }
}
