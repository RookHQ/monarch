/// Replacements for the built-in strings of ``MonarchSwitchSettingsList`` and
/// ``MonarchSwitchSettingsRow``. A property that is `nil` keeps the value
/// from the list, then the built-in English string.
public struct MonarchSwitchSettingsListLabels: Sendable {
    /// Shown beside the spinner while saving. Built in: "Saving".
    public var saving: String?
    /// Shown and announced after a save. Built in: "Saved".
    public var saved: String?
    /// Shown and announced when a save fails, before the error's own
    /// message. Built in: "Couldn't save."
    public var error: String?
    /// The title of the retry button. Built in: "Retry".
    public var retry: String?
    /// The spoken name of the retry button. Receives the row label. Built
    /// in: "Retry <label>", so several failed rows stay distinct.
    public var retryLabel: (@Sendable (String) -> String)?
    /// Turns a failed save into the message after `error`. Built in: the
    /// `errorDescription` of a `LocalizedError`, otherwise nothing.
    public var errorMessage: (@Sendable (any Error) -> String?)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        saving: String? = nil,
        saved: String? = nil,
        error: String? = nil,
        retry: String? = nil,
        retryLabel: (@Sendable (String) -> String)? = nil,
        errorMessage: (@Sendable (any Error) -> String?)? = nil
    ) {
        self.saving = saving
        self.saved = saved
        self.error = error
        self.retry = retry
        self.retryLabel = retryLabel
        self.errorMessage = errorMessage
    }

    /// These labels, with every `nil` taken from `fallback`.
    func merged(over fallback: MonarchSwitchSettingsListLabels) -> MonarchSwitchSettingsListLabels {
        MonarchSwitchSettingsListLabels(
            saving: saving ?? fallback.saving,
            saved: saved ?? fallback.saved,
            error: error ?? fallback.error,
            retry: retry ?? fallback.retry,
            retryLabel: retryLabel ?? fallback.retryLabel,
            errorMessage: errorMessage ?? fallback.errorMessage
        )
    }
}
