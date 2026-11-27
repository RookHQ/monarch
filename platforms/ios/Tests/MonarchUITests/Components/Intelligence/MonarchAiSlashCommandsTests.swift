import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiSlashCommandsFixtures: ComponentFixtures {
    static let id = "ai-slash-commands"

    static let commands = [
        MonarchSlashMenuItem(
            id: "summarize",
            label: "summarize",
            description: "Summarize the open document",
            systemImage: "list.bullet",
            keywords: ["recap", "shorten"]
        ),
        MonarchSlashMenuItem(
            id: "rewrite",
            label: "/rewrite",
            description: "Rewrite the selection in a different tone",
            systemImage: "pencil"
        ),
        MonarchSlashMenuItem(
            id: "search",
            label: "search",
            description: "Search connected documents",
            systemImage: "magnifyingglass"
        ),
        MonarchSlashMenuItem(
            id: "schedule",
            label: "schedule",
            description: "Needs a calendar connection",
            systemImage: "calendar",
            isDisabled: true
        ),
    ]

    static let fixtures: [Fixture] = [
        Fixture("all commands after a slash") {
            MonarchAiSlashCommands(text: .constant("/"), commands: commands) { "/\($0.label) " }
        },
        Fixture("filtered mid-sentence") {
            MonarchAiSlashCommands(text: .constant("Please /re"), commands: commands) { _ in nil }
        },
        Fixture("nothing matches") {
            MonarchAiSlashCommands(text: .constant("/xyz"), commands: commands) { _ in nil }
        },
        Fixture("closed: no command typed") {
            MonarchAiSlashCommands(text: .constant("Hello"), commands: commands) { _ in nil }
        },
        Fixture("grouped with long descriptions") {
            MonarchAiSlashCommands(
                text: .constant("/"),
                commands: [
                    MonarchSlashMenuItem(
                        id: "a",
                        label: "translate",
                        description: String(repeating: "Translate the answer into another language. ", count: 3),
                        group: "Writing"
                    ),
                    MonarchSlashMenuItem(id: "b", label: "chart", description: "Draw a chart", group: "Data"),
                ],
                groupOrder: ["Writing", "Data"]
            ) { _ in nil }
        },
    ]
}

@Suite("MonarchAiSlashCommands")
struct MonarchAiSlashCommandsTests {
    private typealias Tokens = MonarchAiSlashCommandsTokens
    private let commands = MonarchAiSlashCommandsFixtures.commands

    @Test("Names lose their leading slashes")
    func bare() {
        #expect(Tokens.bare("/summarize", trigger: "/") == "summarize")
        #expect(Tokens.bare("//x", trigger: "/") == "x")
        #expect(Tokens.bare("summarize", trigger: "/") == "summarize")
        #expect(Tokens.bare("#tag", trigger: "#") == "tag")
    }

    @Test("A command is found at the start of any word, not inside one")
    func query() {
        #expect(Tokens.query(in: "/", trigger: "/")?.text == "")
        #expect(Tokens.query(in: "Please /sum", trigger: "/")?.text == "sum")
        #expect(Tokens.query(in: "and/or", trigger: "/") == nil)
        #expect(Tokens.query(in: "/summarize now", trigger: "/") == nil)
        #expect(Tokens.query(in: "Hello", trigger: "/") == nil)
    }

    @Test("Names that start with the query come first, then contained names, then descriptions and keywords")
    func shown() {
        #expect(
            Tokens.shown(commands, query: "", trigger: "/").map(\.id) == ["summarize", "rewrite", "search", "schedule"]
        )
        #expect(Tokens.shown(commands, query: "s", trigger: "/").map(\.id).first == "summarize")
        #expect(Tokens.shown(commands, query: "re", trigger: "/").map(\.id) == ["rewrite", "summarize"])
        #expect(Tokens.shown(commands, query: "recap", trigger: "/").map(\.id) == ["summarize"])
        #expect(Tokens.shown(commands, query: "zzz", trigger: "/").isEmpty)
    }

    @Test("Shown names carry the trigger once, and keep disabled state")
    func labels() {
        let shown = Tokens.shown(commands, query: "", trigger: "/")
        #expect(shown.map(\.label) == ["/summarize", "/rewrite", "/search", "/schedule"])
        #expect(shown.last?.isDisabled == true)
    }

    @Test("The chosen command replaces the typed query, or removes it")
    func replacement() throws {
        let text = "Please /sum"
        let query = try #require(Tokens.query(in: text, trigger: "/"))
        #expect(Tokens.text(after: query, in: text, replacement: "/summarize ") == "Please /summarize ")
        #expect(Tokens.text(after: query, in: text, replacement: nil) == "Please ")
    }

    @Test("Menu labels default to the command strings and name what was typed")
    func menuLabels() {
        let builtIn = Tokens.menuLabels(MonarchSlashMenuLabels(), typed: "/xyz")
        #expect(builtIn.label == "Commands")
        #expect(builtIn.empty?("") == "No commands match “/xyz”")
        #expect(builtIn.count?(1) == "1 command")
        #expect(builtIn.count?(3) == "3 commands")
        let custom = Tokens.menuLabels(
            MonarchSlashMenuLabels(label: "Befehle", empty: { "Nichts für \($0)" }, count: { "\($0) Befehle" }),
            typed: "/q"
        )
        #expect(custom.label == "Befehle")
        #expect(custom.empty?("ignored") == "Nichts für /q")
        #expect(custom.count?(2) == "2 Befehle")
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(!MonarchAiSlashCommandsStrings.label.hasPrefix("monarch."))
        #expect(!MonarchAiSlashCommandsStrings.empty("/a").hasPrefix("monarch."))
        #expect(!MonarchAiSlashCommandsStrings.count(1).hasPrefix("monarch."))
        #expect(!MonarchAiSlashCommandsStrings.count(5).hasPrefix("monarch."))
    }
}
