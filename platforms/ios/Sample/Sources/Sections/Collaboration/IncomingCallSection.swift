import CallKit
import MonarchUI
import SwiftUI

/// Native adoption: CallKit shows the system's incoming-call screen.
struct IncomingCallSection: CataloguePage {
    static let title = "Incoming call (CallKit)"

    @State private var provider: CXProvider?
    @State private var result = ""

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Button("Simulate an incoming call", systemImage: "phone.arrow.down.left") {
            let update = CXCallUpdate()
            update.remoteHandle = CXHandle(type: .generic, value: "Ana Flores")
            update.localizedCallerName = "Ana Flores"
            update.hasVideo = false
            // A product keeps one provider for its lifetime and sets a delegate
            // that answers and ends the call.
            let active = provider ?? CXProvider(configuration: CXProviderConfiguration())
            provider = active
            active.reportNewIncomingCall(with: UUID(), update: update) { _ in }
            result = "The call was reported to CallKit. Decline it on the system screen."
        }
        .buttonStyle(.monarch(.primary))
        if !result.isEmpty {
            Text(result).monarchTextStyle(.caption).foregroundStyle(theme.colors.textMuted)
        }
        Text(
            "The system draws the ringing screen, on the lock screen too. This page sets no delegate, so "
                + "answering does not connect anything. CallKit is not available in every region."
        )
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)
    }
}
