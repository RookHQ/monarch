import MonarchUI
import SwiftUI

struct MonarchRecordingIndicatorSection: CataloguePage {
    static let title = "Recording indicator"

    @State private var isRecording = true
    @State private var isPaused = false
    @State private var startedAt = Date.now

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        MonarchRecordingIndicator(isRecording: isRecording, since: startedAt, isPaused: isPaused) {
            isRecording = false
        }
        Toggle("Recording", isOn: $isRecording)
            .onChange(of: isRecording) { _, isOn in
                if isOn { startedAt = .now }
            }
        Toggle("Paused", isOn: $isPaused)

        CatalogueCaption("Time counted by the app")
        MonarchRecordingIndicator(isRecording: true, elapsed: 3723)
        CatalogueCaption("Without a time")
        MonarchRecordingIndicator(isRecording: true)
        CatalogueCaption("Solid, for placing over video")
        MonarchRecordingIndicator(isRecording: true, elapsed: 252, variant: .solid)
            .padding(theme.spacing.space4)
            .frame(maxWidth: .infinity, alignment: .topLeading)
            .background(theme.colors.bgEmphasis)
    }
}
