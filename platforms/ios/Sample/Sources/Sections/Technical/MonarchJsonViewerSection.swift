import MonarchUI
import SwiftUI

struct MonarchJsonViewerSection: CataloguePage {
    static let title = "JSON viewer"

    @State private var selection = "Nothing selected yet."

    private let payload: MonarchJsonValue = {
        let text = """
            {
              "id": "evt_1NqZ",
              "type": "invoice.paid",
              "created": 1731571200,
              "livemode": false,
              "data": {
                "amount_paid": 4200,
                "currency": "eur",
                "customer name": "Ada Lovelace",
                "discount": null,
                "lines": [
                  { "sku": "A1", "description": "Monthly plan", "qty": 2 },
                  { "sku": "B7", "description": "Extra seat", "qty": 1 }
                ]
              }
            }
            """
        return (try? MonarchJsonValue(parsing: text)) ?? .null
    }()

    var body: some View {
        MonarchJsonViewer(payload, expandDepth: 2, onSelect: { path, _ in selection = "Selected \(path)" })
        Text(selection).monarchTextStyle(.caption)

        CatalogueCaption("A large array, 25 at a time")
        MonarchJsonViewer(
            .array(
                (1...240).map { number -> MonarchJsonValue in
                    ["n": .number(String(number)), "even": .bool(number.isMultiple(of: 2))]
                }
            ),
            maxChildren: 25,
            rootPath: ""
        )
    }
}
