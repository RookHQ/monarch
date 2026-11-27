// AVKit's `VideoPlayer` is SwiftUI's own video view; it plays the uploaded video.
import AVKit
import SwiftUI

/// The preview of the uploaded file: the image, or a poster of the video
/// that opens the system player.
struct MonarchMediaUploadPreview: View {
    let url: URL
    let kind: MonarchMediaUploadKind
    let label: String
    let labels: MonarchMediaUploadLabels

    @Environment(\.monarchTheme) private var theme
    @State private var poster: Image?
    @State private var isPlaying = false

    var body: some View {
        switch kind {
        case .image:
            Color.clear
                .aspectRatio(MonarchMediaUploadTokens.previewAspectRatio, contentMode: .fit)
                .overlay {
                    MonarchRemoteImage(url: url) {
                        MonarchIcon(systemName: MonarchMediaUploadTokens.placeholderSymbol, size: .lg)
                            .foregroundStyle(theme.colors[role: MonarchMediaUploadTokens.secondary])
                    }
                }
                .background(theme.colors[role: MonarchMediaUploadTokens.previewBackground])
                .clipShape(MonarchShapes.container)
                .overlay(
                    MonarchShapes.container.strokeBorder(
                        theme.colors[role: MonarchMediaUploadTokens.previewBorder],
                        lineWidth: MonarchShapes.borderWidth
                    )
                )
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityAddTraits(.isImage)
        case .video:
            MonarchVideoThumbnail(label, image: poster) { isPlaying = true }
                .task(id: url) { poster = try? await MonarchVideoThumbnail.frame(of: url) }
                .sheet(isPresented: $isPlaying) {
                    NavigationStack {
                        VideoPlayer(player: AVPlayer(url: url))
                            .toolbar {
                                ToolbarItem(placement: .cancellationAction) {
                                    Button(labels.close ?? MonarchMediaUploadStrings.close) { isPlaying = false }
                                }
                            }
                    }
                }
        }
    }
}
