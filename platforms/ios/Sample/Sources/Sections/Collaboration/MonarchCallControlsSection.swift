import MonarchUI
import SwiftUI

struct MonarchCallControlsSection: CataloguePage {
    static let title = "CallControls"

    @Environment(\.monarchTheme) private var theme
    @State private var isMicOn = true
    @State private var isCameraOn = false
    @State private var isSharing = false
    @State private var isHandRaised = false
    @State private var layout = MonarchCallControlsLayout.automatic
    @State private var isLowData = false
    @State private var last = "Nothing yet"

    var body: some View {
        Text(last)
            .monarchTextStyle(.body)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Every control")
        MonarchCallControls(
            isMicOn: $isMicOn,
            isCameraOn: $isCameraOn,
            isSharing: $isSharing,
            isHandRaised: $isHandRaised,
            layout: $layout,
            isLowData: $isLowData,
            onReact: { emoji in last = "Reacted \(emoji)" },
            onOpenDevices: { last = "Devices" },
            onOpenStats: { last = "Connection" },
            onPictureInPicture: { last = "Picture in picture" },
            onLeave: { last = "Left the call" }
        )

        CatalogueCaption("Minimal, for a small window")
        MonarchCallControls(
            isMicOn: $isMicOn,
            isCameraOn: $isCameraOn,
            isMinimal: true,
            onLeave: { last = "Left the call" }
        )
    }
}
