/// How serious a security alert is. Sets the icon, the badge and its tone.
public enum MonarchSecurityAlertSeverity: String, Sendable, Hashable, Codable, CaseIterable {
    /// For information, such as a new sign-in the person probably made.
    case info
    /// Worth a look.
    case low
    /// Needs attention.
    case medium
    /// Needs action.
    case high
    /// Needs action now, such as a blocked attack.
    case critical
}
