import Foundation

/// Limits the matrix and example tests to some components, for the fast
/// per-component loop. `Scripts/verify.sh --component <Name>` sets
/// `MONARCH_COMPONENT` to a comma-separated list of inventory ids. Without
/// it every component is tested.
enum ComponentFilter {
    /// The ids to test, or `nil` for all.
    static let ids: Set<String>? = {
        guard let value = ProcessInfo.processInfo.environment["MONARCH_COMPONENT"], !value.isEmpty else {
            return nil
        }
        return Set(value.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) })
    }()

    /// Whether a component is part of this run.
    static func includes(_ id: String) -> Bool {
        ids?.contains(id) ?? true
    }
}
