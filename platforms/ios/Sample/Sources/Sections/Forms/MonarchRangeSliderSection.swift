import MonarchUI
import SwiftUI

struct MonarchRangeSliderSection: CataloguePage {
    static let title = "Range slider"

    @State private var price: ClosedRange<Double> = 20...150
    @State private var hours: ClosedRange<Double> = 9...17
    @State private var saved: ClosedRange<Double> = 9...17

    var body: some View {
        CatalogueCaption("Price filter inside a form field")
        MonarchFormField("Price") {
            MonarchRangeSlider(
                "Price",
                value: $price,
                in: 0...200,
                step: 5,
                valueText: { $0.formatted(.currency(code: "EUR").precision(.fractionLength(0))) },
                showsValue: true
            )
        }

        CatalogueCaption("Working hours, at least 2 hours apart, small")
        MonarchRangeSlider(
            "Working hours",
            value: $hours,
            in: 0...24,
            minimumStepsBetween: 2,
            size: .sm,
            showsValue: true,
            onEditingChanged: { isEditing in
                if !isEditing { saved = hours }
            }
        )
        CatalogueCaption("Saved when the drag ends: \(Int(saved.lowerBound))–\(Int(saved.upperBound))")

        CatalogueCaption("Invalid and disabled")
        MonarchRangeSlider("Budget", value: .constant(10...90), isInvalid: true)
        MonarchRangeSlider("Price", value: .constant(10...40)).disabled(true)
    }
}
