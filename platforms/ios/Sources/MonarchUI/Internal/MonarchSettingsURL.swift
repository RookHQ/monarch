import Foundation

#if os(iOS)
    // Only for `UIApplication.openSettingsURLString`, the URL of the app's
    // page in Settings. SwiftUI has no equivalent.
    import UIKit
#endif

/// The URL of the app's own page in Settings, where the person turns a
/// permission (microphone, camera, notifications) back on. Open it with the
/// `openURL` environment action. `nil` on the macOS build host.
enum MonarchSettingsURL {
    @MainActor
    static var appSettings: URL? {
        #if os(iOS)
            URL(string: UIApplication.openSettingsURLString)
        #else
            nil
        #endif
    }
}
