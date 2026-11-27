import MonarchUI
import SwiftUI

struct MonarchAiSourcePreviewSection: CataloguePage {
    static let title = "AI source preview"

    private enum Demo: String, Identifiable, CaseIterable {
        case found = "Quote in the content"
        case missing = "Quote not in the content"
        case quoteOnly = "Quote only"
        case empty = "Nothing to preview"
        var id: String { rawValue }
    }

    @State private var demo: Demo?

    private let source = MonarchAiSourcePart(
        id: "s1",
        title: "Quarterly planning notes",
        url: "https://www.example.com/notes/q3",
        kind: .doc,
        product: "Docs"
    )

    private let content = """
        Planning for the third quarter starts with the numbers.

        Hiring stays flat until the new year, and travel is limited to customer visits.

        Revenue grew 12% against the same quarter last year, led by the enterprise plan.

        The board meets again in January.
        """

    var body: some View {
        Text("Each button opens the preview in a sheet.")
            .monarchTextStyle(.body)
        ForEach(Demo.allCases) { item in
            MonarchButton(item.rawValue) { demo = item }
        }
        .sheet(item: $demo) { item in
            switch item {
            case .found:
                MonarchAiSourcePreview(source, quote: "revenue grew 12% against the same quarter", content: content)
            case .missing:
                MonarchAiSourcePreview(source, quote: "Margins improved by two points.", content: content)
            case .quoteOnly:
                MonarchAiSourcePreview(
                    MonarchAiSourcePart(id: "s2", title: "Acme Ltd", kind: .record, product: "CRM"),
                    quote: "Renewal due in March."
                )
            case .empty:
                MonarchAiSourcePreview(MonarchAiSourcePart(id: "s3", title: "Old forecast.xlsx", kind: .file))
            }
        }
    }
}
