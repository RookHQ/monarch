import MonarchUI
import SwiftUI

struct MonarchAudioLevelMeterSection: CataloguePage {
    static let title = "AudioLevelMeter"

    @Environment(\.monarchTheme) private var theme
    @State private var level = 0.5
    @State private var isMuted = false

    var body: some View {
        CatalogueCaption("Segments, both sizes")
        HStack(spacing: theme.spacing.space4) {
            MonarchAudioLevelMeter(level, isMuted: isMuted)
            MonarchAudioLevelMeter(level, size: .sm, isMuted: isMuted)
            MonarchAudioLevelMeter(level, segments: 9, isMuted: isMuted)
        }

        CatalogueCaption("Bar")
        MonarchAudioLevelMeter(level, variant: .bar, isMuted: isMuted)

        CatalogueCaption("The level is supplied by the app. Here, a slider.")
        Slider(value: $level, in: 0...1) {
            Text("Level")
        }
        Toggle("Muted", isOn: $isMuted)
            .monarchTextStyle(.body)
    }
}
