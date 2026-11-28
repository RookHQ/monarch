import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiMemoryChipFixtures: ComponentFixtures {
    static let id = "ai-memory-chip"
    static let fixtures = [
        Fixture("chip: saved, with scope, edit and forget") {
            MonarchAiMemoryChip(
                MonarchAiMemoryPart(id: "m1", action: .write, text: "Prefers summaries under 200 words", scope: "Ada"),
                onEdit: { _, _ in },
                onForget: { _ in }
            )
        },
        Fixture("chip: used, and forgotten without commands") {
            VStack(alignment: .leading) {
                MonarchAiMemoryChip(MonarchAiMemoryPart(id: "m2", action: .read, text: "Works in Lisbon"))
                MonarchAiMemoryChip(
                    MonarchAiMemoryPart(id: "m3", action: .forget, text: "Has a dog called Pixel"),
                    onForget: { _ in }
                )
            }
        },
        Fixture("row: scope shown on request, forget only") {
            MonarchAiMemoryChip(
                MonarchAiMemoryPart(id: "m4", action: .write, text: "Writes in British English", scope: "Workspace"),
                variant: .row,
                showsScope: true,
                onForget: { _ in }
            )
        },
        Fixture("long text and own labels") {
            MonarchAiMemoryChip(
                MonarchAiMemoryPart(
                    id: "m5",
                    action: .write,
                    text: "Prefers that every weekly report starts with the open risks, then the decisions taken, "
                        + "then a short list of next steps with owners and dates"
                ),
                labels: MonarchAiMemoryChipLabels(prefix: { _ in "Saved:" }),
                onEdit: { _, _ in }
            )
        },
    ]
}

@Suite("MonarchAiMemoryChip")
struct MonarchAiMemoryChipTests {
    typealias Tokens = MonarchAiMemoryChipTokens

    @Test("The chip uses the AI roles; the row has no fill or border")
    func appearance() {
        let chip = Tokens.appearance(variant: .chip, action: .write)
        #expect(chip.background == \.aiSubtle)
        #expect(chip.border == \.aiBorder)
        #expect(chip.glyph == \.aiStrong)
        #expect(chip.text == \.textDefault)
        let row = Tokens.appearance(variant: .row, action: .read)
        #expect(row.background == nil)
        #expect(row.border == nil)
        #expect(Tokens.appearance(variant: .chip, action: .forget).text == \.textMuted)
    }

    @Test("The scope shows by default on the chip, not on the row, and never when empty")
    func scope() {
        #expect(Tokens.showsScope(nil, variant: .chip, scope: "Ada"))
        #expect(!Tokens.showsScope(nil, variant: .row, scope: "Ada"))
        #expect(Tokens.showsScope(true, variant: .row, scope: "Ada"))
        #expect(!Tokens.showsScope(false, variant: .chip, scope: "Ada"))
        #expect(!Tokens.showsScope(true, variant: .chip, scope: " "))
        #expect(!Tokens.showsScope(true, variant: .chip, scope: nil))
    }

    @Test("Commands appear with a callback, and never for a forgotten memory")
    func commands() {
        #expect(Tokens.offersCommands(action: .write, hasEdit: true, hasForget: false))
        #expect(Tokens.offersCommands(action: .read, hasEdit: false, hasForget: true))
        #expect(!Tokens.offersCommands(action: .write, hasEdit: false, hasForget: false))
        #expect(!Tokens.offersCommands(action: .forget, hasEdit: true, hasForget: true))
    }

    @Test("An edit is saved trimmed, and never when empty or unchanged")
    func edited() {
        #expect(Tokens.edited("  New text \n", original: "Old") == "New text")
        #expect(Tokens.edited("   ", original: "Old") == nil)
        #expect(Tokens.edited("Old ", original: "Old") == nil)
    }

    @Test("Spoken names use a shortened copy of the text")
    func shortText() {
        #expect(Tokens.shortText("Short  text\nhere") == "Short text here")
        let long = String(repeating: "word ", count: 20)
        let short = Tokens.shortText(long)
        #expect(short.count <= Tokens.shortTextLength)
        #expect(short.hasSuffix("…"))
        #expect(MonarchAiMemoryChipStrings.options(Tokens.shortText("Likes tea")) == "Options for memory: Likes tea")
    }

    @Test("The element reads the prefix, the text and the scope")
    func accessibilityLabel() {
        #expect(
            Tokens.accessibilityLabel(prefix: "Remembered:", text: "Likes tea", scope: "Ada")
                == "Remembered: Likes tea, Ada"
        )
        #expect(Tokens.accessibilityLabel(prefix: nil, text: "Likes tea", scope: nil) == "Likes tea")
    }

    @Test("The prefix follows the memory action")
    func prefixes() {
        let labels = MonarchAiMemoryChipLabels()
        #expect(labels.prefix(.read) == "Used memory:")
        #expect(labels.prefix(.write) == "Remembered:")
        #expect(labels.prefix(.forget) == "Forgotten:")
    }

    @Test("Own labels replace the built-in strings")
    func overrides() {
        let labels = MonarchAiMemoryChipLabels(
            prefix: { _ in "P" },
            options: { "O " + $0 },
            edit: "E",
            forget: "F",
            editTitle: "ET",
            editField: "EF",
            save: "S",
            cancel: "C"
        )
        #expect(labels.prefix(.write) == "P")
        #expect(labels.options("x") == "O x")
        #expect(
            [labels.edit, labels.forget, labels.editTitle, labels.editField, labels.save, labels.cancel] == [
                "E", "F", "ET", "EF", "S", "C",
            ]
        )
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        #expect(MonarchAiMemoryChipStrings.edit == "Edit")
        #expect(MonarchAiMemoryChipStrings.forget == "Forget")
        #expect(MonarchAiMemoryChipStrings.editTitle == "Edit memory")
        #expect(MonarchAiMemoryChipStrings.editField == "Memory")
        #expect(MonarchAiMemoryChipStrings.save == "Save")
        #expect(MonarchAiMemoryChipStrings.cancel == "Cancel")
    }
}
