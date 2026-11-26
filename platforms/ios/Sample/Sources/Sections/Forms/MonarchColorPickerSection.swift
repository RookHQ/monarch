import MonarchUI
import SwiftUI

struct MonarchColorPickerSection: CataloguePage {
    static let title = "Colour picker"

    @State private var label: String? = "#2563eb"
    @State private var brand: String?

    var body: some View {
        MonarchColorPicker("Label colour", selection: $label, helper: "Pick a preset or any colour.")
        HStack {
            Circle()
                .fill(label.flatMap(MonarchColorSwatch.color(hex:)) ?? Color.clear)
                .frame(width: 24, height: 24)
            Text(label ?? "none").monarchTextStyle(.code)
        }

        MonarchColorPicker("Presets only", selection: $label, allowsCustom: false)

        MonarchColorPicker(
            "Brand colours",
            selection: $brand,
            swatches: [
                MonarchColorSwatch(hex: "#111827", name: "Ink"),
                MonarchColorSwatch(hex: "#f59e0b", name: "Amber"),
                MonarchColorSwatch(hex: "#ffffff", name: "White"),
            ].compactMap { $0 },
            emptyLabel: "Not set"
        )

        MonarchColorPicker("Disabled", selection: .constant("#16a34a"))
            .disabled(true)
    }
}
