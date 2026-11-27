/// Replacements for the built-in strings of ``MonarchMyStatus``.
///
/// Every property is `nil` by default, which keeps the built-in string.
public struct MonarchMyStatusLabels: Sendable {
    /// What VoiceOver says the row does. Built in: "Opens your status and settings".
    public var hint: String?
    /// Shown when there is no custom status and the person is online or offline. Built in: "Set a status".
    public var setStatus: String?
    /// The name of a presence. Built in: the presence names of ``MonarchPresenceDot``.
    public var presence: (@Sendable (MonarchPresenceStatus) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    ///
    /// - Parameters:
    ///   - hint: What VoiceOver says the row does.
    ///   - setStatus: The invitation shown without a custom status.
    ///   - presence: The name of a presence.
    public init(
        hint: String? = nil,
        setStatus: String? = nil,
        presence: (@Sendable (MonarchPresenceStatus) -> String)? = nil
    ) {
        self.hint = hint
        self.setStatus = setStatus
        self.presence = presence
    }
}
