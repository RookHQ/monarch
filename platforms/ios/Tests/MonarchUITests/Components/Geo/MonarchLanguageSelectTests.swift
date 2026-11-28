import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLanguageSelectFixtures: ComponentFixtures {
    static let id = "language-select"
    static let english = Locale(identifier: "en_US")

    static let fixtures = [
        Fixture("chosen, endonym first, helper") {
            MonarchLanguageSelect("Language", selection: .constant("de"), helper: "For emails.", locale: english)
        },
        Fixture("empty with prompt, required, error") {
            MonarchLanguageSelect(
                "Language",
                selection: .constant(nil),
                prompt: "Choose a language",
                error: "Choose a language.",
                locale: english,
                isRequired: true
            )
        },
        Fixture("localized first, tags, priority, limited list") {
            MonarchLanguageSelect(
                "Subtitles",
                selection: .constant("PT-br"),
                languages: ["en", "fr", "pt-BR", "ja", "ar"],
                priority: ["ja"],
                locale: english,
                labelStyle: .localized,
                showsTag: true,
                isClearable: false
            )
        },
        Fixture("right-to-left language chosen, long label") {
            MonarchLanguageSelect(
                "The language we use for every message, invoice and reminder",
                selection: .constant("ar"),
                locale: english
            )
        },
        Fixture("disabled") {
            MonarchLanguageSelect("Language", selection: .constant("ja"), locale: english).disabled(true)
        },
    ]
}

@Suite("MonarchLanguageSelect")
struct MonarchLanguageSelectTests {
    private let english = Locale(identifier: "en_US")
    private let french = Locale(identifier: "fr_FR")

    private func options(
        languages: [String]? = nil,
        priority: [String] = [],
        selection: String? = nil,
        query: String = "",
        locale: Locale? = nil,
        labelStyle: MonarchLanguageSelectLabelStyle = .endonym,
        showsTag: Bool = false
    ) -> [MonarchComboboxOption] {
        MonarchLanguageSelectTokens.options(
            languages: languages,
            priority: priority,
            selection: selection,
            query: query,
            locale: locale ?? english,
            labelStyle: labelStyle,
            showsTag: showsTag,
            suggested: "Suggested",
            all: "All languages"
        )
    }

    @Test("Tags are made canonical; invalid and blank tags are dropped")
    func canonical() {
        #expect(MonarchLanguageSelectTokens.canonical("EN-us") == "en-US")
        #expect(MonarchLanguageSelectTokens.canonical("zh-Hans") == "zh-Hans")
        #expect(MonarchLanguageSelectTokens.canonical("!!") == nil)
        #expect(MonarchLanguageSelectTokens.canonical(" ") == nil)
        #expect(MonarchLanguageSelectTokens.tags(["de", "DE", "!!"]) == ["de"])
        #expect(MonarchLanguageSelectTokens.tags(nil).count == MonarchLanguageSelect.languageTags.count)
    }

    @Test("Names are capitalised for the locale")
    func names() {
        #expect(MonarchLanguageSelectTokens.endonym("de") == "Deutsch")
        #expect(MonarchLanguageSelectTokens.name("de", in: english) == "German")
        #expect(MonarchLanguageSelectTokens.name("de", in: french) == "Allemand")
        #expect(MonarchLanguageSelectTokens.capitalized("", locale: english).isEmpty)
    }

    @Test("Endonym style shows the own name first; localized style swaps them")
    func labelStyle() {
        let own = options(languages: ["de"])
        #expect(own.first?.label == "Deutsch")
        #expect(own.first?.description == "German")
        let localized = options(languages: ["de"], labelStyle: .localized)
        #expect(localized.first?.label == "German")
        #expect(localized.first?.description == "Deutsch")
        let same = options(languages: ["en"])
        #expect(same.first?.label == "English")
        #expect(same.first?.description == nil)
    }

    @Test("The tag is a hint only when asked, and always searched")
    func tag() {
        #expect(options(languages: ["de"]).first?.hint == nil)
        #expect(options(languages: ["de"], showsTag: true).first?.hint == "de")
        #expect(options(languages: ["de"], locale: french).first?.keywords == "de German")
    }

    @Test("Rows are sorted by the name in the interface's language")
    func order() {
        let list = options(languages: ["de", "fr", "ja"])
        #expect(list.map(\.id) == ["fr", "de", "ja"])
        let french = options(languages: ["de", "fr", "ja"], locale: french)
        #expect(french.map(\.id) == ["de", "fr", "ja"])
    }

    @Test("Priority languages come first only while nothing is typed; tags in any form are matched")
    func priority() {
        let idle = options(languages: ["de", "pt-BR"], priority: ["PT-br", "xx-unknown"])
        #expect(idle.map(\.id) == ["pt-BR", "de", "pt-BR"])
        #expect(idle.map(\.group) == ["Suggested", "All languages", "All languages"])
        let searching = options(languages: ["de", "pt-BR"], priority: ["pt-BR"], query: "port")
        #expect(searching.map(\.id) == ["de", "pt-BR"])
    }

    @Test("Search matches the own name, the other name, the tag and the English name")
    func search() {
        let german = options(languages: ["de"], locale: french)[0]
        for query in ["deutsch", "allemand", "german", "de"] {
            #expect(MonarchGeoSearch.matchesEveryWord(german, query: query))
        }
        #expect(!MonarchGeoSearch.matchesEveryWord(german, query: "japanese"))
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchLanguageSelectStrings.suggested == "Suggested")
        #expect(MonarchLanguageSelectStrings.all == "All languages")
        #expect(MonarchLanguageSelectStrings.empty == "No languages match")
    }
}
