import Foundation
// GameController is used because it is the only public API that says whether
// a hardware keyboard is attached; SwiftUI and UIKit have none. It is a
// system framework on iOS and on the macOS host.
import GameController

/// Whether a hardware keyboard is attached, for ``MonarchKbd``.
enum MonarchKbdKeyboard {
    /// Whether a hardware keyboard is attached now.
    static var isAttached: Bool { GCKeyboard.coalesced != nil }

    /// Posted when the first hardware keyboard is attached.
    static var didConnect: Notification.Name { .GCKeyboardDidConnect }

    /// Posted when the last hardware keyboard is detached.
    static var didDisconnect: Notification.Name { .GCKeyboardDidDisconnect }
}
