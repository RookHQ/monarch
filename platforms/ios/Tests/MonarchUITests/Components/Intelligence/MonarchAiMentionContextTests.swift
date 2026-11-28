import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiMentionContextFixtures: ComponentFixtures {
    static let id = "ai-mention-context"
    static let query = MonarchMentionQuery.find(in: "Summarise @q3")
    static let items = [
        MonarchAiMentionItem(id: "p1", kind: .person, label: "Ana Lopez", description: "Finance lead"),
        MonarchAiMentionItem(
            id: "d1",
            kind: .doc,
            label: "Q3 report.pdf",
            description: "Finance / Reports",
            tokens: 12_400
        ),
        MonarchAiMentionItem(id: "r1", kind: .record, label: "Acme Corp", description: "Customer", tokens: 900),
        MonarchAiMentionItem(id: "a1", kind: .app, label: "Billing", isDisabled: true),
    ]

    static let fixtures = [
        Fixture("every kind, grouped") {
            MonarchAiMentionContext(query, items: items) { _ in }
        },
        Fixture("one kind, no headings") {
            MonarchAiMentionContext(query, items: Array(items.dropFirst().prefix(1))) { _ in }
        },
        Fixture("many results scroll") {
            MonarchAiMentionContext(
                query,
                items: (1...8).map {
                    MonarchAiMentionItem(id: "d\($0)", kind: .doc, label: "Document \($0)", tokens: $0 * 1_000)
                }
            ) { _ in }
        },
        Fixture("searching") {
            MonarchAiMentionContext(query, items: [], status: .loading) { _ in }
        },
        Fixture("failed with retry") {
            MonarchAiMentionContext(query, items: [], status: .failed, onRetry: {}, onSelect: { _ in })
        },
        Fixture("nothing matches") {
            MonarchAiMentionContext(query, items: []) { _ in }
        },
        Fixture("long label and description") {
            MonarchAiMentionContext(
                query,
                items: [
                    MonarchAiMentionItem(
                        id: "long",
                        kind: .doc,
                        label: "Quarterly financial results and commentary for the board, third quarter.pdf",
                        description: "Finance / Board / 2026 / Quarterly results / Drafts and final versions",
                        tokens: 1_250_000
                    )
                ]
            ) { _ in }
        },
    ]
}

@Suite("MonarchAiMentionContext")
struct MonarchAiMentionContextTests {
    private let labels = MonarchAiMentionContextLabels()
    private let locale = Locale(identifier: "en_US")
    private let items = MonarchAiMentionContextFixtures.items

    @Test("Results are grouped documents, people, records, apps")
    func groups() {
        let groups = MonarchAiMentionContextTokens.groups(items)
        #expect(groups.map(\.kind) == [.doc, .person, .record, .app])
        #expect(groups.map { $0.items.map(\.id) } == [["d1"], ["p1"], ["r1"], ["a1"]])
        #expect(MonarchAiMentionContextTokens.groups([]).isEmpty)
    }

    @Test("Headings show only with more than one kind")
    func headings() {
        #expect(MonarchAiMentionContextTokens.heading(.doc, groupCount: 1, labels: labels) == nil)
        #expect(MonarchAiMentionContextTokens.heading(.doc, groupCount: 2, labels: labels) == "Documents")
        #expect(MonarchAiMentionContextTokens.heading(.app, groupCount: 4, labels: labels) == "Apps")
        let custom = MonarchAiMentionContextLabels(group: { $0 == .record ? "Clientes" : "Otros" })
        #expect(MonarchAiMentionContextTokens.heading(.record, groupCount: 2, labels: custom) == "Clientes")
    }

    @Test("A chosen result becomes a context item; documents become files")
    func contextItem() {
        #expect(
            items[1].contextItem
                == MonarchAiContextItem(
                    id: "d1",
                    kind: .file,
                    label: "Q3 report.pdf",
                    tokens: 12_400,
                    description: "Finance / Reports"
                )
        )
        #expect(MonarchAiMentionKind.allCases.map(\.contextKind) == [.file, .person, .record, .app])
        #expect(MonarchAiMentionContextTokens.symbol(.doc) == MonarchAiContextFormat.symbol(.file))
    }

    @Test("Rows read label, description and tokens")
    func rowLabel() {
        #expect(
            MonarchAiMentionContextTokens.rowLabel(items[1], locale: locale, labels: labels)
                == "Q3 report.pdf, Finance / Reports, 12.4K tokens"
        )
        #expect(MonarchAiMentionContextTokens.rowLabel(items[3], locale: locale, labels: labels) == "Billing")
        #expect(MonarchAiMentionContextTokens.tokensText(1, locale: locale, labels: labels) == "1 token")
    }

    @Test("The empty text depends on the query")
    func empty() {
        #expect(MonarchAiMentionContextTokens.emptyText(query: "", labels: labels) == "Type to search")
        #expect(MonarchAiMentionContextTokens.emptyText(query: "q3", labels: labels) == "Nothing matches “q3”")
    }

    @Test("Each state of the search is announced")
    func announcement() {
        #expect(
            MonarchAiMentionContextTokens.announcement(status: .loading, count: 0, query: "", labels: labels)
                == "Searching…"
        )
        #expect(
            MonarchAiMentionContextTokens.announcement(status: .failed, count: 0, query: "", labels: labels)
                == "Couldn’t load results."
        )
        #expect(
            MonarchAiMentionContextTokens.announcement(status: .ready, count: 1, query: "q", labels: labels)
                == "1 result"
        )
        #expect(
            MonarchAiMentionContextTokens.announcement(status: .ready, count: 4, query: "q", labels: labels)
                == "4 results"
        )
        #expect(
            MonarchAiMentionContextTokens.announcement(status: .ready, count: 0, query: "q", labels: labels)
                == "Nothing matches “q”"
        )
    }

    @Test("The menu opens with a query and closes after a spaced query found nothing")
    func open() {
        let query = MonarchMentionQuery.find(in: "@q3")
        let spaced = MonarchMentionQuery.find(in: "@q3 report")
        #expect(!MonarchAiMentionContextTokens.isOpen(query: nil, status: .ready, itemCount: 3))
        #expect(MonarchAiMentionContextTokens.isOpen(query: query, status: .ready, itemCount: 0))
        #expect(!MonarchAiMentionContextTokens.isOpen(query: spaced, status: .ready, itemCount: 0))
        #expect(MonarchAiMentionContextTokens.isOpen(query: spaced, status: .loading, itemCount: 0))
        #expect(MonarchAiMentionContextTokens.scrolls(itemCount: 8))
        #expect(!MonarchAiMentionContextTokens.scrolls(itemCount: 7))
    }

    @Test("Disabled rows use the disabled text colour")
    func colours() {
        #expect(MonarchAiMentionContextTokens.textColors(isDisabled: false).label == \MonarchColorTokens.textDefault)
        #expect(MonarchAiMentionContextTokens.textColors(isDisabled: false).detail == \MonarchColorTokens.textMuted)
        #expect(MonarchAiMentionContextTokens.textColors(isDisabled: true).label == \MonarchColorTokens.textDisabled)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiMentionContextStrings.label == "Add context")
        #expect(MonarchAiMentionContextStrings.group(.doc) == "Documents")
        #expect(MonarchAiMentionContextStrings.group(.person) == "People")
        #expect(MonarchAiMentionContextStrings.group(.record) == "Records")
        #expect(MonarchAiMentionContextStrings.typeToSearch == "Type to search")
    }
}
