/// The pure decisions of ``MonarchAutosaveIndicator``.
enum MonarchAutosaveIndicatorTokens {
    /// The message shown for an error: the app's text for the error, or
    /// `nil` for the indicator's built-in "Couldn't save".
    static func errorMessage(
        status: MonarchSaveStatus,
        error: (any Error)?,
        transform: ((any Error) -> String)?
    ) -> String? {
        guard status == .error, let error, let transform else { return nil }
        let message = transform(error)
        return message.isEmpty ? nil : message
    }
}
