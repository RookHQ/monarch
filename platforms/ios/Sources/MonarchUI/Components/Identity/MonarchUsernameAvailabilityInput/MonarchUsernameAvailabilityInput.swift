import SwiftUI

/// A text field whose value is checked while the person types: handles,
/// workspace addresses, slugs.
///
/// The app supplies the check. The component waits for typing to pause,
/// cancels a check when the text changes again, shows the status as an icon
/// and a message, and announces the result.
///
/// ```swift
/// MonarchUsernameAvailabilityInput("Handle", text: $handle, prefix: "rook.com/") { handle in
///     let isFree = try await api.isFree(handle)
///     return MonarchUsernameAvailabilityResult(isValid: isFree)
/// }
/// ```
///
/// Return `nil` from the check when there is nothing to check, such as the
/// person's current handle. The check runs in a task that is cancelled when
/// the text changes, so `URLSession` requests made in it stop by themselves.
/// Monarch does no networking of its own.
///
/// Accessibility: the field is one element whose label is `label`; its hint
/// is the prefix, the helper text and the status message. Settled results
/// ("Available", "Not available", a failure) are announced; "Checking" is
/// not, because it would be read on every pause. Each status has its own icon
/// and message, not colour alone. After a failure a "Try again" button with a
/// 44-point touch target runs the check again.
public struct MonarchUsernameAvailabilityInput: View {
    private let label: String
    @Binding private var text: String
    private let prompt: String?
    private let helper: String?
    private let isRequired: Bool
    private let prefix: String?
    private let debounce: Duration
    private let labels: MonarchUsernameAvailabilityInputTokens.Labels
    private let retryLabel: String?
    private let transform: ((String) -> String)?
    private let onStatusChange: (@MainActor (MonarchUsernameAvailabilityStatus, String?) -> Void)?
    private let validate: @Sendable (String) async throws -> MonarchUsernameAvailabilityResult?

    @State private var checker = MonarchUsernameAvailabilityChecker()
    @State private var attempt = 0
    @State private var skipsDebounce = false

    /// The value and the attempt that one check task belongs to.
    private struct CheckID: Hashable {
        let value: String
        let attempt: Int
    }

    /// The status and message, to observe as one value.
    private struct Outcome: Equatable {
        let status: MonarchUsernameAvailabilityStatus
        let message: String?
    }

    /// A username availability input.
    ///
    /// - Parameters:
    ///   - label: The visible label, already localised.
    ///   - text: The text to edit and check.
    ///   - prompt: Placeholder text shown while the field is empty.
    ///   - helper: Supporting text under the field, shown while there is no status message.
    ///   - prefix: Fixed text before the value, such as a domain.
    ///   - isRequired: Shows a mark after the label, as the other fields do.
    ///   - debounce: How long typing must pause before the check runs.
    ///   - checkingLabel: Replaces the built-in "Checking availability…".
    ///   - validLabel: Replaces the built-in "Available".
    ///   - invalidLabel: Replaces the built-in "Not available".
    ///   - errorLabel: Replaces the built-in "Couldn’t check availability."
    ///   - retryLabel: Replaces the built-in "Try again" title of the button shown after a failure.
    ///   - transform: Rewrites typed text before it is stored, such as lower-casing.
    ///   - onStatusChange: Runs when the status or its message changes, so a form can wait for `valid`.
    ///   - validate: The check. Return `nil` when there is nothing to check. Throwing sets the error status.
    public init(
        _ label: String,
        text: Binding<String>,
        prompt: String? = nil,
        helper: String? = nil,
        prefix: String? = nil,
        isRequired: Bool = false,
        debounce: Duration = .milliseconds(400),
        checkingLabel: String? = nil,
        validLabel: String? = nil,
        invalidLabel: String? = nil,
        errorLabel: String? = nil,
        retryLabel: String? = nil,
        transform: ((String) -> String)? = nil,
        onStatusChange: (@MainActor (MonarchUsernameAvailabilityStatus, String?) -> Void)? = nil,
        validate: @escaping @Sendable (String) async throws -> MonarchUsernameAvailabilityResult?
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.helper = helper
        self.prefix = prefix
        self.isRequired = isRequired
        self.debounce = debounce
        self.labels = MonarchUsernameAvailabilityInputTokens.Labels(
            checking: checkingLabel,
            valid: validLabel,
            invalid: invalidLabel,
            error: errorLabel
        )
        self.retryLabel = retryLabel
        self.transform = transform
        self.onStatusChange = onStatusChange
        self.validate = validate
    }

    private var editedText: Binding<String> {
        guard let transform else { return $text }
        return Binding(
            get: { text },
            set: { text = transform($0) }
        )
    }

    /// The content of the view.
    public var body: some View {
        MonarchUsernameAvailabilityField(
            label: label,
            text: editedText,
            prompt: prompt,
            helper: helper,
            prefix: prefix,
            isRequired: isRequired,
            status: checker.status,
            statusText: MonarchUsernameAvailabilityInputTokens.text(
                status: checker.status,
                message: checker.message,
                labels: labels
            ),
            retryLabel: retryLabel ?? MonarchUsernameAvailabilityInputStrings.retry
        ) {
            skipsDebounce = true
            attempt += 1
        }
        .task(id: CheckID(value: text, attempt: attempt)) {
            let wait: Duration = skipsDebounce ? .zero : debounce
            skipsDebounce = false
            await checker.check(text, debounce: wait, validate: validate)
        }
        .onChange(of: Outcome(status: checker.status, message: checker.message)) { _, outcome in
            let announcement = MonarchUsernameAvailabilityInputTokens.announcement(
                status: outcome.status,
                message: outcome.message,
                labels: labels
            )
            if let announcement {
                AccessibilityNotification.Announcement(announcement).post()
            }
            onStatusChange?(outcome.status, outcome.message)
        }
    }
}

#if DEBUG
    #Preview("MonarchUsernameAvailabilityInput") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchUsernameAvailabilityStatus.allCases, id: \.self) { status in
                    MonarchUsernameAvailabilityField(
                        label: "Handle",
                        text: .constant("ada"),
                        prompt: nil,
                        helper: "Letters, numbers and dashes.",
                        prefix: "rook.com/",
                        status: status,
                        statusText: MonarchUsernameAvailabilityInputTokens.text(status: status, message: nil),
                        retryLabel: MonarchUsernameAvailabilityInputStrings.retry
                    ) {}
                }
            }
        }
    }
#endif
