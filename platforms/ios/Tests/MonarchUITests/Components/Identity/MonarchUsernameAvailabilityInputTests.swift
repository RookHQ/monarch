import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchUsernameAvailabilityInputFixtures: ComponentFixtures {
    static let id = "username-availability-input"

    @MainActor
    private static func field(
        _ status: MonarchUsernameAvailabilityStatus,
        text: String = "ada",
        message: String? = nil,
        prefix: String? = "rook.com/"
    ) -> some View {
        MonarchUsernameAvailabilityField(
            label: "Handle",
            text: .constant(text),
            prompt: "your-name",
            helper: "Letters, numbers and dashes.",
            prefix: prefix,
            status: status,
            statusText: MonarchUsernameAvailabilityInputTokens.text(status: status, message: message),
            retryLabel: MonarchUsernameAvailabilityInputStrings.retry
        ) {}
    }

    static let fixtures = [
        Fixture("component: empty and idle") {
            MonarchUsernameAvailabilityInput("Handle", text: .constant(""), prompt: "your-name", prefix: "rook.com/") {
                _ in nil
            }
        },
        Fixture("idle with helper") { field(.idle, text: "") },
        Fixture("checking") { field(.checking) },
        Fixture("valid") { field(.valid, prefix: nil) },
        Fixture("invalid with the check's message") {
            field(.invalid, message: "That handle is taken. Try ada-l or ada-lovelace.")
        },
        Fixture("error with retry") { field(.error) },
        Fixture("disabled") { field(.valid).disabled(true) },
    ]
}

/// A failure for the check to throw.
private struct CheckFailure: Error {}

@Suite("MonarchUsernameAvailabilityInput")
struct MonarchUsernameAvailabilityInputTests {
    typealias Tokens = MonarchUsernameAvailabilityInputTokens

    @Test("An answer maps to a status; no answer is idle")
    func statusForResult() {
        #expect(Tokens.status(for: nil) == .idle)
        #expect(Tokens.status(for: MonarchUsernameAvailabilityResult(isValid: true)) == .valid)
        #expect(Tokens.status(for: MonarchUsernameAvailabilityResult(isValid: false)) == .invalid)
    }

    @Test("The text is the check's message for a settled answer, otherwise the text of the status")
    func text() {
        #expect(Tokens.text(status: .idle, message: "ignored").isEmpty)
        #expect(Tokens.text(status: .checking, message: "ignored") == "Checking availability…")
        #expect(Tokens.text(status: .valid, message: nil) == "Available")
        #expect(Tokens.text(status: .invalid, message: nil) == "Not available")
        #expect(Tokens.text(status: .invalid, message: "Taken") == "Taken")
        #expect(Tokens.text(status: .error, message: nil) == "Couldn’t check availability.")
    }

    @Test("The app's labels replace the built-in texts, and the check's message wins over both")
    func labels() {
        let labels = Tokens.Labels(checking: "Looking…", valid: "Free", invalid: "Taken", error: "Offline")
        #expect(Tokens.text(status: .checking, message: nil, labels: labels) == "Looking…")
        #expect(Tokens.text(status: .valid, message: nil, labels: labels) == "Free")
        #expect(Tokens.text(status: .invalid, message: nil, labels: labels) == "Taken")
        #expect(Tokens.text(status: .error, message: nil, labels: labels) == "Offline")
        #expect(Tokens.text(status: .invalid, message: "Reserved", labels: labels) == "Reserved")
    }

    @Test("Only settled results are announced")
    func announcement() {
        #expect(Tokens.announcement(status: .idle, message: nil) == nil)
        #expect(Tokens.announcement(status: .checking, message: nil) == nil)
        #expect(Tokens.announcement(status: .valid, message: nil) == "Available")
        #expect(Tokens.announcement(status: .invalid, message: "Taken") == "Taken")
        #expect(Tokens.announcement(status: .error, message: nil) == "Couldn’t check availability.")
    }

    @Test("Each settled status has its own icon and colour")
    func icons() {
        #expect(Tokens.symbol(.idle) == nil)
        #expect(Tokens.symbol(.checking) == nil)
        let symbols = [Tokens.symbol(.valid), Tokens.symbol(.invalid), Tokens.symbol(.error)].compactMap { $0 }
        #expect(Set(symbols).count == 3)
        #expect(Tokens.iconForeground(.valid) == \MonarchColorTokens.successStrong)
        #expect(Tokens.iconForeground(.invalid) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.iconForeground(.error) == \MonarchColorTokens.warningStrong)
        #expect(Tokens.messageForeground(.valid) == \MonarchColorTokens.successStrong)
        #expect(Tokens.messageForeground(.invalid) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.messageForeground(.error) == \MonarchColorTokens.textMuted)
        #expect(Tokens.messageForeground(.checking) == \MonarchColorTokens.textMuted)
    }

    @Test("The border follows the status unless the field is disabled")
    func border() {
        #expect(Tokens.border(status: .idle, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(Tokens.border(status: .checking, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(Tokens.border(status: .valid, isEnabled: true) == \MonarchColorTokens.successBorder)
        #expect(Tokens.border(status: .invalid, isEnabled: true) == \MonarchColorTokens.dangerStrong)
        #expect(Tokens.border(status: .error, isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(Tokens.border(status: .invalid, isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.background(isEnabled: true) == \MonarchColorTokens.bgSurface)
        #expect(Tokens.background(isEnabled: false) == \MonarchColorTokens.bgSubtle)
    }

    @Test("VoiceOver hears the prefix, the helper text and the status text")
    func accessibilityHint() {
        #expect(Tokens.accessibilityHint(prefix: nil, helper: nil, statusText: "").isEmpty)
        #expect(
            Tokens.accessibilityHint(prefix: "rook.com/", helper: "Letters only.", statusText: "Available")
                == "rook.com/, Letters only., Available"
        )
        #expect(Tokens.accessibilityHint(prefix: "", helper: nil, statusText: "Not available") == "Not available")
    }

    @Test("The field matches the text field, and the default wait is 400 milliseconds")
    func sizes() {
        #expect(Tokens.minHeight == MonarchTextFieldTokens.minHeight)
        #expect(Tokens.minHeight >= MonarchAccessibility.minimumHitTarget)
        #expect(Tokens.debounce == .milliseconds(400))
    }

    @Test("Built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchUsernameAvailabilityInputStrings.checking == "Checking availability…")
        #expect(MonarchUsernameAvailabilityInputStrings.valid == "Available")
        #expect(MonarchUsernameAvailabilityInputStrings.invalid == "Not available")
        #expect(MonarchUsernameAvailabilityInputStrings.error == "Couldn’t check availability.")
        #expect(MonarchUsernameAvailabilityInputStrings.retry == "Try again")
    }

    @Test("The check sets valid or invalid and keeps the check's message")
    @MainActor
    func checkSettles() async {
        let checker = MonarchUsernameAvailabilityChecker()
        #expect(checker.status == .idle)
        await checker.check("ada", debounce: .zero) { _ in MonarchUsernameAvailabilityResult(isValid: true) }
        #expect(checker.status == .valid)
        #expect(checker.message == nil)
        await checker.check("root", debounce: .zero) { value in
            MonarchUsernameAvailabilityResult(isValid: false, message: "\(value) is reserved")
        }
        #expect(checker.status == .invalid)
        #expect(checker.message == "root is reserved")
    }

    @Test("An empty value is idle and is never checked; no answer is idle")
    @MainActor
    func checkIdle() async {
        let checker = MonarchUsernameAvailabilityChecker()
        await checker.check("ada", debounce: .zero) { _ in MonarchUsernameAvailabilityResult(isValid: false) }
        await checker.check("", debounce: .zero) { _ in
            Issue.record("An empty value must not be checked")
            return MonarchUsernameAvailabilityResult(isValid: true)
        }
        #expect(checker.status == .idle)
        #expect(checker.message == nil)
        await checker.check("current", debounce: .zero) { _ in nil }
        #expect(checker.status == .idle)
    }

    @Test("A check that throws sets the error status, and a retry can settle it")
    @MainActor
    func checkFails() async {
        let checker = MonarchUsernameAvailabilityChecker()
        await checker.check("ada", debounce: .zero) { _ in throw CheckFailure() }
        #expect(checker.status == .error)
        await checker.check("ada", debounce: .zero) { _ in MonarchUsernameAvailabilityResult(isValid: true) }
        #expect(checker.status == .valid)
    }

    @Test("While it waits and runs, the status is checking")
    @MainActor
    func checkIsCheckingWhileRunning() async {
        let checker = MonarchUsernameAvailabilityChecker()
        let (gate, open) = AsyncStream<Void>.makeStream()
        let task = Task { @MainActor in
            await checker.check("ada", debounce: .zero) { _ in
                for await _ in gate { break }
                return MonarchUsernameAvailabilityResult(isValid: true)
            }
        }
        await Task.yield()
        #expect(checker.status == .checking)
        open.yield()
        await task.value
        #expect(checker.status == .valid)
    }

    /// A wait that never ends by itself: it returns only by throwing when its task is cancelled.
    private static let waitUntilCancelled: @Sendable (Duration) async throws -> Void = { _ in
        while true {
            try Task.checkCancellation()
            await Task.yield()
        }
    }

    @Test("A cancelled check never writes its result, so the status describes the latest value")
    @MainActor
    func staleCheckIsIgnored() async {
        let checker = MonarchUsernameAvailabilityChecker()
        // The wait is injected, so no wall-clock time decides the outcome: it
        // ends only when the task is cancelled, however busy the main actor is.
        // Cancelling straight away also covers a task that is cancelled before it starts.
        let stale = Task { @MainActor in
            await checker.check("ad", debounce: .seconds(1), sleep: Self.waitUntilCancelled) { _ in
                Issue.record("A check cancelled during the wait must not run")
                return MonarchUsernameAvailabilityResult(isValid: false, message: "stale")
            }
        }
        stale.cancel()
        await stale.value
        #expect(checker.status == .checking)
        await checker.check("ada", debounce: .zero) { _ in MonarchUsernameAvailabilityResult(isValid: true) }
        #expect(checker.status == .valid)
        #expect(checker.message == nil)
    }

    @Test("A check cancelled while it runs does not report a failure")
    @MainActor
    func cancelledWhileRunning() async {
        let checker = MonarchUsernameAvailabilityChecker()
        let running = Task { @MainActor in
            await checker.check("ada", debounce: .zero) { _ in
                try await Task.sleep(for: .seconds(30))
                return MonarchUsernameAvailabilityResult(isValid: false)
            }
        }
        await Task.yield()
        running.cancel()
        await running.value
        #expect(checker.status == .checking)
    }
}
