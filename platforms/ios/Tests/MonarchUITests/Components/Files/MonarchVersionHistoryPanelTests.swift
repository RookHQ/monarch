import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchVersionHistoryPanelFixtures: ComponentFixtures {
    static let id = "version-history-panel"

    /// A fixed moment, so the fixtures never depend on the clock.
    static let reference = Date(timeIntervalSince1970: 1_800_000_000)

    static let versions = [
        MonarchVersionHistoryPanelVersion(id: "v3", date: reference, authorName: "Ana Silva", note: "Launch copy"),
        MonarchVersionHistoryPanelVersion(
            id: "v2",
            date: reference.addingTimeInterval(-7_200),
            authorName: "Sam Lee",
            isAutomatic: true
        ),
        MonarchVersionHistoryPanelVersion(
            id: "v1",
            date: reference.addingTimeInterval(-172_800),
            authorName: "A colleague with a very long name that wraps",
            authorImage: nil,
            note: "First draft, before the review with the whole product and design team"
        ),
    ]

    static let current = ["Ship on Monday.", "Tell support.", "Update the status page."]
    static let older = ["Ship on Friday.", "Tell support."]

    @MainActor
    static func detail(lines: [String]?, canRestore: Bool = true) -> some View {
        NavigationStack {
            MonarchVersionHistoryPanelDetail(
                version: versions[1],
                currentLines: current,
                selectedLines: lines,
                canRestore: canRestore,
                labels: MonarchVersionHistoryPanelLabels(),
                onRestore: { _ in },
                onRestored: {}
            )
        }
    }

    static let fixtures = [
        Fixture("list with save, current and close") {
            MonarchVersionHistoryPanel(
                versions: versions,
                selection: .constant(nil),
                currentLines: current,
                onSaveVersion: { _ in },
                onRestore: { _ in },
                onClose: {}
            )
        },
        Fixture("list, read only, custom title") {
            MonarchVersionHistoryPanel(
                "Page history",
                versions: versions,
                selection: .constant(nil),
                currentLines: current
            )
        },
        Fixture("list loading") {
            MonarchVersionHistoryPanel(versions: [], selection: .constant(nil), currentLines: current, isLoading: true)
        },
        Fixture("list empty") {
            MonarchVersionHistoryPanel(
                versions: [],
                selection: .constant(nil),
                currentLines: current,
                onSaveVersion: { _ in }
            )
        },
        Fixture("version selected") {
            MonarchVersionHistoryPanel(
                versions: versions,
                selection: .constant("v2"),
                currentLines: current,
                selectedLines: older,
                onRestore: { _ in }
            )
        },
        Fixture("detail: changes with restore") { detail(lines: older) },
        Fixture("detail: identical, no restore") { detail(lines: current, canRestore: false) },
        Fixture("detail: loading") { detail(lines: nil) },
    ]
}

@Suite("MonarchVersionHistoryPanel")
struct MonarchVersionHistoryPanelTests {
    private typealias Tokens = MonarchVersionHistoryPanelTokens
    private let versions = MonarchVersionHistoryPanelFixtures.versions
    private let reference = MonarchVersionHistoryPanelFixtures.reference
    private let english = Locale(identifier: "en_US")
    private let labels = MonarchVersionHistoryPanelLabels()

    @Test("The current version is the given one, or the newest")
    func currentID() {
        #expect(Tokens.currentID(nil, versions: versions) == "v3")
        #expect(Tokens.currentID("v1", versions: versions) == "v1")
        #expect(Tokens.currentID(nil, versions: []) == nil)
    }

    @Test("Times are relative to the injected now, in the given locale")
    func times() {
        #expect(
            Tokens.relativeTime(reference.addingTimeInterval(-7_200), now: reference, locale: english) == "2 hours ago"
        )
        #expect(
            Tokens.relativeTime(reference.addingTimeInterval(-172_800), now: reference, locale: english) == "2 days ago"
        )
        let french = Tokens.relativeTime(
            reference.addingTimeInterval(-7_200),
            now: reference,
            locale: Locale(identifier: "fr_FR")
        )
        #expect(french.hasPrefix("il y a"))
        let absolute = Tokens.absoluteTime(reference, locale: english, timeZone: TimeZone(identifier: "UTC")!)
        #expect(absolute.contains("2027"))
        #expect(absolute.contains("8:00"))
    }

    @Test("The byline is the author with the name or Saved automatically")
    func byline() {
        #expect(Tokens.byline(versions[0], labels: labels) == "Ana Silva · Launch copy")
        #expect(Tokens.byline(versions[1], labels: labels) == "Sam Lee · auto")
        let plain = MonarchVersionHistoryPanelVersion(id: "x", date: reference, authorName: "Lin", note: "  ")
        #expect(Tokens.byline(plain, labels: labels) == "Lin")
        #expect(
            Tokens.byline(versions[1], labels: MonarchVersionHistoryPanelLabels(automatic: "auto")) == "Sam Lee · auto"
        )
    }

    @Test("VoiceOver reads when, who and the name, with Current as the value")
    func accessibility() {
        #expect(
            Tokens.accessibilityLabel(versions[0], relativeTime: "now", labels: labels) == "now, Ana Silva, Launch copy"
        )
        #expect(
            Tokens.accessibilityLabel(versions[1], relativeTime: "2 hours ago", labels: labels)
                == "2 hours ago, Sam Lee, auto"
        )
        #expect(Tokens.accessibilityValue(isCurrent: true, labels: labels) == "Current")
        #expect(Tokens.accessibilityValue(isCurrent: false, labels: labels).isEmpty)
    }

    @Test("Identical text is detected through the diff")
    func identical() {
        let current = MonarchVersionHistoryPanelFixtures.current
        #expect(Tokens.isIdentical(current, to: current))
        #expect(!Tokens.isIdentical(MonarchVersionHistoryPanelFixtures.older, to: current))
        #expect(Tokens.isIdentical([], to: []))
    }

    @Test("Restore is offered only with a handler and not on the current version")
    func canRestore() {
        #expect(Tokens.canRestore(versionID: "v2", currentID: "v3", hasHandler: true))
        #expect(!Tokens.canRestore(versionID: "v3", currentID: "v3", hasHandler: true))
        #expect(!Tokens.canRestore(versionID: "v2", currentID: "v3", hasHandler: false))
    }

    @Test("The saved name is trimmed and cut to 200 characters")
    func note() {
        #expect(Tokens.note("  Launch copy \n") == "Launch copy")
        #expect(Tokens.note(String(repeating: "a", count: 250)).count == 200)
        #expect(Tokens.note("   ").isEmpty)
    }

    @Test("Tab titles, built in and replaced")
    func tabs() {
        #expect(Tokens.title(.changes, labels: labels) == "Changes")
        #expect(Tokens.title(.fullText, labels: labels) == "Full text")
        #expect(Tokens.title(.fullText, labels: MonarchVersionHistoryPanelLabels(fullText: "Text")) == "Text")
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchVersionHistoryPanelStrings.title == "History")
        #expect(MonarchVersionHistoryPanelStrings.save == "Save version")
        #expect(MonarchVersionHistoryPanelStrings.empty == "No versions yet. Save one to mark this moment.")
        #expect(MonarchVersionHistoryPanelStrings.restoreTitle == "Restore this version?")
        #expect(
            MonarchVersionHistoryPanelStrings.restoreDescription == "The current content is replaced with this version."
        )
        #expect(MonarchVersionHistoryPanelStrings.restoreFailed == "The version could not be restored. Try again.")
        #expect(MonarchVersionHistoryPanelStrings.now == "Now")
        #expect(MonarchVersionHistoryPanelStrings.loading == "Loading")
        #expect(MonarchVersionHistoryPanelStrings.view == "View")
    }
}
