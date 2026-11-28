import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchToastFixtures: ComponentFixtures {
    static let id = "toast"
    static let fixtures = [
        Fixture("toast view, every tone") {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchToastView(
                        toast: MonarchToast("Ticket archived", message: "You can undo this.", tone: tone),
                        dismissLabel: nil,
                        onDismiss: {}
                    )
                }
            }
        },
        Fixture("toast view with action and long text") {
            MonarchToastView(
                toast: MonarchToast(
                    "The export finished and the file was saved to your documents",
                    message: "It includes every ticket closed in the last 90 days.",
                    tone: .success,
                    actionTitle: "Open the file"
                ) {},
                dismissLabel: "Close",
                onDismiss: {}
            )
        },
        Fixture("host at the bottom with two toasts") {
            Text("Content")
                .frame(maxWidth: .infinity, minHeight: 400)
                .monarchToastHost(centerWithTwoToasts())
        },
        Fixture("host at the top, empty") {
            Text("Content")
                .frame(maxWidth: .infinity, minHeight: 200)
                .monarchToastHost(MonarchToastCenter(), position: .top, dismissLabel: "Close")
        },
    ]

    @MainActor
    private static func centerWithTwoToasts() -> MonarchToastCenter {
        let center = MonarchToastCenter()
        center.show("Saved", tone: .success, duration: .infinity)
        center.show("Could not sync", message: "Check your connection.", tone: .danger, duration: .infinity)
        return center
    }
}

@Suite("MonarchToast")
@MainActor
struct MonarchToastTests {
    @Test("A toast stays 5 seconds, or 10 for an error or with an action")
    func defaultLifetime() {
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Saved"), scale: 1) == 5)
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Failed", tone: .danger), scale: 1) == 10)
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Archived", actionTitle: "Undo") {}, scale: 1) == 10)
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Archived", actionTitle: "Undo"), scale: 1) == 5)
    }

    @Test("An explicit duration wins, the scale multiplies it, and infinity keeps the toast")
    func scaledLifetime() {
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Saved", duration: 2), scale: 1) == 2)
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Saved", duration: 2), scale: 3) == 6)
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Saved", duration: .infinity), scale: 1) == nil)
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Saved"), scale: .infinity) == nil)
        #expect(MonarchToastTokens.lifetime(of: MonarchToast("Saved", duration: -4), scale: 1) == 0)
    }

    @Test("The announcement is the tone name, the title, then the message")
    func announcement() {
        #expect(
            MonarchToastTokens.announcement(for: MonarchToast("Could not sync", message: "Try again.", tone: .danger))
                == "Error, Could not sync, Try again."
        )
        #expect(MonarchToastTokens.announcement(for: MonarchToast("Saved")) == "Saved")
        let success = MonarchToast("Saved", message: "", tone: .success)
        #expect(MonarchToastTokens.announcement(for: success) == "Success, Saved")
    }

    @Test("Only the danger tone interrupts speech", arguments: MonarchTone.allCases)
    func urgency(tone: MonarchTone) {
        #expect(MonarchToastTokens.isUrgent(tone: tone) == (tone == .danger))
    }

    @Test("A drag closes a toast only towards its own edge and far enough")
    func drag() {
        let far = MonarchToastTokens.swipeDistance
        #expect(MonarchToastTokens.dragDismisses(translation: far, position: .bottom))
        #expect(!MonarchToastTokens.dragDismisses(translation: far - 1, position: .bottom))
        #expect(!MonarchToastTokens.dragDismisses(translation: -far, position: .bottom))
        #expect(MonarchToastTokens.dragDismisses(translation: -far, position: .top))
        #expect(!MonarchToastTokens.dragDismisses(translation: far, position: .top))
    }

    @Test("The icon takes the strong colour of the tone, and every tone has its own icon")
    func icon() {
        #expect(MonarchToastTokens.iconRole(tone: .success) == \MonarchColorTokens.successStrong)
        #expect(MonarchToastTokens.iconRole(tone: .danger) == \MonarchColorTokens.dangerStrong)
        #expect(Set(MonarchTone.allCases.map(MonarchToastTokens.symbol)).count == MonarchTone.allCases.count)
        #expect(MonarchToastTokens.background == \MonarchColorTokens.bgRaised)
    }

    @Test("The center shows toasts oldest first and returns their ids")
    func show() {
        let center = MonarchToastCenter()
        let first = center.show("First", duration: .infinity)
        let second = center.show(MonarchToast("Second", duration: .infinity))
        #expect(center.toasts.map(\.title) == ["First", "Second"])
        #expect(center.toasts.map(\.id) == [first, second])
    }

    @Test("Over the limit, the oldest toasts close first")
    func limit() {
        let center = MonarchToastCenter(limit: 2)
        for title in ["One", "Two", "Three", "Four"] {
            center.show(title, duration: .infinity)
        }
        #expect(center.toasts.map(\.title) == ["Three", "Four"])
        #expect(MonarchToastCenter(limit: 0).limit == 1)
        #expect(MonarchToastCenter().limit == 3)
    }

    @Test("Dismiss closes one toast, ignores an unknown id, and dismissAll closes every toast")
    func dismiss() {
        let center = MonarchToastCenter()
        let first = center.show("First", duration: .infinity)
        center.show("Second", duration: .infinity)
        center.dismiss(UUID())
        #expect(center.toasts.count == 2)
        center.dismiss(first)
        #expect(center.toasts.map(\.title) == ["Second"])
        center.dismiss(first)
        #expect(center.toasts.count == 1)
        center.dismissAll()
        #expect(center.toasts.isEmpty)
    }

    @Test("A toast closes itself after its duration, and a kept toast does not")
    func timer() async {
        let sleeper = ToastSleeper()
        let center = MonarchToastCenter(limit: 3, durationScale: 1, sleeper: sleeper.sleep)
        let brief = center.show("Brief", duration: 0.05)
        center.show("Kept", duration: .infinity)
        #expect(center.toasts.count == 2)
        await sleeper.untilRequested(1)
        #expect(sleeper.requested == [.seconds(0.05)])
        #expect(center.toasts.count == 2)
        sleeper.releaseAll()
        await center.waitForTimer(brief)
        #expect(center.toasts.map(\.title) == ["Kept"])
    }

    @Test("The duration scale of the center stretches the timer, and infinity starts none")
    func scaledTimer() async {
        let stretched = ToastSleeper()
        let center = MonarchToastCenter(limit: 3, durationScale: 2, sleeper: stretched.sleep)
        let id = center.show("Brief", duration: 1)
        await stretched.untilRequested(1)
        #expect(stretched.requested == [.seconds(2)])
        stretched.releaseAll()
        await center.waitForTimer(id)
        #expect(center.toasts.isEmpty)

        let never = ToastSleeper()
        let kept = MonarchToastCenter(limit: 3, durationScale: .infinity, sleeper: never.sleep)
        let keptID = kept.show("Brief", duration: 0.01)
        await kept.waitForTimer(keptID)
        #expect(never.requested.isEmpty)
        #expect(kept.toasts.count == 1)
    }

    @Test("The dismiss name comes from the component's strings table")
    func strings() {
        #expect(MonarchToastStrings.dismiss == "Dismiss")
    }
}

@Suite("MonarchToast onDismiss")
@MainActor
struct MonarchToastDismissTests {
    @Test("onDismiss runs once when the toast is dismissed, and not again")
    func dismissOnce() {
        let center = MonarchToastCenter()
        var count = 0
        let id = center.show("Archived", duration: .infinity, onDismiss: { count += 1 })
        center.dismiss(id)
        center.dismiss(id)
        #expect(count == 1)
    }

    @Test("onDismiss runs when a newer toast pushes the toast out")
    func pushedOut() {
        let center = MonarchToastCenter(limit: 1)
        var dismissed: [String] = []
        center.show("First", duration: .infinity, onDismiss: { dismissed.append("First") })
        center.show("Second", duration: .infinity, onDismiss: { dismissed.append("Second") })
        #expect(dismissed == ["First"])
        center.dismissAll()
        #expect(dismissed == ["First", "Second"])
    }

    @Test("onDismiss runs when the time runs out")
    func timeout() async {
        let sleeper = ToastSleeper()
        let center = MonarchToastCenter(limit: 3, durationScale: 1, sleeper: sleeper.sleep)
        var count = 0
        let id = center.show("Saved", duration: 0.01, onDismiss: { count += 1 })
        await sleeper.untilRequested(1)
        #expect(count == 0)
        sleeper.releaseAll()
        await center.waitForTimer(id)
        #expect(count == 1)
        #expect(center.toasts.isEmpty)
    }
}

/// Stands in for the wall clock: a sleep lasts until the test releases it, so no test waits on time.
private final class ToastSleeper: @unchecked Sendable {
    private let lock = NSLock()
    private var waiting: [CheckedContinuation<Void, Never>] = []
    private var recorded: [Duration] = []
    /// Tests waiting for a number of sleeps to start.
    private var watchers: [(count: Int, continuation: CheckedContinuation<Void, Never>)] = []

    /// The durations asked for, in order.
    var requested: [Duration] { lock.withLock { recorded } }

    func sleep(_ duration: Duration) async throws {
        await withCheckedContinuation { continuation in
            let ready = lock.withLock {
                recorded.append(duration)
                waiting.append(continuation)
                let count = recorded.count
                let ready = watchers.filter { $0.count <= count }.map(\.continuation)
                watchers.removeAll { $0.count <= count }
                return ready
            }
            for watcher in ready { watcher.resume() }
        }
    }

    /// Returns once `count` sleeps have started. It waits for the timer
    /// task however long the scheduler takes to run it, so the test does not
    /// depend on the load of the machine (a fixed number of yields did).
    func untilRequested(_ count: Int) async {
        await withCheckedContinuation { continuation in
            let isReady = lock.withLock {
                if recorded.count >= count { return true }
                watchers.append((count, continuation))
                return false
            }
            if isReady { continuation.resume() }
        }
    }

    /// Ends every sleep.
    func releaseAll() {
        let pending = lock.withLock {
            defer { waiting = [] }
            return waiting
        }
        for continuation in pending { continuation.resume() }
    }
}
