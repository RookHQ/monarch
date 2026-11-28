import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchEmojiPickerFixtures: ComponentFixtures {
    static let id = "emoji-picker"
    static let fixtures = [
        Fixture("built-in index") {
            MonarchEmojiPicker { _, _ in }
        },
        Fixture("recents and a skin tone") {
            MonarchEmojiPicker(recents: ["👍🏽", "🎉", "❤️"], skinTone: .constant(.medium)) { _, _ in }
        },
        Fixture("custom categories, no skin tones") {
            MonarchEmojiPicker(
                categories: [
                    MonarchEmojiPickerCategory(
                        id: "team",
                        label: "Team",
                        emojis: [
                            MonarchEmojiPickerItem("🚀", name: "rocket", keywords: ["ship"]),
                            MonarchEmojiPickerItem("✅", name: "check mark button", keywords: ["done"]),
                        ]
                    )
                ],
                showsSkinTones: false
            ) { _, _ in }
        },
        Fixture("disabled") {
            MonarchEmojiPicker { _, _ in }.disabled(true)
        },
    ]
}

@Suite("MonarchEmojiPicker")
struct MonarchEmojiPickerTests {
    private typealias Tokens = MonarchEmojiPickerTokens
    private let labels = MonarchEmojiPickerLabels()

    @Test("The built-in index has nine categories and about 500 emoji")
    func index() {
        let categories = MonarchEmojiPickerCategory.defaults
        #expect(
            categories.map(\.id) == [
                "smileys", "people", "animals", "food", "travel", "activities", "objects", "symbols", "flags",
            ]
        )
        let count = categories.reduce(0) { $0 + $1.emojis.count }
        #expect(count == 517)
        #expect(categories.allSatisfy { !$0.emojis.isEmpty })
        let wave = categories[1].emojis.first
        #expect(wave?.emoji == "👋")
        #expect(wave?.name == "waving hand")
        #expect(wave?.acceptsSkinTone == true)
        #expect(wave?.keywords.contains("hello") == true)
    }

    @Test("Rows of the index are parsed; malformed rows are skipped")
    func rows() {
        #expect(Tokens.item(fromRow: "😀|grinning face|smile happy|")?.keywords == ["smile", "happy"])
        #expect(Tokens.item(fromRow: "👍|thumbs up||1")?.acceptsSkinTone == true)
        #expect(Tokens.item(fromRow: "👍|thumbs up||1")?.keywords == [])
        #expect(Tokens.item(fromRow: "nothing") == nil)
        #expect(Tokens.item(fromRow: "|no emoji||") == nil)
    }

    @Test("Search matches word starts in names and keywords, names first")
    func search() {
        let results = Tokens.search("thumbs", in: MonarchEmojiPickerCategory.defaults)
        #expect(results.first?.emoji == "👍")
        #expect(Tokens.search("   ", in: MonarchEmojiPickerCategory.defaults).isEmpty)
        #expect(Tokens.search("zzzz", in: MonarchEmojiPickerCategory.defaults).isEmpty)
        let multi = Tokens.search("grin sweat", in: MonarchEmojiPickerCategory.defaults)
        #expect(multi.map(\.emoji).contains("😅"))
        #expect(Tokens.search("smile", in: MonarchEmojiPickerCategory.defaults, limit: 2).count == 2)
        let custom = [
            MonarchEmojiPickerCategory(
                id: "a",
                emojis: [
                    MonarchEmojiPickerItem("🚀", name: "rocket", keywords: ["ship"]),
                    MonarchEmojiPickerItem("🛳️", name: "ship"),
                ]
            )
        ]
        #expect(Tokens.search("ship", in: custom).map(\.emoji) == ["🛳️", "🚀"])
    }

    @Test("Skin tones go after the first character; other modifiers are removed")
    func skinTones() {
        #expect(Tokens.applying(.none, to: "👍") == "👍")
        #expect(Tokens.applying(.medium, to: "👍") == "👍\u{1F3FD}")
        #expect(Tokens.applying(.dark, to: "👍\u{1F3FB}") == "👍\u{1F3FF}")
        #expect(Tokens.applying(.light, to: "✌️") == "✌\u{1F3FB}")
        #expect(Tokens.base(of: "👍\u{1F3FD}") == "👍")
        #expect(MonarchEmojiSkinTone.allCases.compactMap(\.modifier).count == 5)
        #expect(MonarchEmojiPicker.applying(.mediumDark, to: "👋") == "👋\u{1F3FE}")
    }

    @Test("Recents: newest first, no duplicates, capped")
    func recents() {
        #expect(Tokens.pushingRecent("🎉", to: ["👍", "🎉", "❤️"], max: 24) == ["🎉", "👍", "❤️"])
        #expect(Tokens.pushingRecent("😀", to: ["👍", "🎉"], max: 2) == ["😀", "👍"])
        #expect(Tokens.pushingRecent("😀", to: ["👍"], max: 0).isEmpty)
        #expect(MonarchEmojiPicker.pushingRecent("😀", to: []) == ["😀"])
    }

    @Test("Sections: recents first, skin tone applied; search replaces them")
    func sections() {
        let sections = Tokens.sections(
            query: "",
            categories: MonarchEmojiPickerCategory.defaults,
            recents: ["👍🏽", "unknown"],
            maxRecents: 1,
            tone: .medium,
            labels: labels
        )
        #expect(sections.first?.id == "recent")
        #expect(sections.first?.label == "Recently used")
        #expect(sections.first?.entries.count == 1)
        #expect(sections.first?.entries.first?.item.name == "thumbs up")
        #expect(sections.count == 10)
        let people = sections.first { $0.id == "people" }
        #expect(people?.label == "People & body")
        #expect(people?.entries.first?.display == "👋\u{1F3FD}")
        let smileys = sections.first { $0.id == "smileys" }
        #expect(smileys?.entries.first?.display == "😀")

        let search = Tokens.sections(
            query: " party ",
            categories: MonarchEmojiPickerCategory.defaults,
            recents: ["👍"],
            maxRecents: 24,
            tone: .none,
            labels: labels
        )
        #expect(search.map(\.id) == ["search"])
        #expect(search.first?.label == "Search results")
        #expect(search.first?.entries.isEmpty == false)
    }

    @Test("Category names: the given label, the labels, the built-in name, then the id")
    func categoryNames() {
        let own = MonarchEmojiPickerCategory(id: "team", label: "Team", emojis: [])
        let builtIn = MonarchEmojiPickerCategory(id: "food", emojis: [])
        let unknown = MonarchEmojiPickerCategory(id: "misc", emojis: [])
        #expect(Tokens.categoryName(own, labels: labels) == "Team")
        #expect(Tokens.categoryName(builtIn, labels: labels) == "Food & drink")
        #expect(Tokens.categoryName(unknown, labels: labels) == "misc")
        let custom = MonarchEmojiPickerLabels(categoryName: { $0 == "food" ? "Essen" : nil })
        #expect(Tokens.categoryName(builtIn, labels: custom) == "Essen")
    }

    @Test("Announcements and skin tone names")
    func spoken() {
        #expect(Tokens.resultsAnnouncement(1, labels: labels) == "1 emoji found")
        #expect(Tokens.resultsAnnouncement(12, labels: labels) == "12 emoji found")
        #expect(Tokens.resultsAnnouncement(0, labels: labels) == "0 emoji found")
        #expect(Tokens.skinToneName(.mediumLight, labels: labels) == "Medium-light")
        #expect(Tokens.skinToneName(.none, labels: labels) == "Default")
    }

    @Test("The built-in strings resolve")
    func strings() {
        let strings = MonarchEmojiPickerStrings.self
        #expect(strings.label == "Emoji picker")
        #expect(strings.search == "Search emoji")
        #expect(strings.clearSearch == "Clear search")
        #expect(strings.categoryBar == "Emoji categories")
        #expect(strings.noResults == "No emoji found")
        #expect(strings.skinTone == "Skin tone")
        for id in strings.categoryIDs {
            #expect(strings.category(id)?.hasPrefix("monarch.") == false)
        }
        for tone in MonarchEmojiSkinTone.allCases {
            #expect(!strings.skinToneName(tone).hasPrefix("monarch."))
        }
    }

    @Test("The picker is the grid plus at least a 44-point search row high")
    @MainActor
    func touchTargets() {
        let size = Host.layout(MonarchEmojiPicker { _, _ in })
        #expect(size.height >= MonarchEmojiPickerTokens.gridHeight + MonarchAccessibility.minimumHitTarget)
    }
}
