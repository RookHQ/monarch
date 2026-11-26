import MonarchUI
import SwiftUI

/// Native adoption: the standard `Slider`, tinted by the theme.
struct SliderSection: CataloguePage {
    static let title = "Slider (standard Slider)"

    @State private var volume = 40.0
    @State private var seats = 5.0

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        Slider(value: $volume, in: 0...100) {
            Text("Volume")
        } minimumValueLabel: {
            MonarchIcon(systemName: "speaker")
        } maximumValueLabel: {
            MonarchIcon(systemName: "speaker.wave.3")
        }

        CatalogueCaption("Steps, with the value in text")
        Slider(value: $seats, in: 1...20, step: 1) {
            Text("Seats")
        }
        Text("\(Int(seats)) seats")
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors.textMuted)

        CatalogueCaption("Disabled")
        Slider(value: .constant(30), in: 0...100) {
            Text("Volume")
        }
        .disabled(true)
    }
}
