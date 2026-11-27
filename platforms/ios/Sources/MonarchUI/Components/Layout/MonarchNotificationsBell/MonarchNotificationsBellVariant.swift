/// The shape of the notifications button.
public enum MonarchNotificationsBellVariant: Sendable, Hashable, CaseIterable {
    /// A square icon button with the count on the bell, for a toolbar or a header.
    case icon
    /// A full-width row with the title and the count, for a menu, a sidebar or a list.
    case row
}
