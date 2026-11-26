import Foundation
import Observation

/// Saves a value automatically after the person stops changing it:
/// debounced, one save at a time, retried with back-off on failure, and
/// flushable on demand. Show its state with ``MonarchAutosaveIndicator``.
///
/// ```swift
/// @State private var autosave = MonarchAutosave("") { text in
///     try await api.saveDraft(text)
/// }
///
/// TextEditor(text: $draft)
///     .onChange(of: draft) { autosave.update(draft) }
/// MonarchAutosaveIndicator(autosave)
/// ```
///
/// Each change waits `delay` before saving; a newer change restarts the
/// wait. Only one save runs at a time: a change made during a save is saved
/// after it. A failed save is retried `retries` times, after `retryDelay`
/// and then twice as long each time, before the status becomes `error`.
/// Call ``flush()`` before leaving a screen and ``retry()`` after an error.
///
/// The clock and the source of the current date can be replaced, so tests
/// and previews never wait for real time.
@MainActor
@Observable
public final class MonarchAutosave<Value: Equatable & Sendable> {
    private var machine: MonarchAutosaveIndicatorMachine<Value>
    private var lastError: (any Error)?

    @ObservationIgnored private let clock: any Clock<Duration>
    @ObservationIgnored private let now: @Sendable () -> Date
    @ObservationIgnored private let save: @MainActor (Value) async throws -> Void
    @ObservationIgnored private var timer: Task<Void, Never>?
    @ObservationIgnored private var flushWaiters: [CheckedContinuation<Bool, Never>] = []
    @ObservationIgnored private var settleWaiters: [CheckedContinuation<Void, Never>] = []

    /// An autosave for a value.
    ///
    /// - Parameters:
    ///   - value: The current value, taken as saved.
    ///   - delay: How long to wait after the last change before saving.
    ///   - retries: Automatic retries after a failed save, before the status becomes `error`.
    ///   - retryDelay: The wait before the first automatic retry; it doubles each time.
    ///   - savedAt: When `value` was last saved, if known. Shown as "Saved · 3 minutes ago".
    ///   - clock: The clock the waits are measured on.
    ///   - now: The current date, recorded when a save finishes.
    ///   - save: Saves a value. Throw to report a failure.
    public init(
        _ value: Value,
        delay: Duration = .seconds(1),
        retries: Int = 2,
        retryDelay: Duration = .seconds(2),
        savedAt: Date? = nil,
        clock: any Clock<Duration> = ContinuousClock(),
        now: @escaping @Sendable () -> Date = { Date() },
        save: @escaping @MainActor (Value) async throws -> Void
    ) {
        machine = MonarchAutosaveIndicatorMachine(
            value: value,
            savedAt: savedAt,
            delay: delay,
            retries: retries,
            retryDelay: retryDelay
        )
        self.clock = clock
        self.now = now
        self.save = save
    }

    /// Where saving stands.
    public var status: MonarchSaveStatus { machine.status }

    /// When the last save finished, or the initial `savedAt`.
    public var savedAt: Date? { machine.savedAt }

    /// The newest value given to ``update(_:)``.
    public var value: Value { machine.latest }

    /// The error of the last failed save while the status is `error`.
    public var error: (any Error)? { machine.status == .error ? lastError : nil }

    /// Whether some changes are not saved yet: unsaved, saving or failed.
    /// Use it to warn before leaving.
    public var isDirty: Bool { machine.isDirty }

    /// Whether changes are saved automatically. Turn it off while the value
    /// is invalid; changes are still marked unsaved, and saving resumes when
    /// it is turned back on.
    public var isEnabled: Bool {
        get { machine.isEnabled }
        set { perform(machine.setEnabled(newValue)) }
    }

    /// Records a new value and saves it after the delay.
    public func update(_ value: Value) {
        perform(machine.change(to: value))
    }

    /// Saves now after a failure, with the automatic retries reset.
    public func retry() {
        perform(machine.retry())
    }

    /// Saves pending changes now, skipping the delay, for example before
    /// leaving a screen.
    ///
    /// - Returns: `true` when everything is saved, `false` when the save failed.
    @discardableResult
    public func flush() async -> Bool {
        perform(machine.flush())
        guard machine.inFlight != nil else { return machine.latest == machine.lastSaved }
        return await withCheckedContinuation { flushWaiters.append($0) }
    }

    /// Returns when no save is in flight. For tests.
    func settled() async {
        guard machine.inFlight != nil else { return }
        await withCheckedContinuation { settleWaiters.append($0) }
    }

    private func perform(_ effects: [MonarchAutosaveIndicatorMachine<Value>.Effect]) {
        for effect in effects {
            switch effect {
            case .cancelTimer:
                timer?.cancel()
                timer = nil
            case .schedule(let duration):
                timer?.cancel()
                let sleep = clock.monarchAutosaveIndicatorSleeper(after: duration)
                timer = Task { [weak self] in
                    do { try await sleep() } catch { return }
                    guard let self, !Task.isCancelled else { return }
                    self.timer = nil
                    self.perform(self.machine.timerFired())
                }
            case .save(let value):
                Task { [weak self] in await self?.run(value) }
            }
        }
    }

    private func run(_ value: Value) async {
        let succeeded: Bool
        do {
            try await save(value)
            lastError = nil
            perform(machine.saveSucceeded(at: now()))
            succeeded = true
        } catch {
            lastError = error
            perform(machine.saveFailed())
            succeeded = false
        }
        if !succeeded || machine.inFlight == nil {
            let waiters = flushWaiters
            flushWaiters = []
            let result = succeeded && machine.latest == machine.lastSaved
            for waiter in waiters { waiter.resume(returning: result) }
        }
        if machine.inFlight == nil {
            let waiters = settleWaiters
            settleWaiters = []
            for waiter in waiters { waiter.resume() }
        }
    }

}

extension Clock where Duration == Swift.Duration {
    /// Waits until `duration` from now, with the deadline fixed when this is
    /// called, so the wait does not depend on when the task starts.
    func monarchAutosaveIndicatorSleeper(after duration: Duration) -> @Sendable () async throws -> Void {
        let deadline = now.advanced(by: duration)
        return { try await self.sleep(until: deadline, tolerance: nil) }
    }
}
