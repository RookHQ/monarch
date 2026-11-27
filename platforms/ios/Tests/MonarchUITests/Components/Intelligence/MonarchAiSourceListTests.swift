import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private enum SourceListSamples {
    static let notes = MonarchAiSourcePart(
        id: "s1",
        title: "Quarterly planning notes",
        url: "https://www.example.com/notes/q3",
        kind: .doc,
        product: "Docs"
    )
    static let record = MonarchAiSourcePart(id: "s2", title: "Acme Ltd", kind: .record, product: "CRM")
    static let file = MonarchAiSourcePart(id: "s3", title: "Old forecast.xlsx", kind: .file)
    static let web = MonarchAiSourcePart(
        id: "s4",
        title: "Industry report with a very long title that wraps onto several lines at large text sizes",
        url: "https://example.org/reports/2024",
        kind: .web,
        favicon: "https://example.org/favicon.ico"
    )
    static let unsafe = MonarchAiSourcePart(id: "s5", title: "Script link", url: "javascript:alert(1)", kind: .web)

    static let mixed: [MonarchAiSourceListItem] = [
        MonarchAiSourceListItem(notes, snippet: "Revenue grew 12% in the third quarter."),
        MonarchAiSourceListItem(record),
        MonarchAiSourceListItem(file, isUsed: false),
        MonarchAiSourceListItem(web, isUsed: false, snippet: "Market size estimates for the region."),
        MonarchAiSourceListItem(unsafe),
    ]
}

enum MonarchAiSourceListFixtures: ComponentFixtures {
    static let id = "ai-source-list"
    static let fixtures = [
        Fixture("used only, links") {
            MonarchAiSourceList([
                MonarchAiSourceListItem(SourceListSamples.notes, snippet: "Revenue grew 12% in the third quarter."),
                MonarchAiSourceListItem(SourceListSamples.web),
            ])
        },
        Fixture("grouped, collapsed, buttons") {
            MonarchAiSourceList(SourceListSamples.mixed, onOpen: { _ in })
        },
        Fixture("grouped, expanded") {
            MonarchAiSourceList(SourceListSamples.mixed, isInitiallyExpanded: true)
        },
        Fixture("not numbered, never collapses, custom labels") {
            MonarchAiSourceList(
                SourceListSamples.mixed,
                collapseAfter: nil,
                isNumbered: false,
                labels: MonarchAiSourceListLabels(
                    heading: "Where this came from",
                    used: { "Relied on (\($0))" },
                    considered: { "Looked at (\($0))" }
                )
            )
        },
        Fixture("plain text sources") {
            MonarchAiSourceList([
                MonarchAiSourceListItem(SourceListSamples.record),
                MonarchAiSourceListItem(SourceListSamples.file),
            ])
        },
        Fixture("empty") {
            MonarchAiSourceList([])
        },
    ]
}

@Suite("MonarchAiSourceList")
struct MonarchAiSourceListTests {
    @Test("Sources are numbered in order and split into used and considered")
    func grouping() {
        let layout = MonarchAiSourceListTokens.layout(SourceListSamples.mixed, collapseAfter: nil, isExpanded: false)
        #expect(layout.used.map(\.number) == [1, 2, 5])
        #expect(layout.considered.map(\.number) == [3, 4])
        #expect(layout.usedCount == 3)
        #expect(layout.consideredCount == 2)
        #expect(layout.isGrouped)
        #expect(!layout.isCollapsible)
    }

    @Test("A collapsed list shows the first used sources, then considered ones, up to the limit")
    func collapsing() {
        let collapsed = MonarchAiSourceListTokens.layout(SourceListSamples.mixed, collapseAfter: 3, isExpanded: false)
        #expect(collapsed.isCollapsible)
        #expect(collapsed.used.map(\.number) == [1, 2, 5])
        #expect(collapsed.considered.isEmpty)
        // The counts in the group headings are the totals, not what is on show.
        #expect(collapsed.consideredCount == 2)

        let four = MonarchAiSourceListTokens.layout(SourceListSamples.mixed, collapseAfter: 4, isExpanded: false)
        #expect(four.considered.map(\.number) == [3])

        let expanded = MonarchAiSourceListTokens.layout(SourceListSamples.mixed, collapseAfter: 3, isExpanded: true)
        #expect(expanded.used.count + expanded.considered.count == 5)
        #expect(expanded.isCollapsible)
    }

    @Test("A list at or under the limit does not collapse, and without considered sources is not grouped")
    func noCollapse() {
        let items = Array(SourceListSamples.mixed.prefix(2))
        let layout = MonarchAiSourceListTokens.layout(items, collapseAfter: 3, isExpanded: false)
        #expect(!layout.isCollapsible)
        #expect(!layout.isGrouped)
        #expect(layout.used.count == 2)

        let zero = MonarchAiSourceListTokens.layout(items, collapseAfter: 0, isExpanded: false)
        #expect(zero.isCollapsible)
        #expect(zero.used.isEmpty)

        let empty = MonarchAiSourceListTokens.layout([], collapseAfter: 3, isExpanded: false)
        #expect(empty.used.isEmpty && !empty.isCollapsible && !empty.isGrouped)
    }

    @Test("Only http, https and mailto addresses are opened, and the host drops www")
    func addresses() {
        #expect(
            MonarchAiSourceFormat.safeURL("https://www.example.com/a")?.absoluteString
                == "https://www.example.com/a"
        )
        #expect(MonarchAiSourceFormat.safeURL("mailto:ana@example.com") != nil)
        #expect(MonarchAiSourceFormat.safeURL("javascript:alert(1)") == nil)
        #expect(MonarchAiSourceFormat.safeURL("/relative/path") == nil)
        #expect(MonarchAiSourceFormat.safeURL("https://") == nil)
        #expect(MonarchAiSourceFormat.safeURL(nil) == nil)
        #expect(MonarchAiSourceFormat.host("https://www.Example.com/notes") == "example.com")
        #expect(MonarchAiSourceFormat.host("https://docs.example.com") == "docs.example.com")
        #expect(MonarchAiSourceFormat.host("mailto:ana@example.com") == nil)
    }

    @Test("The meta line joins the host, the product and the kind")
    func meta() {
        #expect(
            MonarchAiSourceListTokens.meta(SourceListSamples.notes, kindName: "Document")
                == "example.com · Docs · Document"
        )
        #expect(MonarchAiSourceListTokens.meta(SourceListSamples.record, kindName: "Record") == "CRM · Record")
        #expect(MonarchAiSourceListTokens.meta(SourceListSamples.unsafe, kindName: "Web page") == "Web page")
    }

    @Test("VoiceOver reads the number and title, then the meta line and the snippet")
    func accessibilityLabel() {
        let label = MonarchAiSourceListTokens.accessibilityLabel(
            number: 1,
            title: "Notes",
            meta: "example.com · Document",
            snippet: "Revenue grew.",
            sourceLabel: MonarchAiSourceListStrings.source
        )
        #expect(label == "Source 1: Notes, example.com · Document, Revenue grew.")
        let plain = MonarchAiSourceListTokens.accessibilityLabel(
            number: nil,
            title: "Notes",
            meta: "",
            snippet: nil,
            sourceLabel: MonarchAiSourceListStrings.source
        )
        #expect(plain == "Notes")
    }

    @Test("The sources of a message are taken in order, each once")
    func sourcesOfMessage() {
        let message = MonarchAiConversationMessage(
            id: "m1",
            role: .assistant,
            parts: [
                .text(MonarchAiTextPart(text: "Revenue grew.")),
                .source(SourceListSamples.notes),
                .source(SourceListSamples.record),
                .source(SourceListSamples.notes),
            ],
            createdAt: "2024-10-14T10:00:00Z"
        )
        let items = MonarchAiSourceListItem.sources(of: message)
        #expect(items.map(\.id) == ["s1", "s2"])
        #expect(items.allSatisfy { $0.isUsed })
    }

    @Test("Every kind has its own icon, and actionable titles use the link colour")
    func kindsAndColours() {
        let symbols = Set(MonarchAiSourceKind.allCases.map(MonarchAiSourceFormat.symbol))
        #expect(symbols.count == MonarchAiSourceKind.allCases.count)
        #expect(MonarchAiSourceListTokens.titleColor(isActionable: true) == \.textDefault)
        #expect(MonarchAiSourceListTokens.titleColor(isActionable: false) == \.textDefault)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchAiSourceListStrings.heading == "Sources")
        #expect(MonarchAiSourceListStrings.used(3) == "Used (3)")
        #expect(MonarchAiSourceListStrings.considered(2) == "Considered (2)")
        #expect(MonarchAiSourceListStrings.showAll(5) == "Show all 5 sources")
        #expect(MonarchAiSourceListStrings.showFewer == "Show fewer")
        #expect(MonarchAiSourceListStrings.source(2, "Notes") == "Source 2: Notes")
        #expect(!MonarchAiSourceListStrings.opensSource.hasPrefix("monarch."))
        for kind in MonarchAiSourceKind.allCases {
            #expect(!MonarchAiSourceListStrings.kind(kind).hasPrefix("monarch."))
        }
    }
}
