import Observation

/// Runs the check of a ``MonarchUsernameAvailabilityInput`` and holds its status.
///
/// The view starts one task per value. SwiftUI cancels the task when the
/// value changes, which ends the wait or the check, and a cancelled check
/// never writes its result, so the status always describes the latest value.
@MainActor
@Observable
final class MonarchUsernameAvailabilityChecker {
    /// Where the check stands.
    private(set) var status: MonarchUsernameAvailabilityStatus = .idle
    /// The check's own message for the latest settled answer.
    private(set) var message: String?

    /// Waits for `debounce` (through `sleep`, which tests replace so no real time passes), then checks `value`. An empty value is idle and
    /// is not checked. A check that throws sets the error status.
    func check(
        _ value: String,
        debounce: Duration,
        sleep: @Sendable (Duration) async throws -> Void = { try await Task.sleep(for: $0) },
        validate: @Sendable (String) async throws -> MonarchUsernameAvailabilityResult?
    ) async {
        guard !value.isEmpty else {
            set(.idle, message: nil)
            return
        }
        set(.checking, message: nil)
        do {
            if debounce > .zero { try await sleep(debounce) }
            let result = try await validate(value)
            guard !Task.isCancelled else { return }
            set(MonarchUsernameAvailabilityInputTokens.status(for: result), message: result?.message)
        } catch {
            guard !Task.isCancelled else { return }
            set(.error, message: nil)
        }
    }

    private func set(_ status: MonarchUsernameAvailabilityStatus, message: String?) {
        if self.status != status { self.status = status }
        if self.message != message { self.message = message }
    }
}
