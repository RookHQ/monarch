import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private let breadcrumbFolders = ["Drive", "Projects", "Monarch", "Specs", "Navigation", "Breadcrumb"].map {
    MonarchBreadcrumbItem($0)
}

enum MonarchBreadcrumbFixtures: ComponentFixtures {
    static let id = "breadcrumb"
    static let fixtures = [
        Fixture("short trail") {
            MonarchBreadcrumb([MonarchBreadcrumbItem("Drive"), MonarchBreadcrumbItem("Projects")]) { _ in }
        },
        Fixture("collapsed middle levels") {
            MonarchBreadcrumb(breadcrumbFolders) { _ in }
        },
        Fixture("custom maxVisible and labels") {
            MonarchBreadcrumb(
                breadcrumbFolders,
                maxVisible: 5,
                labels: MonarchBreadcrumbLabels(label: "Folders", more: { "\($0) hidden folders" }, current: "Here")
            ) { _ in }
        },
        Fixture("a level without a screen, one level") {
            VStack(alignment: .leading) {
                MonarchBreadcrumb(
                    [
                        MonarchBreadcrumbItem("Drive"),
                        MonarchBreadcrumbItem("Shared with me", isLinked: false),
                        MonarchBreadcrumbItem("Quarterly report"),
                    ]
                ) { _ in }
                MonarchBreadcrumb([MonarchBreadcrumbItem("Drive")]) { _ in }
            }
        },
        Fixture("long titles, wider than the screen") {
            MonarchBreadcrumb(
                [
                    MonarchBreadcrumbItem("Everything the organisation has shared"),
                    MonarchBreadcrumbItem("Finance and accounting for the northern region"),
                    MonarchBreadcrumbItem("Invoices received in the second quarter of the year"),
                ]
            ) { _ in }
        },
        Fixture("disabled") {
            MonarchBreadcrumb(breadcrumbFolders) { _ in }.disabled(true)
        },
    ]
}

@Suite("MonarchBreadcrumb")
struct MonarchBreadcrumbTests {
    @Test("A trail that fits shows every level")
    func noCollapse() {
        #expect(MonarchBreadcrumbTokens.segments(count: 4, maxVisible: 4) == [.item(0), .item(1), .item(2), .item(3)])
        #expect(MonarchBreadcrumbTokens.segments(count: 1, maxVisible: 4) == [.item(0)])
        #expect(MonarchBreadcrumbTokens.segments(count: 0, maxVisible: 4).isEmpty)
    }

    @Test("A long trail keeps the first level and the last maxVisible - 2, as on the web")
    func collapse() {
        #expect(
            MonarchBreadcrumbTokens.segments(count: 6, maxVisible: 4)
                == [.item(0), .collapsed([1, 2, 3]), .item(4), .item(5)]
        )
        #expect(
            MonarchBreadcrumbTokens.segments(count: 6, maxVisible: 5)
                == [.item(0), .collapsed([1, 2]), .item(3), .item(4), .item(5)]
        )
    }

    @Test("maxVisible below 3 still shows the first level, the menu and the current level")
    func minimum() {
        for limit in [-1, 0, 1, 2, 3] {
            #expect(
                MonarchBreadcrumbTokens.segments(count: 5, maxVisible: limit)
                    == [.item(0), .collapsed([1, 2, 3]), .item(4)]
            )
        }
    }

    @Test("Every level appears exactly once, visible or collapsed", arguments: 0...12)
    func everyLevelOnce(count: Int) {
        for limit in 3...6 {
            let indices = MonarchBreadcrumbTokens.segments(count: count, maxVisible: limit).flatMap { segment in
                switch segment {
                case .item(let index): [index]
                case .collapsed(let indices): indices
                }
            }
            #expect(indices == Array(0..<count))
        }
    }

    @Test("The last level is current, the others link unless they have no screen")
    func kinds() {
        #expect(MonarchBreadcrumbTokens.kind(index: 2, count: 3, isLinked: true) == .current)
        #expect(MonarchBreadcrumbTokens.kind(index: 2, count: 3, isLinked: false) == .current)
        #expect(MonarchBreadcrumbTokens.kind(index: 0, count: 3, isLinked: true) == .link)
        #expect(MonarchBreadcrumbTokens.kind(index: 1, count: 3, isLinked: false) == .text)
    }

    @Test("The current level has the default text colour and the others the muted one")
    func colors() {
        #expect(MonarchBreadcrumbTokens.textColor(.current) == \MonarchColorTokens.textDefault)
        #expect(MonarchBreadcrumbTokens.textColor(.text) == \MonarchColorTokens.textMuted)
        #expect(MonarchBreadcrumbTokens.separatorColor == \MonarchColorTokens.textSubtle)
    }

    @Test("Spoken strings come from the table and can be replaced")
    func spoken() {
        let builtIn = MonarchBreadcrumbLabels()
        let english = Locale(identifier: "en_US")
        #expect(MonarchBreadcrumbTokens.label(builtIn) == "Breadcrumb")
        #expect(MonarchBreadcrumbTokens.moreLabel(count: 3, labels: builtIn, locale: english) == "Show 3 more levels")
        #expect(
            MonarchBreadcrumbTokens.moreLabel(count: 1200, labels: builtIn, locale: english) == "Show 1,200 more levels"
        )
        #expect(MonarchBreadcrumbTokens.value(.current, labels: builtIn) == "Current page")
        #expect(MonarchBreadcrumbTokens.value(.link, labels: builtIn).isEmpty)
        #expect(MonarchBreadcrumbTokens.value(.text, labels: builtIn).isEmpty)

        let custom = MonarchBreadcrumbLabels(label: "Folders", more: { "\($0) hidden" }, current: "Here")
        #expect(MonarchBreadcrumbTokens.label(custom) == "Folders")
        #expect(MonarchBreadcrumbTokens.moreLabel(count: 2, labels: custom, locale: english) == "2 hidden")
        #expect(MonarchBreadcrumbTokens.value(.current, labels: custom) == "Here")
    }

    @Test("An item's id defaults to its title")
    func item() {
        let item = MonarchBreadcrumbItem("Projects")
        #expect(item.id == "Projects")
        #expect(item.isLinked)
        let other = MonarchBreadcrumbItem("Projects", id: "p-1", isLinked: false)
        #expect(other.id == "p-1")
        #expect(!other.isLinked)
    }

    #if os(iOS)
        @Test("A link level keeps a 44-point touch target")
        @MainActor
        func hitTarget() {
            let trail = MonarchBreadcrumb([MonarchBreadcrumbItem("A"), MonarchBreadcrumbItem("B")]) { _ in }
                .fixedSize()
            #expect(Host.layout(trail).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
