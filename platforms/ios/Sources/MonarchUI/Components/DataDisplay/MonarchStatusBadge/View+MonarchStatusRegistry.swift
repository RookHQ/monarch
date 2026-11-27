import SwiftUI

/// The statuses installed with `monarchStatusRegistry(_:)`. Empty by default.
private struct MonarchStatusRegistryKey: EnvironmentKey {
    static let defaultValue = MonarchStatusRegistry([:])
}

extension EnvironmentValues {
    /// The statuses installed with `monarchStatusRegistry(_:)`.
    var monarchStatusRegistry: MonarchStatusRegistry {
        get { self[MonarchStatusRegistryKey.self] }
        set { self[MonarchStatusRegistryKey.self] = newValue }
    }
}

extension View {
    /// Makes the app's statuses available to every ``MonarchStatusBadge``
    /// in this view. Nested calls add up; the innermost wins where two have
    /// a key.
    ///
    /// ```swift
    /// TicketList()
    ///     .monarchStatusRegistry(ticketStatuses)
    /// ```
    ///
    /// - Parameter registry: The statuses to add.
    /// - Returns: The view with the statuses in its environment.
    public func monarchStatusRegistry(_ registry: MonarchStatusRegistry) -> some View {
        transformEnvironment(\.monarchStatusRegistry) { current in
            current = current.merging(registry)
        }
    }
}
