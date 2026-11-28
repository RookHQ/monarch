import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let items = [
    MonarchLinkedDocsPanelItem(
        id: "1",
        title: "Launch plan",
        docID: "d1",
        meta: "Updated 2 hours ago",
        status: "Draft",
        spaceID: "mkt",
        spaceName: "Marketing",
        fileName: "launch.pdf"
    ),
    MonarchLinkedDocsPanelItem(id: "2", title: "Brand guide", docID: "d2", spaceID: "design", spaceName: "Design"),
    MonarchLinkedDocsPanelItem(
        id: "3",
        title: "Runbook for the payments service, with a title long enough to wrap",
        kind: .link,
        url: URL(string: "https://www.example.com/runbook")
    ),
    MonarchLinkedDocsPanelItem(
        id: "4",
        title: "Pinned spec",
        docID: "d4",
        spaceID: "mkt",
        spaceName: "Marketing",
        isUnlinkable: false
    ),
]

private let options = [
    MonarchLinkedDocsPanelOption(docID: "d1", label: "Launch plan"),
    MonarchLinkedDocsPanelOption(docID: "d9", label: "Pricing notes", description: "Finance"),
    MonarchLinkedDocsPanelOption(docID: "d10", label: "Résumé template", description: "People"),
]

enum MonarchLinkedDocsPanelFixtures: ComponentFixtures {
    static let id = "linked-docs-panel"
    static let fixtures = [
        Fixture("docs and links, editable, space filter") {
            MonarchLinkedDocsPanel(
                items,
                docOptions: options,
                onLinkDoc: { _ in },
                onAddLink: { _, _ in },
                onUnlink: { _ in },
                onOpen: { _ in }
            )
        },
        Fixture("read only, no filter") {
            MonarchLinkedDocsPanel(items, isReadOnly: true, isSpaceFilterShown: false, onOpen: { _ in })
        },
        Fixture("loading") {
            MonarchLinkedDocsPanel(nil, onOpen: { _ in })
        },
        Fixture("empty, editable and read only") {
            VStack {
                MonarchLinkedDocsPanel([], onAddLink: { _, _ in }, onOpen: { _ in })
                MonarchLinkedDocsPanel([], isReadOnly: true, onOpen: { _ in })
            }
        },
        Fixture("error with retry") {
            MonarchLinkedDocsPanel(
                Array(items.prefix(1)),
                errorMessage: "Couldn’t load the linked docs.",
                isRetrying: true,
                onRetry: {},
                onOpen: { _ in }
            )
        },
        Fixture("notice") {
            MonarchLinkedDocsPanel(items, notice: "You don’t have access to docs in this workspace.", onOpen: { _ in })
        },
        Fixture("picker") {
            MonarchLinkedDocsPanelPicker(
                options: options,
                items: items,
                labels: MonarchLinkedDocsPanelLabels(),
                onQueryChange: nil,
                onCreateDoc: {},
                onSelect: { _ in }
            )
        },
        Fixture("link form") {
            MonarchLinkedDocsPanelLinkForm(labels: MonarchLinkedDocsPanelLabels()) { _, _ in }
        },
    ]
}

@Suite("MonarchLinkedDocsPanel")
struct MonarchLinkedDocsPanelTests {
    private let locale = Locale(identifier: "en_US")

    @Test("A notice wins, nil items are loading, and no items is empty")
    func phase() {
        #expect(MonarchLinkedDocsPanelTokens.phase(items: items, notice: "No access") == .notice)
        #expect(MonarchLinkedDocsPanelTokens.phase(items: nil, notice: nil) == .loading)
        #expect(MonarchLinkedDocsPanelTokens.phase(items: [], notice: "") == .empty)
        #expect(MonarchLinkedDocsPanelTokens.phase(items: items, notice: nil) == .list)
    }

    @Test("Only full http and https addresses are accepted")
    func parse() {
        #expect(
            MonarchLinkedDocsPanelTokens.parseLinkURL("  https://example.com/a ")?.absoluteString
                == "https://example.com/a"
        )
        #expect(MonarchLinkedDocsPanelTokens.parseLinkURL("http://intranet/page") != nil)
        #expect(MonarchLinkedDocsPanelTokens.parseLinkURL("example.com") == nil)
        #expect(MonarchLinkedDocsPanelTokens.parseLinkURL("javascript:alert(1)") == nil)
        #expect(MonarchLinkedDocsPanelTokens.parseLinkURL("ftp://example.com") == nil)
        #expect(MonarchLinkedDocsPanelTokens.parseLinkURL("") == nil)
    }

    @Test("A link's title and secondary line default to its host")
    func host() {
        let url = URL(string: "https://www.example.com/runbook")!
        #expect(MonarchLinkedDocsPanelTokens.host(url) == "example.com")
        #expect(MonarchLinkedDocsPanelTokens.linkTitle("  ", url: url) == "example.com")
        #expect(MonarchLinkedDocsPanelTokens.linkTitle(" Runbook ", url: url) == "Runbook")
        #expect(MonarchLinkedDocsPanelTokens.meta(items[2]) == "example.com")
        #expect(MonarchLinkedDocsPanelTokens.meta(items[0]) == "Updated 2 hours ago")
        #expect(MonarchLinkedDocsPanelTokens.meta(items[1]) == nil)
    }

    @Test("Spaces are counted, sorted by name, with No space last")
    func spaces() {
        let spaces = MonarchLinkedDocsPanelTokens.spaces(items, noSpace: "No space", locale: locale)
        #expect(spaces.map(\.name) == ["Design", "Marketing", "No space"])
        #expect(spaces.map(\.count) == [1, 2, 1])
        #expect(MonarchLinkedDocsPanelTokens.showsFilter(isEnabled: true, spaces: spaces))
        #expect(!MonarchLinkedDocsPanelTokens.showsFilter(isEnabled: false, spaces: spaces))
        #expect(!MonarchLinkedDocsPanelTokens.showsFilter(isEnabled: true, spaces: Array(spaces.prefix(1))))
    }

    @Test("The filter keeps one space, and an unknown space keeps every row")
    func filter() {
        let spaces = MonarchLinkedDocsPanelTokens.spaces(items, noSpace: "No space", locale: locale)
        #expect(MonarchLinkedDocsPanelTokens.filtered(items, key: "mkt", spaces: spaces).map(\.id) == ["1", "4"])
        #expect(
            MonarchLinkedDocsPanelTokens.filtered(items, key: MonarchLinkedDocsPanelTokens.noSpaceKey, spaces: spaces)
                .map(\.id) == ["3"]
        )
        #expect(MonarchLinkedDocsPanelTokens.filtered(items, key: "", spaces: spaces).count == 4)
        #expect(MonarchLinkedDocsPanelTokens.filtered(items, key: "gone", spaces: spaces).count == 4)
    }

    @Test("The picker leaves out linked docs and matches the query without case or accents")
    func pickerOptions() {
        let all = MonarchLinkedDocsPanelTokens.availableOptions(options, items: items, query: "", locale: locale)
        #expect(all.map(\.docID) == ["d9", "d10"])
        let matched = MonarchLinkedDocsPanelTokens.availableOptions(
            options,
            items: items,
            query: "resume",
            locale: locale
        )
        #expect(matched.map(\.docID) == ["d10"])
        let byDescription = MonarchLinkedDocsPanelTokens.availableOptions(
            options,
            items: items,
            query: "FIN",
            locale: locale
        )
        #expect(byDescription.map(\.docID) == ["d9"])
    }

    @Test("VoiceOver reads the secondary line and status after the title")
    func accessibility() {
        #expect(MonarchLinkedDocsPanelTokens.accessibilityValue(meta: "Updated", status: "Draft") == "Updated, Draft")
        #expect(MonarchLinkedDocsPanelTokens.accessibilityValue(meta: nil, status: nil).isEmpty)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchLinkedDocsPanelStrings.title == "Docs")
        #expect(MonarchLinkedDocsPanelStrings.unlink("Runbook") == "Unlink Runbook")
        #expect(MonarchLinkedDocsPanelStrings.hiddenByFilter(3) == "3 more in other spaces.")
        for value in [
            MonarchLinkedDocsPanelStrings.linkDoc, MonarchLinkedDocsPanelStrings.addLink,
            MonarchLinkedDocsPanelStrings.newDoc, MonarchLinkedDocsPanelStrings.searchDocs,
            MonarchLinkedDocsPanelStrings.noDocs, MonarchLinkedDocsPanelStrings.empty,
            MonarchLinkedDocsPanelStrings.emptyReadOnly, MonarchLinkedDocsPanelStrings.loading,
            MonarchLinkedDocsPanelStrings.add, MonarchLinkedDocsPanelStrings.filterBySpace,
            MonarchLinkedDocsPanelStrings.all, MonarchLinkedDocsPanelStrings.noSpace,
            MonarchLinkedDocsPanelStrings.showAll, MonarchLinkedDocsPanelStrings.unlinkFailed,
            MonarchLinkedDocsPanelStrings.linkFailed, MonarchLinkedDocsPanelStrings.urlLabel,
            MonarchLinkedDocsPanelStrings.urlInvalid, MonarchLinkedDocsPanelStrings.titleLabel,
            MonarchLinkedDocsPanelStrings.confirmAdd, MonarchLinkedDocsPanelStrings.cancel,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
