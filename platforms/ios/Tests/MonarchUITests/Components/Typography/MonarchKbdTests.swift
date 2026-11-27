import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchKbdFixtures: ComponentFixtures {
    static let id = "kbd"
    static let fixtures = [
        Fixture("single keys and shortcuts") {
            HStack {
                MonarchKbd("K", isAlwaysVisible: true)
                MonarchKbd("⌘K", isAlwaysVisible: true)
                MonarchKbd("⇧⌘P", isAlwaysVisible: true)
                MonarchKbd("Esc", isAlwaysVisible: true)
                MonarchKbd("↩", isAlwaysVisible: true)
            }
        },
        Fixture("beside a label") {
            HStack {
                Text("Search").monarchTextStyle(.body)
                Spacer()
                MonarchKbd("⌘F", isAlwaysVisible: true)
            }
        },
        Fixture("long text") {
            MonarchKbd("Control + Option + Command + Space", isAlwaysVisible: true)
        },
        Fixture("with translated key names") {
            MonarchKbd("⌘⌫", labels: MonarchKbdLabels(command: "Befehl", delete: "Rückschritt"), isAlwaysVisible: true)
        },
        Fixture("shown only with a hardware keyboard, beside text that is always there") {
            HStack {
                Text("Save").monarchTextStyle(.body)
                MonarchKbd("⌘S")
            }
        },
    ]
}

@Suite("MonarchKbd")
struct MonarchKbdTests {
    typealias Tokens = MonarchKbdTokens

    private let labels = MonarchKbdLabels()

    @Test("The cap is drawn with a hardware keyboard, or always when asked")
    func visibility() {
        #expect(!Tokens.isVisible(isAlwaysVisible: false, isKeyboardAttached: false))
        #expect(Tokens.isVisible(isAlwaysVisible: false, isKeyboardAttached: true))
        #expect(Tokens.isVisible(isAlwaysVisible: true, isKeyboardAttached: false))
        #expect(Tokens.isVisible(isAlwaysVisible: true, isKeyboardAttached: true))
    }

    @Test("The cap is a subtle fill with a border and muted text")
    func appearance() {
        #expect(Tokens.appearance.background == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.appearance.foreground == \MonarchColorTokens.textMuted)
        #expect(Tokens.appearance.border == \MonarchColorTokens.borderDefault)
        #expect(Tokens.bottomEdgeWidth > MonarchShapes.borderWidth)
    }

    @Test("Key glyphs are read by name and other characters as they are")
    func spokenText() {
        #expect(Tokens.spokenText(for: "⌘K", labels: labels) == "Command K")
        #expect(Tokens.spokenText(for: "⇧⌘P", labels: labels) == "Shift Command P")
        #expect(Tokens.spokenText(for: "⌃⌥⌫", labels: labels) == "Control Option Delete")
        #expect(Tokens.spokenText(for: "↩", labels: labels) == "Return")
        #expect(Tokens.spokenText(for: "⏎", labels: labels) == "Return")
        #expect(Tokens.spokenText(for: "⎋", labels: labels) == "Escape")
        #expect(Tokens.spokenText(for: "⇥", labels: labels) == "Tab")
        #expect(Tokens.spokenText(for: "␣", labels: labels) == "Space")
        #expect(Tokens.spokenText(for: "⇪", labels: labels) == "Caps Lock")
        #expect(Tokens.spokenText(for: "⌥↑", labels: labels) == "Option Up Arrow")
        #expect(Tokens.spokenText(for: "↓←→", labels: labels) == "Down Arrow Left Arrow Right Arrow")
    }

    @Test("Text without glyphs is unchanged, and white space does not add pauses")
    func spokenTextWithoutGlyphs() {
        #expect(Tokens.spokenText(for: "Esc", labels: labels) == "Esc")
        #expect(Tokens.spokenText(for: "Ctrl+K", labels: labels) == "Ctrl+K")
        #expect(Tokens.spokenText(for: "F12", labels: labels) == "F12")
        #expect(Tokens.spokenText(for: "⌘ K", labels: labels) == "Command K")
        #expect(Tokens.spokenText(for: " ⌘ ", labels: labels) == "Command")
        #expect(Tokens.spokenText(for: "", labels: labels).isEmpty)
    }

    @Test("Every key name can be replaced")
    func overrides() {
        let german = MonarchKbdLabels(
            command: "Befehl",
            shift: "Umschalt",
            option: "Wahl",
            control: "Steuerung",
            return: "Eingabe",
            escape: "Abbrechen",
            delete: "Rückschritt",
            tab: "Tabulator",
            space: "Leertaste",
            capsLock: "Feststell",
            upArrow: "Hoch",
            downArrow: "Runter",
            leftArrow: "Links",
            rightArrow: "Rechts"
        )
        #expect(
            Tokens.spokenText(for: "⌘⇧⌥⌃↩⎋⌫⇥␣⇪↑↓←→", labels: german)
                == "Befehl Umschalt Wahl Steuerung Eingabe Abbrechen Rückschritt Tabulator Leertaste Feststell "
                + "Hoch Runter Links Rechts"
        )
        #expect(Tokens.spokenText(for: "⌘K", labels: MonarchKbdLabels(command: "Cmd")) == "Cmd K")
    }

    @Test("The built-in key names resolve and are all different")
    func strings() {
        let names = [
            MonarchKbdStrings.command, MonarchKbdStrings.shift, MonarchKbdStrings.option, MonarchKbdStrings.control,
            MonarchKbdStrings.return, MonarchKbdStrings.escape, MonarchKbdStrings.delete, MonarchKbdStrings.tab,
            MonarchKbdStrings.space, MonarchKbdStrings.capsLock, MonarchKbdStrings.upArrow,
            MonarchKbdStrings.downArrow, MonarchKbdStrings.leftArrow, MonarchKbdStrings.rightArrow,
        ]
        for name in names {
            #expect(!name.hasPrefix("monarch."))
            #expect(!name.isEmpty)
        }
        #expect(Set(names).count == names.count)
    }

    @MainActor
    @Test("A cap that is always visible takes room; a longer shortcut takes no less height")
    func layout() {
        let single = Host.layout(MonarchKbd("K", isAlwaysVisible: true))
        #expect(single.height >= MonarchKbdTokens.minimumSize)
        let long = Host.layout(MonarchKbd("Control + Option + Command + Space", isAlwaysVisible: true))
        #expect(long.height >= single.height)
    }
}
