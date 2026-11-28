import MonarchUI
import SwiftUI

struct MonarchAiTonePickerSection: CataloguePage {
    static let title = "AI tone picker"

    @State private var tone = "professional"
    @State private var customTone = "professional"
    @State private var customText = ""
    @State private var compactTone = "casual"

    var body: some View {
        CatalogueCaption("Rows with descriptions")
        MonarchAiTonePicker(selection: $tone)

        CatalogueCaption("With a custom tone")
        MonarchAiTonePicker(selection: $customTone, customText: $customText)

        CatalogueCaption("Compact, a menu picker")
        MonarchAiTonePicker(selection: $compactTone, customText: $customText, variant: .compact)

        CatalogueCaption("Disabled")
        MonarchAiTonePicker(selection: .constant("technical"))
            .disabled(true)
    }
}
