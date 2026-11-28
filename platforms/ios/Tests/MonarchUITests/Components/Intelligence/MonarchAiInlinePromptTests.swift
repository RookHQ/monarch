import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let paragraph = "Early numbers suggest more people finish setup on their first visit."
private let long = String(
    repeating: "A long selection that wraps across many lines at the largest text size. ",
    count: 5
)

enum MonarchAiInlinePromptFixtures: ComponentFixtures {
    static let id = "ai-inline-prompt"
    static let fixtures = [
        Fixture("with context and quick prompts") {
            MonarchAiInlinePrompt(context: paragraph, suggestions: ["Shorten", "Make it more formal", "Fix spelling"]) {
                _ in
            }
        },
        Fixture("without context, with an initial prompt") {
            MonarchAiInlinePrompt(initialValue: "Translate to Spanish") { _ in }
        },
        Fixture("pending") {
            MonarchAiInlinePrompt(context: paragraph, suggestions: ["Shorten"], isPending: true) { _ in }
        },
        Fixture("long context cut to an excerpt, custom labels") {
            MonarchAiInlinePrompt(
                context: long,
                closesOnSubmit: false,
                labels: MonarchAiInlinePromptLabels(
                    label: "Ask the assistant",
                    context: "Selected text",
                    cancel: "Close"
                )
            ) { _ in }
        },
        Fixture("context menu area, on and off") {
            VStack(alignment: .leading) {
                MonarchAiInlinePromptArea(
                    onAskAi: {},
                    content: {
                        Text(paragraph).monarchTextStyle(.body)
                    }
                )
                MonarchAiInlinePromptArea(
                    isEnabled: false,
                    label: "Rewrite",
                    onAskAi: {},
                    content: {
                        Text(long).monarchTextStyle(.body)
                    }
                )
            }
        },
        Fixture("text view: editable, read only and disabled") {
            VStack(alignment: .leading) {
                MonarchAiInlinePromptTextView("Draft", text: .constant(paragraph), onAskAi: { _ in })
                MonarchAiInlinePromptTextView("Summary", text: .constant(long), isEditable: false, onAskAi: { _ in })
                MonarchAiInlinePromptTextView("Locked", text: .constant(paragraph), onAskAi: { _ in })
                    .disabled(true)
                MonarchAiInlinePromptTextView("Empty", text: .constant(""), isEnabled: false, onAskAi: { _ in })
            }
        },
    ]
}

@Suite("MonarchAiInlinePrompt")
struct MonarchAiInlinePromptTests {
    private typealias Tokens = MonarchAiInlinePromptTokens

    @Test("The excerpt is trimmed, cut at 140 characters and left out when blank")
    func excerpt() {
        #expect(Tokens.excerpt(nil) == nil)
        #expect(Tokens.excerpt("  \n ") == nil)
        #expect(Tokens.excerpt("  Short  ") == "Short")
        let exact = String(repeating: "a", count: 140)
        #expect(Tokens.excerpt(exact) == exact)
        let cut = Tokens.excerpt(String(repeating: "a", count: 139) + " bcdef")
        #expect(cut == String(repeating: "a", count: 139) + "…")
        #expect(Tokens.excerpt("Hello world", limit: 6) == "Hello…")
        // Characters, not UTF-16 units: an emoji is not split.
        #expect(Tokens.excerpt("👋🏽👋🏽👋🏽", limit: 2) == "👋🏽👋🏽…")
    }

    @Test("Only a trimmed, non-blank prompt is sent, and never while pending")
    func submission() {
        #expect(Tokens.submission("  Shorten it \n", isPending: false) == "Shorten it")
        #expect(Tokens.submission("   ", isPending: false) == nil)
        #expect(Tokens.submission("", isPending: false) == nil)
        #expect(Tokens.submission("Shorten", isPending: true) == nil)
    }

    @Test("The selection is read in UTF-16 ranges and is nil for a caret")
    func selection() {
        let text = "Hi 👋🏽 there"
        #expect(Tokens.selection(in: text, location: 0, length: 2) == "Hi")
        #expect(Tokens.selection(in: text, location: 3, length: 4) == "👋🏽")
        #expect(Tokens.selection(in: text, location: 3, length: 0) == nil)
        #expect(Tokens.selection(in: text, location: 2, length: 1) == nil)
        #expect(Tokens.selection(in: text, location: 10, length: 40) == nil)
        #expect(Tokens.selection(in: text, location: -1, length: 2) == nil)
    }

    @Test("The menu item needs the item enabled and text selected")
    func menuItem() {
        #expect(Tokens.showsMenuItem(isEnabled: true, selection: "Hi"))
        #expect(!Tokens.showsMenuItem(isEnabled: false, selection: "Hi"))
        #expect(!Tokens.showsMenuItem(isEnabled: true, selection: nil))
    }

    @Test("The VoiceOver action passes the selection, else the whole text")
    func accessibilityAction() {
        #expect(Tokens.accessibilityActionText("All of it", selection: "it") == "it")
        #expect(Tokens.accessibilityActionText("All of it", selection: nil) == "All of it")
        #expect(Tokens.accessibilityActionText("  ", selection: nil) == nil)
    }

    @Test("The excerpt is read as Selection and the text")
    func excerptLabel() {
        #expect(Tokens.excerptAccessibilityLabel("Hello", labels: MonarchAiInlinePromptLabels()) == "Selection, Hello")
        #expect(
            Tokens.excerptAccessibilityLabel("Hello", labels: MonarchAiInlinePromptLabels(context: "Auswahl"))
                == "Auswahl, Hello"
        )
    }

    @Test("Colours come from the AI and text roles")
    func colours() {
        #expect(Tokens.excerptSurface.background == \.bgSubtle)
        #expect(Tokens.excerptSurface.border == \.borderDefault)
        #expect(Tokens.excerptLabelRole == \.aiStrong)
        #expect(Tokens.excerptTextRole == \.textMuted)
        #expect(Tokens.textRole(isEnabled: true) == \.textDefault)
        #expect(Tokens.textRole(isEnabled: false) == \.textDisabled)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let strings = [
            MonarchAiInlinePromptStrings.label, MonarchAiInlinePromptStrings.placeholder,
            MonarchAiInlinePromptStrings.inputLabel, MonarchAiInlinePromptStrings.submit,
            MonarchAiInlinePromptStrings.context, MonarchAiInlinePromptStrings.suggestions,
            MonarchAiInlinePromptStrings.pending, MonarchAiInlinePromptStrings.cancel,
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
        #expect(MonarchAiInlinePromptStrings.label == "Ask AI")
    }
}
