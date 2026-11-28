import MonarchUI
import SwiftUI

struct MonarchAiCitationSection: CataloguePage {
    static let title = "AI citation"

    private let parts: [MonarchAiPart] = [
        .source(
            MonarchAiSourcePart(
                id: "report",
                title: "Quarterly report",
                url: "https://www.example.com/q3",
                kind: .doc,
                product: "Dossier"
            )
        ),
        .source(MonarchAiSourcePart(id: "deal", title: "Acme renewal", kind: .record, product: "CRM")),
    ]

    @State private var opened = "Nothing opened"

    var body: some View {
        let numbers = MonarchAiCitation.sourceNumbers(in: parts)
        CatalogueCaption("Tap a marker")
        MonarchFlowLayout {
            Text("Revenue grew 12% in the third quarter")
            ForEach(Array(parts.enumerated()), id: \.offset) { _, part in
                if case .source(let source) = part {
                    if source.kind == .record {
                        MonarchAiCitation(numbers[source.id] ?? 1, source: source) { opened in
                            self.opened = "Opened \(opened.title)"
                        }
                    } else {
                        MonarchAiCitation(
                            numbers[source.id] ?? 1,
                            source: source,
                            quote: "Revenue rose 12% quarter on quarter."
                        )
                    }
                }
            }
        }
        Text(opened).monarchTextStyle(.caption)
    }
}
