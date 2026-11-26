import SwiftUI

private struct MonarchHidesDayDividersKey: EnvironmentKey {
    static let defaultValue = false
}

extension EnvironmentValues {
    /// Whether a message list leaves out its day separators, as the web thread panel does.
    var monarchHidesDayDividers: Bool {
        get { self[MonarchHidesDayDividersKey.self] }
        set { self[MonarchHidesDayDividersKey.self] = newValue }
    }
}
