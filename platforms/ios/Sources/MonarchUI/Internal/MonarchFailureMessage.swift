import Foundation

/// The message shown for an error an app's action threw: the error's own
/// `LocalizedError` description when it has one, otherwise the component's
/// fallback ("Couldn't save."). Shared, so every component treats thrown
/// errors the same way.
enum MonarchFailureMessage {
    /// The message for `error`.
    ///
    /// - Parameters:
    ///   - error: The error the app threw.
    ///   - override: The app's replacement for the fallback, from the component's labels.
    ///   - fallback: The component's built-in fallback.
    /// - Returns: The message to show and announce.
    static func message(for error: any Error, override: String? = nil, fallback: String) -> String {
        if let described = (error as? any LocalizedError)?.errorDescription, !described.isEmpty {
            return described
        }
        if let override, !override.isEmpty { return override }
        return fallback
    }

    /// The spoken form of an error message: "Error: <message>".
    static func spoken(_ message: String) -> String {
        MonarchStrings.string("monarch.internal.error", table: "InternalFormats", message)
    }
}
