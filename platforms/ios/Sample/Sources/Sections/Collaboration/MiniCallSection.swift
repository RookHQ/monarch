import AVKit
import MonarchUI
import SwiftUI

/// Native adoption: system picture in picture keeps a call visible while
/// people use the rest of the app or another app.
struct MiniCallSection: CataloguePage {
    static let title = "Mini call (system picture in picture)"

    @State private var controller: AVPictureInPictureController?
    @State private var result = ""

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        let isSupported = AVPictureInPictureController.isPictureInPictureSupported()
        MonarchBadge(
            isSupported ? "Picture in picture is supported here" : "Picture in picture is not supported here",
            tone: isSupported ? .success : .neutral
        )
        Button("Continue in picture in picture", systemImage: "pip.enter") {
            // A product passes the layer that shows its call.
            let layer = AVPlayerLayer(player: AVPlayer())
            controller = AVPictureInPictureController(playerLayer: layer)
            if controller?.isPictureInPicturePossible == true {
                controller?.startPictureInPicture()
                result = "Picture in picture was asked to start."
            } else {
                result = "The system does not allow picture in picture now: nothing is playing in this page."
            }
        }
        .buttonStyle(.monarch(.secondary))
        .disabled(!isSupported)
        if !result.isEmpty {
            Text(result).monarchTextStyle(.caption).foregroundStyle(theme.colors.textMuted)
        }
        Text(
            "The catalogue has no call and does not declare the Audio, AirPlay, and Picture in Picture "
                + "background mode, so the window does not open here. In a product the system draws the window, "
                + "its controls and its drag behaviour."
        )
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors.textMuted)
    }
}
