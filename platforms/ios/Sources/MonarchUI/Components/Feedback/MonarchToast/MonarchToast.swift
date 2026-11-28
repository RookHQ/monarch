import Foundation

/// A transient notification, shown by a ``MonarchToastCenter`` through the
/// host installed with `monarchToastHost(_:position:dismissLabel:)`.
///
/// ```swift
/// toasts.show(MonarchToast("Ticket archived", tone: .success, actionTitle: "Undo") { undo() })
/// ```
public struct MonarchToast: Identifiable, Sendable {
    /// The identity of the toast, used to dismiss it.
    public let id: UUID
    /// A short headline, for example "Ticket assigned".
    public let title: String
    /// One line of detail.
    public let message: String?
    /// The meaning, which selects the icon and its colour.
    public let tone: MonarchTone
    /// The title of the action button, for example "Undo".
    public let actionTitle: String?
    /// How long the toast stays, in seconds. `nil` uses the default for the
    /// tone; `.infinity` keeps the toast until it is dismissed.
    public let duration: TimeInterval?
    /// Runs when the action button is activated. The toast then closes.
    public let action: (@MainActor () -> Void)?
    /// Runs once when the toast closes for any reason: its time ran out,
    /// the person dismissed or swiped it, a newer toast pushed it out, or
    /// the app dismissed it. After the action button it runs after `action`.
    /// An undo toast commits the change here unless the action undid it.
    public let onDismiss: (@MainActor () -> Void)?

    /// A toast.
    ///
    /// - Parameters:
    ///   - title: A short headline, already localised.
    ///   - message: One line of detail.
    ///   - tone: The meaning, which selects the icon and its colour.
    ///   - actionTitle: The title of the action button. No button without it.
    ///   - duration: Seconds before the toast closes itself. `nil` is 5
    ///     seconds, or 10 for the danger tone and for a toast with an action.
    ///     `.infinity` keeps it until dismissed.
    ///   - action: Runs when the action button is activated. The toast then closes.
    ///   - onDismiss: Runs once when the toast closes, for any reason.
    public init(
        _ title: String,
        message: String? = nil,
        tone: MonarchTone = .neutral,
        actionTitle: String? = nil,
        duration: TimeInterval? = nil,
        action: (@MainActor () -> Void)? = nil,
        onDismiss: (@MainActor () -> Void)? = nil
    ) {
        self.id = UUID()
        self.title = title
        self.message = message
        self.tone = tone
        self.actionTitle = actionTitle
        self.duration = duration
        self.action = action
        self.onDismiss = onDismiss
    }

    /// Whether the toast shows an action button.
    var hasAction: Bool { actionTitle != nil && action != nil }
}
