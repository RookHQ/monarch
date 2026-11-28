/// How far a surface is raised above the canvas.
public enum MonarchElevationLevel: Sendable, Hashable, CaseIterable {
    /// Flat. No shadow.
    case none
    /// The smallest shadow.
    case xs
    /// Resting cards.
    case sm
    /// Raised surfaces.
    case md
    /// Floating surfaces.
    case lg
    /// The largest shadow.
    case xl

    /// The one level for presented surfaces: menus, popovers, sheets,
    /// dialogs and toasts. It is the soft, low `md` level. Buttons, fields,
    /// chips, list rows and inline cards stay flat: a hairline border and the
    /// surface colour separate them. In dark appearance the shadow barely
    /// shows, so surfaces are told apart by their surface step (`bgRaised`).
    public static let presented: MonarchElevationLevel = .md
}
