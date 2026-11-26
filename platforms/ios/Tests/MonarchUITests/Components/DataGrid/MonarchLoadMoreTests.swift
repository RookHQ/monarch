import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLoadMoreFixtures: ComponentFixtures {
    static let id = "load-more"
    static let fixtures = [
        Fixture("idle with a count of a total") {
            MonarchLoadMore(hasMore: true, loadedCount: 20, totalCount: 120, onLoadMore: {})
        },
        Fixture("loading without a total") {
            MonarchLoadMore(hasMore: true, isLoading: true, loadedCount: 40, onLoadMore: {})
        },
        Fixture("failed and retrying") {
            VStack {
                MonarchLoadMore(hasMore: true, isFailed: true, loadedCount: 40, onRetry: {}, onLoadMore: {})
                MonarchLoadMore(hasMore: true, isLoading: true, isFailed: true, onLoadMore: {})
            }
        },
        Fixture("end, with and without the end line") {
            VStack {
                MonarchLoadMore(hasMore: false, loadedCount: 1, totalCount: 1, onLoadMore: {})
                MonarchLoadMore(hasMore: false, showsEnd: false, onLoadMore: {})
            }
        },
        Fixture("custom labels, long text") {
            MonarchLoadMore(
                hasMore: true,
                loadedCount: 3,
                labels: MonarchLoadMoreLabels(
                    loadMore: "Show older messages from the previous weeks",
                    count: { loaded, _ in "\(loaded) messages from this conversation are on screen" }
                ),
                onLoadMore: {}
            )
        },
    ]
}

@Suite("MonarchLoadMore")
struct MonarchLoadMoreTests {
    private typealias Snapshot = MonarchLoadMoreTokens.Snapshot

    @Test("A failure wins over loading, and loading over idle")
    func phase() {
        #expect(MonarchLoadMoreTokens.phase(hasMore: true, isLoading: false, isFailed: false) == .idle)
        #expect(MonarchLoadMoreTokens.phase(hasMore: true, isLoading: true, isFailed: false) == .loading)
        #expect(MonarchLoadMoreTokens.phase(hasMore: true, isLoading: true, isFailed: true) == .failed)
        #expect(MonarchLoadMoreTokens.phase(hasMore: false, isLoading: false, isFailed: false) == .done)
    }

    @Test("The count is shown when the loaded count is known")
    func count() {
        let labels = MonarchLoadMoreLabels()
        func count(_ loaded: Int?, _ total: Int?, _ shows: Bool? = nil) -> String? {
            MonarchLoadMoreTokens.countText(loadedCount: loaded, totalCount: total, showsCount: shows, labels: labels)
        }
        #expect(count(40, 120) == "Showing 40 of 120 items")
        #expect(count(1, 1) == "Showing 1 of 1 item")
        #expect(count(40, nil) == "Showing 40 items")
        #expect(count(1, nil) == "Showing 1 item")
        #expect(count(nil, 120) == nil)
        #expect(count(40, 120, false) == nil)
        let custom = MonarchLoadMoreLabels(count: { "\($0)/\($1 ?? 0)" })
        #expect(
            MonarchLoadMoreTokens.countText(loadedCount: 2, totalCount: 9, showsCount: nil, labels: custom) == "2/9"
        )
    }

    @Test("New items, a failure and the end are announced")
    func announcements() {
        let labels = MonarchLoadMoreLabels()
        let start = Snapshot(loadedCount: 20, isFailed: false, hasMore: true)
        func announce(_ new: Snapshot, showsEnd: Bool = true) -> String? {
            MonarchLoadMoreTokens.announcement(from: start, to: new, showsEnd: showsEnd, labels: labels)
        }
        #expect(announce(Snapshot(loadedCount: 40, isFailed: false, hasMore: true)) == "20 more items loaded.")
        #expect(announce(Snapshot(loadedCount: 21, isFailed: false, hasMore: true)) == "1 more item loaded.")
        #expect(announce(Snapshot(loadedCount: 20, isFailed: true, hasMore: true)) == "Couldn’t load more.")
        #expect(
            announce(Snapshot(loadedCount: 25, isFailed: false, hasMore: false))
                == "5 more items loaded. That’s everything"
        )
        let end = Snapshot(loadedCount: 25, isFailed: false, hasMore: false)
        #expect(announce(end, showsEnd: false) == "5 more items loaded.")
        #expect(announce(start) == nil)
        #expect(announce(Snapshot(loadedCount: 0, isFailed: false, hasMore: true)) == nil)
    }

    @Test("Auto-loading needs the footer on screen, more to load, and no fetch or failure")
    func autoLoad() {
        #expect(
            MonarchLoadMoreTokens.shouldAutoLoad(
                autoLoads: true,
                isVisible: true,
                hasMore: true,
                isLoading: false,
                isFailed: false
            )
        )
        let blocked = [
            (false, true, true, false, false),
            (true, false, true, false, false),
            (true, true, false, false, false),
            (true, true, true, true, false),
            (true, true, true, false, true),
        ]
        for (autoLoads, isVisible, hasMore, isLoading, isFailed) in blocked {
            #expect(
                !MonarchLoadMoreTokens.shouldAutoLoad(
                    autoLoads: autoLoads,
                    isVisible: isVisible,
                    hasMore: hasMore,
                    isLoading: isLoading,
                    isFailed: isFailed
                )
            )
        }
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchLoadMoreStrings.loadMore == "Load more")
        #expect(MonarchLoadMoreStrings.loading == "Loading…")
        #expect(MonarchLoadMoreStrings.failed == "Couldn’t load more.")
        #expect(MonarchLoadMoreStrings.retry == "Retry")
        #expect(MonarchLoadMoreStrings.end == "That’s everything")
        #expect(MonarchLoadMoreStrings.loaded(3) == "3 more items loaded.")
    }
}
