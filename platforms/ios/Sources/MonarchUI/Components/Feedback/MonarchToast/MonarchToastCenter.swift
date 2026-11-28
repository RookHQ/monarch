import Foundation
import Observation
import SwiftUI

/// Holds the toasts on screen. Create one for a scene, install it with
/// `monarchToastHost(_:position:dismissLabel:)`, and show toasts from any
/// view below it.
///
/// ```swift
/// @State private var toasts = MonarchToastCenter()
///
/// var body: some View {
///     ContentView()
///         .monarchToastHost(toasts)
/// }
///
/// // In a descendant:
/// @Environment(MonarchToastCenter.self) private var toasts
/// toasts.show("Saved", tone: .success)
/// ```
///
/// Each toast closes itself after its duration, and is announced to
/// VoiceOver when it is shown. A toast's `onDismiss` runs once when it
/// closes, whatever closed it.
@MainActor
@Observable
public final class MonarchToastCenter {
    /// The toasts on screen, oldest first.
    public private(set) var toasts: [MonarchToast] = []

    /// How many toasts are shown at once. Older ones close first.
    public let limit: Int

    /// Multiplies every duration. Set it from a user preference: `2`
    /// doubles the time, `.infinity` turns closing by time off.
    public var durationScale: Double

    @ObservationIgnored private var timers: [UUID: Task<Void, Never>] = [:]
    /// Waits for a toast's lifetime. Tests replace it so no test waits on the wall clock.
    @ObservationIgnored private let sleeper: @Sendable (Duration) async throws -> Void

    /// A toast center.
    ///
    /// - Parameters:
    ///   - limit: How many toasts are shown at once. At least 1.
    ///   - durationScale: Multiplies every duration.
    public init(limit: Int = 3, durationScale: Double = 1) {
        self.limit = MonarchToastTokens.limit(limit)
        self.durationScale = durationScale
        self.sleeper = { try await Task.sleep(for: $0) }
    }

    /// A center whose timers wait through `sleeper`, for tests that must not wait on the wall clock.
    init(limit: Int, durationScale: Double, sleeper: @escaping @Sendable (Duration) async throws -> Void) {
        self.limit = MonarchToastTokens.limit(limit)
        self.durationScale = durationScale
        self.sleeper = sleeper
    }

    /// Returns when the timer of the toast has finished or been cancelled. For tests.
    func waitForTimer(_ id: UUID) async {
        await timers[id]?.value
    }

    /// Shows a toast, announces it, and starts its timer.
    ///
    /// - Parameter toast: The toast.
    /// - Returns: The id of the toast, for `dismiss(_:)`.
    @discardableResult
    public func show(_ toast: MonarchToast) -> UUID {
        let next = MonarchToastTokens.adding(toast, to: toasts, limit: limit)
        let kept = Set(next.map(\.id))
        for id in timers.keys where !kept.contains(id) {
            cancelTimer(id)
        }
        let pushedOut = toasts.filter { !kept.contains($0.id) }
        toasts = next
        pushedOut.forEach(notifyDismissed)
        announce(toast)
        if let lifetime = MonarchToastTokens.lifetime(of: toast, scale: durationScale) {
            let id = toast.id
            let sleeper = sleeper
            timers[id] = Task { [weak self] in
                try? await sleeper(.seconds(lifetime))
                guard !Task.isCancelled else { return }
                self?.dismiss(id)
            }
        }
        return toast.id
    }

    /// Shows a toast made from its parts.
    ///
    /// - Parameters:
    ///   - title: A short headline, already localised.
    ///   - message: One line of detail.
    ///   - tone: The meaning, which selects the icon and its colour.
    ///   - actionTitle: The title of the action button. No button without it.
    ///   - duration: Seconds before the toast closes itself. See ``MonarchToast``.
    ///   - action: Runs when the action button is activated.
    ///   - onDismiss: Runs once when the toast closes, for any reason.
    /// - Returns: The id of the toast, for `dismiss(_:)`.
    @discardableResult
    public func show(
        _ title: String,
        message: String? = nil,
        tone: MonarchTone = .neutral,
        actionTitle: String? = nil,
        duration: TimeInterval? = nil,
        action: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) -> UUID {
        show(
            MonarchToast(
                title,
                message: message,
                tone: tone,
                actionTitle: actionTitle,
                duration: duration,
                action: action,
                onDismiss: onDismiss
            )
        )
    }

    /// Closes one toast. Does nothing if it has already closed.
    ///
    /// - Parameter id: The id `show` returned.
    public func dismiss(_ id: UUID) {
        cancelTimer(id)
        guard let index = toasts.firstIndex(where: { $0.id == id }) else { return }
        let closed = toasts.remove(at: index)
        notifyDismissed(closed)
    }

    /// Closes every toast.
    public func dismissAll() {
        for id in Array(timers.keys) {
            cancelTimer(id)
        }
        let closed = toasts
        toasts.removeAll()
        closed.forEach(notifyDismissed)
    }

    /// Runs a closed toast's `onDismiss`. Each toast is removed from the
    /// list before this runs, so it runs at most once per toast.
    private func notifyDismissed(_ toast: MonarchToast) {
        toast.onDismiss?()
    }

    private func cancelTimer(_ id: UUID) {
        timers.removeValue(forKey: id)?.cancel()
    }

    private func announce(_ toast: MonarchToast) {
        var text = AttributedString(MonarchToastTokens.announcement(for: toast))
        text.accessibilitySpeechAnnouncementPriority = MonarchToastTokens.isUrgent(tone: toast.tone) ? .high : .default
        AccessibilityNotification.Announcement(text).post()
    }
}
