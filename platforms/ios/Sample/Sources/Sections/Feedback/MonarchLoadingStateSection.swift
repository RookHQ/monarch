import MonarchUI
import SwiftUI

struct MonarchLoadingStateSection: CataloguePage {
    static let title = "Loading state"

    @State private var variant = MonarchLoadingStateVariant.list

    var body: some View {
        Picker("Variant", selection: $variant) {
            ForEach(MonarchLoadingStateVariant.allCases, id: \.self) { variant in
                Text(String(describing: variant)).tag(variant)
            }
        }
        .pickerStyle(.segmented)
        MonarchLoadingState("Loading tickets", variant: variant, rows: 3)
            .id(variant)
    }
}
