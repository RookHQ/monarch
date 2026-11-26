/// What a tool did, for ``MonarchAiToolResultCard``. Picks the icon and the
/// small label above the title.
public enum MonarchAiToolResultKind: Sendable, Hashable, CaseIterable {
    /// A record was created.
    case recordCreated
    /// An email was sent.
    case emailSent
    /// An event was booked.
    case eventBooked
    /// A ticket was filed.
    case ticketFiled
    /// Anything else the tool did.
    case generic
}
