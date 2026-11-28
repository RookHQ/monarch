import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let start = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let versions = [
    MonarchAiArtifactVersion(3, createdAt: start.addingTimeInterval(1_200), label: "Final", content: "Ship on Monday."),
    MonarchAiArtifactVersion(1, createdAt: start, content: "Ship on Friday.\nTell support."),
    MonarchAiArtifactVersion(
        2,
        createdAt: start.addingTimeInterval(600),
        label: "Shorter intro",
        content: "Ship on Friday."
    ),
    MonarchAiArtifactVersion(4, createdAt: start.addingTimeInterval(1_800)),
]

enum MonarchAiArtifactVersionsFixtures: ComponentFixtures {
    static let id = "ai-artifact-versions"
    static let fixtures = [
        Fixture("middle version") {
            MonarchAiArtifactVersions(selection: .constant(2), versions: versions)
        },
        Fixture("comparing, word diff") {
            MonarchAiArtifactVersions(selection: .constant(3), versions: versions, isComparing: .constant(true))
        },
        Fixture("comparing, line diff") {
            MonarchAiArtifactVersions(
                selection: .constant(2),
                versions: versions,
                isComparing: .constant(true),
                granularity: .line
            )
        },
        Fixture("first and last, compare hidden") {
            VStack(alignment: .leading) {
                MonarchAiArtifactVersions(selection: .constant(1), versions: versions)
                MonarchAiArtifactVersions(selection: .constant(4), versions: versions)
                MonarchAiArtifactVersions(selection: .constant(3), versions: versions, showsCompare: false)
            }
        },
        Fixture("single version") {
            MonarchAiArtifactVersions(selection: .constant(1), versions: [versions[1]])
        },
        Fixture("no versions") {
            MonarchAiArtifactVersions(selection: .constant(1), versions: [])
        },
        Fixture("disabled") {
            MonarchAiArtifactVersions(selection: .constant(2), versions: versions).disabled(true)
        },
    ]
}

@Suite("MonarchAiArtifactVersions")
struct MonarchAiArtifactVersionsTests {
    private typealias Tokens = MonarchAiArtifactVersionsTokens
    private let labels = MonarchAiArtifactVersionsLabels()

    @Test("Versions are oldest first, one per number")
    func sorted() {
        #expect(Tokens.sorted(versions).map(\.version) == [1, 2, 3, 4])
        let duplicate = versions + [MonarchAiArtifactVersion(2, createdAt: start, label: "Copy")]
        #expect(Tokens.sorted(duplicate).map(\.version) == [1, 2, 3, 4])
    }

    @Test("A selection not in the list shows the newest version")
    func index() {
        let sorted = Tokens.sorted(versions)
        #expect(Tokens.index(of: 1, in: sorted) == 0)
        #expect(Tokens.index(of: 3, in: sorted) == 2)
        #expect(Tokens.index(of: 9, in: sorted) == 3)
        #expect(Tokens.index(of: 1, in: []) == nil)
    }

    @Test("Compare needs a previous version and content on both")
    func comparison() {
        let sorted = Tokens.sorted(versions)
        #expect(Tokens.comparison(at: 0, in: sorted) == nil)
        let pair = Tokens.comparison(at: 2, in: sorted)
        #expect(pair?.before.version == 2)
        #expect(pair?.after.version == 3)
        #expect(Tokens.comparison(at: 3, in: sorted) == nil)
        #expect(Tokens.previous(at: 1, in: sorted)?.version == 1)
        #expect(Tokens.previous(at: 0, in: sorted) == nil)
    }

    @Test("Position, announcement and version names use the version number")
    func texts() {
        #expect(Tokens.position(3, total: 5, labels: labels) == "Version 3 of 5")
        #expect(Tokens.announcement(3, total: 5, labels: labels) == "Showing version 3 of 5")
        #expect(Tokens.versionName(2, labels: labels) == "Version 2")
        let custom = MonarchAiArtifactVersionsLabels(position: { "\($0)/\($1)" }, versionName: { "Rev \($0)" })
        #expect(Tokens.position(3, total: 5, labels: custom) == "3/5")
        #expect(Tokens.versionName(2, labels: custom) == "Rev 2")
    }

    @Test("A menu item is the tag, the name and the date in the given locale and time zone")
    func option() throws {
        let utc = try #require(TimeZone(identifier: "UTC"))
        let calendar = Calendar(identifier: .gregorian)
        let named = Tokens.option(
            versions[2],
            locale: Locale(identifier: "en_US"),
            calendar: calendar,
            timeZone: utc,
            labels: labels
        )
        #expect(named.hasPrefix("v2 · Shorter intro · "))
        #expect(named.contains("2026"))
        let plain = Tokens.option(
            versions[1],
            locale: Locale(identifier: "en_US"),
            calendar: calendar,
            timeZone: utc,
            labels: labels
        )
        #expect(plain.hasPrefix("v1 · "))
        #expect(plain.components(separatedBy: " · ").count == 2)
        let german = Tokens.option(
            versions[1],
            locale: Locale(identifier: "de_DE"),
            calendar: calendar,
            timeZone: utc,
            labels: MonarchAiArtifactVersionsLabels(versionTag: { "V\($0)" })
        )
        #expect(german.hasPrefix("V1 · "))
        #expect(german != plain)
    }

    @Test("The built-in strings come from the component's table")
    func strings() {
        #expect(MonarchAiArtifactVersionsStrings.label == "Versions")
        #expect(MonarchAiArtifactVersionsStrings.versionTag(3) == "v3")
        for value in [
            MonarchAiArtifactVersionsStrings.previous, MonarchAiArtifactVersionsStrings.next,
            MonarchAiArtifactVersionsStrings.select, MonarchAiArtifactVersionsStrings.compare,
        ] {
            #expect(!value.hasPrefix("monarch."))
        }
    }
}
