import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAutosaveIndicatorFixtures: ComponentFixtures {
    static let id = "autosave-indicator"

    @MainActor
    static func edited(enabled: Bool) -> MonarchAutosave<String> {
        let autosave = MonarchAutosave("Draft", clock: MonarchAutosaveIndicatorTestClock()) { _ in }
        autosave.isEnabled = enabled
        autosave.update("Draft, edited")
        return autosave
    }

    static let fixtures = [
        Fixture("idle draws nothing") {
            MonarchAutosaveIndicator(MonarchAutosave("Draft") { _ in })
        },
        Fixture("saved earlier") {
            MonarchAutosaveIndicator(
                MonarchAutosave("Draft", savedAt: Date(timeIntervalSince1970: 1_800_000_000)) { _ in }
            )
        },
        Fixture("unsaved") {
            MonarchAutosaveIndicator(edited(enabled: false))
        },
        Fixture("waiting to save, custom labels") {
            MonarchAutosaveIndicator(
                edited(enabled: true),
                announcesSaving: true,
                labels: MonarchSaveStatusIndicatorLabels(unsaved: "Not saved yet")
            )
        },
    ]
}

/// A clock that moves only when the test advances it.
final class MonarchAutosaveIndicatorTestClock: Clock, @unchecked Sendable {
    struct Instant: InstantProtocol {
        var offset: Duration
        func advanced(by duration: Duration) -> Instant { Instant(offset: offset + duration) }
        func duration(to other: Instant) -> Duration { other.offset - offset }
        static func < (lhs: Instant, rhs: Instant) -> Bool { lhs.offset < rhs.offset }
    }

    private struct Sleeper {
        let deadline: Instant
        let continuation: CheckedContinuation<Void, Never>
    }

    private let lock = NSLock()
    private var current = Instant(offset: .zero)
    private var sleepers: [Sleeper] = []

    var now: Instant { lock.withLock { current } }
    var minimumResolution: Duration { .zero }

    func sleep(until deadline: Instant, tolerance: Duration?) async throws {
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            let resumeNow = lock.withLock {
                if deadline <= current { return true }
                sleepers.append(Sleeper(deadline: deadline, continuation: continuation))
                return false
            }
            if resumeNow { continuation.resume() }
        }
        try Task.checkCancellation()
    }

    /// Moves time forward and wakes every sleeper whose deadline has passed.
    func advance(by duration: Duration) {
        let due = lock.withLock {
            current = current.advanced(by: duration)
            let due = sleepers.filter { $0.deadline <= current }
            sleepers.removeAll { $0.deadline <= current }
            return due
        }
        for sleeper in due { sleeper.continuation.resume() }
    }
}

/// Records saves and lets a test wait for the next one.
@MainActor
private final class MonarchAutosaveIndicatorTestSaver {
    var saved: [String] = []
    var failuresLeft = 0
    private var waiters: [CheckedContinuation<String, Never>] = []

    struct Failure: Error {}

    func save(_ value: String) throws {
        saved.append(value)
        let waiting = waiters
        waiters = []
        for waiter in waiting { waiter.resume(returning: value) }
        if failuresLeft > 0 {
            failuresLeft -= 1
            throw Failure()
        }
    }

    /// The next value saved (or the latest, if one was saved since `count`).
    func next(after count: Int) async -> String {
        if saved.count > count { return saved[count] }
        return await withCheckedContinuation { waiters.append($0) }
    }
}

@Suite("MonarchAutosaveIndicator")
@MainActor
struct MonarchAutosaveIndicatorTests {
    private typealias Machine = MonarchAutosaveIndicatorMachine<String>
    private let date = Date(timeIntervalSince1970: 1_800_000_000)

    private func machine(savedAt: Date? = nil) -> Machine {
        Machine(value: "a", savedAt: savedAt, delay: .seconds(1), retries: 2, retryDelay: .seconds(2))
    }

    @Test("It starts idle, or saved when the last save time is known")
    func initial() {
        #expect(machine().status == .idle)
        #expect(machine(savedAt: date).status == .saved)
        #expect(!machine().isDirty)
    }

    @Test("A change waits for the delay, then saves the newest value")
    func debounce() {
        var m = machine()
        #expect(m.change(to: "b") == [.schedule(.seconds(1))])
        #expect(m.status == .unsaved)
        #expect(m.isDirty)
        #expect(m.change(to: "c") == [.schedule(.seconds(1))])
        #expect(m.timerFired() == [.save("c")])
        #expect(m.status == .saving)
        #expect(m.saveSucceeded(at: date) == [])
        #expect(m.status == .saved)
        #expect(m.savedAt == date)
        #expect(!m.isDirty)
    }

    @Test("The same value does not push the save back")
    func sameValue() {
        var m = machine()
        _ = m.change(to: "b")
        #expect(m.change(to: "b") == [])
    }

    @Test("Changing back to the saved value cancels the wait")
    func changeBack() {
        var m = machine(savedAt: date)
        _ = m.change(to: "b")
        #expect(m.change(to: "a") == [.cancelTimer])
        #expect(m.status == .saved)
        var fresh = machine()
        _ = fresh.change(to: "b")
        _ = fresh.change(to: "a")
        #expect(fresh.status == .idle)
    }

    @Test("Only one save runs at a time; edits during a save are saved after it")
    func oneAtATime() {
        var m = machine()
        _ = m.change(to: "b")
        _ = m.timerFired()
        #expect(m.change(to: "c") == [.schedule(.seconds(1))])
        #expect(m.status == .saving)
        // The timer fires while "b" is still saving: nothing starts.
        #expect(m.timerFired() == [])
        // When "b" finishes, "c" is saved right away.
        #expect(m.saveSucceeded(at: date) == [.save("c")])
        #expect(m.status == .saving)
        #expect(m.saveSucceeded(at: date) == [])
        #expect(m.status == .saved)
    }

    @Test("An edit during a save is saved after the delay when nothing asked sooner")
    func editDuringSave() {
        var m = machine()
        _ = m.change(to: "b")
        _ = m.timerFired()
        _ = m.change(to: "c")
        #expect(m.saveSucceeded(at: date) == [.schedule(.seconds(1))])
        #expect(m.status == .unsaved)
    }

    @Test("A failed save is retried with back-off, then reports the error")
    func retries() {
        var m = machine()
        _ = m.change(to: "b")
        _ = m.timerFired()
        #expect(m.saveFailed() == [.schedule(.seconds(2))])
        #expect(m.status == .saving)
        #expect(m.timerFired() == [.save("b")])
        #expect(m.saveFailed() == [.schedule(.seconds(4))])
        #expect(m.timerFired() == [.save("b")])
        #expect(m.saveFailed() == [])
        #expect(m.status == .error)
        #expect(m.isDirty)
        // Retry saves at once with the retries reset.
        #expect(m.retry() == [.save("b")])
        #expect(m.attempt == 0)
        #expect(m.saveSucceeded(at: date) == [])
        #expect(m.status == .saved)
    }

    @Test("Back-off doubles each time")
    func backoff() {
        #expect(Machine.backoff(.seconds(2), attempt: 1) == .seconds(2))
        #expect(Machine.backoff(.seconds(2), attempt: 2) == .seconds(4))
        #expect(Machine.backoff(.seconds(2), attempt: 3) == .seconds(8))
    }

    @Test("An edit after an error keeps the error until the next save starts")
    func editAfterError() {
        var m = Machine(value: "a", savedAt: nil, delay: .seconds(1), retries: 0, retryDelay: .seconds(2))
        _ = m.change(to: "b")
        _ = m.timerFired()
        _ = m.saveFailed()
        #expect(m.status == .error)
        #expect(m.change(to: "c") == [.schedule(.seconds(1))])
        #expect(m.status == .error)
        #expect(m.timerFired() == [.save("c")])
        #expect(m.status == .saving)
    }

    @Test("Flush saves at once and skips the delay")
    func flush() {
        var m = machine()
        _ = m.change(to: "b")
        #expect(m.flush() == [.cancelTimer, .save("b")])
        var clean = machine()
        #expect(clean.flush() == [])
    }

    @Test("Paused, changes are marked unsaved but not saved; resuming saves them")
    func paused() {
        var m = machine()
        #expect(m.setEnabled(false) == [])
        #expect(m.change(to: "b") == [])
        #expect(m.status == .unsaved)
        #expect(m.setEnabled(true) == [.schedule(.seconds(1))])
        _ = m.change(to: "c")
        #expect(m.setEnabled(false) == [.cancelTimer])
    }

    @Test("The error message is the app's text only in the error status")
    func errorMessage() {
        struct Offline: Error {}
        let transform: (any Error) -> String = { _ in "You are offline" }
        #expect(
            MonarchAutosaveIndicatorTokens.errorMessage(status: .error, error: Offline(), transform: transform)
                == "You are offline"
        )
        #expect(
            MonarchAutosaveIndicatorTokens.errorMessage(status: .saved, error: Offline(), transform: transform) == nil
        )
        #expect(MonarchAutosaveIndicatorTokens.errorMessage(status: .error, error: Offline(), transform: nil) == nil)
        #expect(
            MonarchAutosaveIndicatorTokens.errorMessage(status: .error, error: Offline(), transform: { _ in "" }) == nil
        )
    }

    @Test("The autosave saves after the delay on its clock and records the date")
    func controllerDebounce() async {
        let clock = MonarchAutosaveIndicatorTestClock()
        let saver = MonarchAutosaveIndicatorTestSaver()
        let date = date
        let autosave = MonarchAutosave("a", clock: clock, now: { date }, save: { try saver.save($0) })
        autosave.update("b")
        #expect(autosave.status == .unsaved)
        clock.advance(by: .milliseconds(999))
        #expect(saver.saved.isEmpty)
        clock.advance(by: .milliseconds(1))
        #expect(await saver.next(after: 0) == "b")
        await autosave.settled()
        #expect(autosave.status == .saved)
        #expect(autosave.savedAt == date)
        #expect(!autosave.isDirty)
    }

    @Test("Flush saves without waiting and reports the outcome")
    func controllerFlush() async {
        let saver = MonarchAutosaveIndicatorTestSaver()
        let autosave = MonarchAutosave("a", retries: 0, clock: MonarchAutosaveIndicatorTestClock()) {
            try saver.save($0)
        }
        #expect(await autosave.flush())
        #expect(saver.saved.isEmpty)
        autosave.update("b")
        #expect(await autosave.flush())
        #expect(saver.saved == ["b"])
        saver.failuresLeft = 1
        autosave.update("c")
        #expect(await autosave.flush() == false)
        #expect(autosave.status == .error)
        #expect(autosave.error is MonarchAutosaveIndicatorTestSaver.Failure)
        autosave.retry()
        await autosave.settled()
        #expect(autosave.status == .saved)
        #expect(autosave.error == nil)
        #expect(saver.saved == ["b", "c", "c"])
    }

    @Test("Automatic retries wait for the back-off on the clock")
    func controllerRetry() async {
        let clock = MonarchAutosaveIndicatorTestClock()
        let saver = MonarchAutosaveIndicatorTestSaver()
        saver.failuresLeft = 1
        let autosave = MonarchAutosave("a", retries: 1, retryDelay: .seconds(2), clock: clock) {
            try saver.save($0)
        }
        autosave.update("b")
        #expect(await autosave.flush() == false)
        #expect(autosave.status == .saving)
        clock.advance(by: .seconds(2))
        #expect(await saver.next(after: 1) == "b")
        await autosave.settled()
        #expect(autosave.status == .saved)
    }
}
