import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

private struct CursorListRequest: Identifiable {
    let id: Int
    var title: String { "Access request \(id)" }
}

private let cursorListHeight = MonarchTokens.Space.space24 * 4

enum MonarchCursorListFixtures: ComponentFixtures {
    static let id = "cursor-list"
    static let fixtures = [
        Fixture("a page with more and a total") {
            MonarchCursorList(
                (1...4).map(CursorListRequest.init),
                label: "Requests",
                hasMore: true,
                totalCount: 40,
                onLoadMore: {},
                row: { Text($0.title).monarchTextStyle(.body) }
            )
            .frame(height: cursorListHeight)
        },
        Fixture("loading the next page, undivided") {
            MonarchCursorList(
                (1...2).map(CursorListRequest.init),
                label: "Requests",
                hasMore: true,
                isLoading: true,
                isDivided: false,
                onLoadMore: {},
                row: { Text($0.title).monarchTextStyle(.body) }
            )
            .frame(height: cursorListHeight)
        },
        Fixture("a later page failed") {
            MonarchCursorList(
                (1...2).map(CursorListRequest.init),
                label: "Requests",
                hasMore: true,
                isFailed: true,
                onLoadMore: {},
                row: { Text($0.title).monarchTextStyle(.body) }
            )
            .frame(height: cursorListHeight)
        },
        Fixture("everything loaded") {
            MonarchCursorList(
                (1...2).map(CursorListRequest.init),
                label: "Requests",
                hasMore: false,
                totalCount: 2,
                onLoadMore: {},
                row: { Text($0.title).monarchTextStyle(.body) }
            )
            .frame(height: cursorListHeight)
        },
        Fixture("first page loading") {
            MonarchCursorList(
                [CursorListRequest](),
                label: "Requests",
                hasMore: true,
                isLoading: true,
                loadingVariant: .rows,
                onLoadMore: {},
                row: { Text($0.title) }
            )
            .frame(height: cursorListHeight)
        },
        Fixture("first page failed, retrying") {
            MonarchCursorList(
                [CursorListRequest](),
                label: "Requests",
                hasMore: true,
                isLoading: true,
                isFailed: true,
                onLoadMore: {},
                row: { Text($0.title) }
            )
            .frame(height: cursorListHeight)
        },
        Fixture("empty with custom labels") {
            MonarchCursorList(
                [CursorListRequest](),
                label: "Requests",
                hasMore: false,
                labels: MonarchCursorListLabels(
                    emptyTitle: "No access requests",
                    emptyMessage: "Requests from people who want to join this workspace appear here for review."
                ),
                onLoadMore: {},
                row: { Text($0.title) }
            )
            .frame(height: cursorListHeight)
        },
    ]
}

@Suite("MonarchCursorList")
struct MonarchCursorListTests {
    private typealias Tokens = MonarchCursorListTokens

    @Test("With no items a failure wins, then loading, then empty when nothing more")
    func phase() {
        #expect(Tokens.phase(count: 0, hasMore: true, isLoading: true, isFailed: true) == .failed)
        #expect(Tokens.phase(count: 0, hasMore: true, isLoading: true, isFailed: false) == .loading)
        #expect(Tokens.phase(count: 0, hasMore: false, isLoading: false, isFailed: false) == .empty)
        #expect(Tokens.phase(count: 0, hasMore: true, isLoading: false, isFailed: false) == .ready)
        #expect(Tokens.phase(count: 3, hasMore: false, isLoading: true, isFailed: true) == .ready)
    }

    @Test("Focus moves to the first new item once the page has arrived")
    func focusTarget() {
        #expect(Tokens.focusTarget(pending: 20, count: 40, isLoading: false) == 20)
        #expect(Tokens.focusTarget(pending: 20, count: 40, isLoading: true) == nil)
        #expect(Tokens.focusTarget(pending: 20, count: 20, isLoading: false) == nil)
        #expect(Tokens.focusTarget(pending: nil, count: 40, isLoading: false) == nil)
        #expect(Tokens.focusTarget(pending: -1, count: 40, isLoading: false) == nil)
    }

    @Test("The list's retry title fills the footer's when it has none")
    func footerLabels() {
        let shared = Tokens.footerLabels(MonarchCursorListLabels(retry: "Try again"))
        #expect(shared.retry == "Try again")
        let own = Tokens.footerLabels(
            MonarchCursorListLabels(retry: "Try again", footer: MonarchLoadMoreLabels(retry: "Reload"))
        )
        #expect(own.retry == "Reload")
        #expect(Tokens.footerLabels(MonarchCursorListLabels()).retry == nil)
    }

    @Test("The built-in strings resolve")
    func strings() {
        #expect(MonarchCursorListStrings.loading == "Loading")
        #expect(MonarchCursorListStrings.emptyTitle == "Nothing here yet")
        #expect(MonarchCursorListStrings.emptyMessage == "Items appear here once they are added.")
        #expect(MonarchCursorListStrings.errorTitle == "This list could not be loaded")
    }
}
