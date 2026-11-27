import Foundation

/// One app and what the AI agent may do in it, for ``MonarchAiPermissionScope``.
public struct MonarchAiPermissionScopeItem: Sendable, Hashable, Identifiable {
    /// The app's name. Also the row's identity, so keep it unique.
    public var id: String { app }
    /// The app's display name, already localised, such as "Calendar".
    public let app: String
    /// Which part of the app the access covers, such as "Shared projects only".
    public let resources: String?
    /// What the agent may do in the app.
    public var access: MonarchAiAccessLevel
    /// The app's icon. A letter tile is shown while it loads, on failure and
    /// when there is none.
    public let iconURL: URL?

    /// A permission row.
    ///
    /// - Parameters:
    ///   - app: The app's display name, already localised. Unique in the list.
    ///   - resources: Which part of the app the access covers.
    ///   - access: What the agent may do.
    ///   - iconURL: The app's icon.
    public init(_ app: String, resources: String? = nil, access: MonarchAiAccessLevel, iconURL: URL? = nil) {
        self.app = app
        self.resources = resources
        self.access = access
        self.iconURL = iconURL
    }
}
