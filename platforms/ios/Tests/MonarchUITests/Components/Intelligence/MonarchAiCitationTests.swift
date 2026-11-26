import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let report = MonarchAiSourcePart(
    id: "report",
    title: "Quarterly report",
    url: "https://www.example.com/q3",
    kind: .doc,
    product: "Dossier"
)
private let record = MonarchAiSourcePart(id: "deal", title: "Acme renewal", kind: .record, product: "CRM")
private let unsafe = MonarchAiSourcePart(id: "bad", title: "Script", url: "javascript:alert(1)", kind: .web)

enum MonarchAiCitationFixtures: ComponentFixtures {
    static let id = "ai-citation"
    static let fixtures = [
        Fixture("markers in a sentence") {
            MonarchFlowLayout {
                Text("Revenue grew 12% in the third quarter.")
                MonarchAiCitation(1, source: report, quote: "Revenue rose 12%.")
                MonarchAiCitation(12, source: record) { _ in }
            }
        },
        Fixture("card with quote and visit link") {
            MonarchAiCitationCard(
                index: 1,
                source: report,
                quote: "Revenue rose 12% quarter on quarter, driven by renewals in the enterprise segment.",
                labels: MonarchAiCitationLabels(),
                onOpen: nil
            )
        },
        Fixture("card with open button") {
            MonarchAiCitationCard(index: 2, source: record, quote: nil, labels: MonarchAiCitationLabels(), onOpen: {})
        },
        Fixture("card without action") {
            MonarchAiCitationCard(index: 3, source: unsafe, quote: nil, labels: MonarchAiCitationLabels(), onOpen: nil)
        },
    ]
}

@Suite("MonarchAiCitation")
struct MonarchAiCitationTests {
    private let labels = MonarchAiCitationLabels()

    @Test("Sources are numbered in the order they appear")
    func numbers() {
        let parts: [MonarchAiPart] = [
            .text(MonarchAiTextPart(text: "Hi")),
            .source(report),
            .citation(MonarchAiCitationPart(id: "c1", sourceId: "report")),
            .source(record),
            .source(report),
        ]
        #expect(MonarchAiCitation.sourceNumbers(in: parts) == ["report": 1, "deal": 2])
        #expect(MonarchAiCitation.sourceNumbers(in: []).isEmpty)
    }

    @Test("Only http, https and mailto addresses are opened")
    func safeURL() {
        #expect(MonarchAiSourceFormat.safeURL("https://example.com/a")?.absoluteString == "https://example.com/a")
        #expect(MonarchAiSourceFormat.safeURL("mailto:a@example.com") != nil)
        #expect(MonarchAiSourceFormat.safeURL("javascript:alert(1)") == nil)
        #expect(MonarchAiSourceFormat.safeURL("/relative") == nil)
        #expect(MonarchAiSourceFormat.safeURL("https://") == nil)
        #expect(MonarchAiSourceFormat.safeURL(nil) == nil)
        #expect(MonarchAiSourceFormat.faviconURL("mailto:a@example.com") == nil)
    }

    @Test("The meta line joins host, product and kind")
    func meta() {
        #expect(MonarchAiSourceFormat.host("https://www.example.com/q3") == "example.com")
        #expect(MonarchAiCitationTokens.meta(report, labels: labels) == "example.com · Dossier · Document")
        #expect(MonarchAiCitationTokens.meta(record, labels: labels) == "CRM · Record")
        let custom = MonarchAiCitationLabels(kind: { $0 == .record ? "Deal" : nil })
        #expect(MonarchAiCitationTokens.meta(record, labels: custom) == "CRM · Deal")
    }

    @Test("The card opens in the app, visits a safe address, or has no action")
    func action() {
        #expect(MonarchAiCitationTokens.action(report, hasOpenHandler: true) == .open)
        #expect(
            MonarchAiCitationTokens.action(report, hasOpenHandler: false)
                == .visit(URL(string: "https://www.example.com/q3")!)
        )
        #expect(MonarchAiCitationTokens.action(unsafe, hasOpenHandler: false) == nil)
        #expect(MonarchAiCitationTokens.action(record, hasOpenHandler: false) == nil)
    }

    @Test("The marker reads the number and title, not the brackets")
    func marker() {
        #expect(MonarchAiCitationTokens.markerText(3) == "[3]")
        #expect(MonarchAiCitationTokens.markerLabel(1, source: report, labels: labels) == "Source 1: Quarterly report")
        let custom = MonarchAiCitationLabels(marker: { "Quelle \($0): \($1)" })
        #expect(MonarchAiCitationTokens.markerLabel(2, source: record, labels: custom) == "Quelle 2: Acme renewal")
    }

    @Test("Each kind has a symbol and a name")
    func kinds() {
        let symbols = MonarchAiSourceKind.allCases.map(MonarchAiSourceFormat.symbol)
        #expect(Set(symbols).count == MonarchAiSourceKind.allCases.count)
        #expect(
            MonarchAiSourceKind.allCases.map { MonarchAiCitationTokens.kindName($0, labels: labels) } == [
                "Web page", "Document", "Record", "File",
            ]
        )
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchAiCitationStrings.card(4) == "Source 4")
        #expect(MonarchAiCitationStrings.open == "Open source")
        #expect(MonarchAiCitationStrings.visit == "Visit page")
    }
}
