import Foundation
@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchAiClaimSourcesSection: CataloguePage {
    static let title = "AI claim sources"

    @State private var message = "No action yet"

    private let sources = [
        MonarchAiClaimSource(id: "s1", title: "Q3 report", url: "https://example.com/q3", kind: .doc, published: .now),
        MonarchAiClaimSource(id: "s2", title: "Analyst note", url: "https://example.com/note", kind: .web),
    ]

    private let claims = [
        MonarchAiClaim(
            id: "c1",
            text: "Revenue grew 12% in the third quarter.",
            support: .supported,
            evidence: [
                MonarchAiClaimEvidence(
                    id: "e1",
                    sourceID: "s1",
                    quote: "Third-quarter revenue rose 12% year on year.",
                    location: "Page 4",
                    stance: .supports
                )
            ]
        ),
        MonarchAiClaim(
            id: "c2",
            text: "Margins improved in every region.",
            support: .conflicting,
            evidence: [
                MonarchAiClaimEvidence(
                    id: "e2",
                    sourceID: "s1",
                    quote: "Margins rose in Europe and Asia.",
                    stance: .supports
                ),
                MonarchAiClaimEvidence(
                    id: "e3",
                    sourceID: "s2",
                    quote: "North American margins fell.",
                    stance: .contradicts
                ),
            ]
        ),
        MonarchAiClaim(id: "c3", text: "Churn halved.", support: .unsupported),
        MonarchAiClaim(id: "c4", text: "The new plan launches in March.", support: .unchecked),
    ]

    var body: some View {
        CatalogueCaption("Open a claim to see its passages")
        MonarchAiClaimSources(
            claims,
            sources: sources,
            onOpenSource: { source, _ in message = "Opened \(source.title)" },
            onFlag: { message = "Reported \($0)" },
            onVerify: { message = "Checking \($0)" },
            onCopyQuote: { _ in message = "Copied a quote" }
        )
        Text(message).monarchTextStyle(.caption)

        CatalogueCaption("No claims")
        MonarchAiClaimSources([], sources: [])
    }
}
