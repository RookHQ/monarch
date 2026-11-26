import AVKit
import SwiftUI

/// One video of a lightbox, in the system player. The player is created when
/// the page appears and paused when the person moves to another item.
struct MonarchLightboxVideoPage: View {
    let item: MonarchLightboxItem
    let isCurrent: Bool
    let labels: MonarchLightboxLabels

    @State private var player: AVPlayer?

    var body: some View {
        if let url = item.url {
            VideoPlayer(player: player)
                .clipShape(MonarchLightboxTokens.mediaShape)
                .accessibilityLabel(item.alt)
                .onAppear {
                    if player == nil {
                        player = AVPlayer(url: url)
                    }
                }
                .onDisappear { player?.pause() }
                .onChange(of: isCurrent) { _, isCurrent in
                    if !isCurrent { player?.pause() }
                }
        } else {
            MonarchLightboxNotice(text: labels.unavailable ?? MonarchLightboxStrings.unavailable)
        }
    }
}
