import Foundation
import MonarchTokens
import SwiftUI
import Testing
import os

@testable import MonarchUI

#if os(iOS)
    import UIKit
#endif

enum MonarchAiGhostTextFixtures: ComponentFixtures {
    static let id = "ai-ghost-text"

    @MainActor
    static func ready(_ suggestion: String, after text: String) -> MonarchAiCompletion {
        let completion = MonarchAiCompletion { _, _ in nil }
        completion.show(suggestion, after: text)
        return completion
    }

    static let fixtures = [
        Fixture("empty with prompt and helper") {
            MonarchAiGhostText(
                "Message",
                text: .constant(""),
                prompt: "Write a reply",
                helper: "Suggestions come from the model."
            ) { _, _ in nil }
        },
        Fixture("typed text, no suggestion") {
            MonarchAiGhostText("Message", text: .constant("Thanks for getting back to me.")) { _, _ in nil }
        },
        Fixture("suggestion showing") {
            MonarchAiGhostText(
                "Message",
                text: .constant("Thanks for"),
                completion: ready(" getting back to me so quickly.", after: "Thanks for"),
                helper: "Suggestions come from the model."
            )
        },
        Fixture("long text and a suggestion that wraps") {
            let text = String(repeating: "A long message that wraps onto several lines. ", count: 4)
            return MonarchAiGhostText(
                "Message",
                text: .constant(text),
                completion: ready(String(repeating: "And a long suggestion after it. ", count: 3), after: text),
                labels: MonarchAiGhostTextLabels(accept: "Take the suggestion")
            )
        },
        Fixture("without the accept button") {
            MonarchAiGhostText(
                "Message",
                text: .constant("Thanks for"),
                completion: ready(" the update.", after: "Thanks for"),
                showsAcceptButton: false
            )
        },
        Fixture("disabled") {
            MonarchAiGhostText(
                "Message",
                text: .constant("Read only"),
                completion: ready(" and no suggestion is drawn.", after: "Read only")
            )
            .disabled(true)
        },
    ]
}

/// Holds a request until the test lets it go.
private actor MonarchAiGhostTextGate {
    private var isOpen = false
    private var waiters: [CheckedContinuation<Void, Never>] = []

    func wait() async {
        if isOpen { return }
        await withCheckedContinuation { waiters.append($0) }
    }

    func open() {
        isOpen = true
        for waiter in waiters { waiter.resume() }
        waiters = []
    }
}

@Suite("MonarchAiGhostText")
@MainActor
struct MonarchAiGhostTextTests {
    private typealias Tokens = MonarchAiGhostTextTokens

    /// A completion with no debounce delay that records what it was asked.
    private func make(
        minimumCharacters: Int = 3,
        isEnabled: Bool = true,
        calls: OSAllocatedUnfairLock<[String]> = OSAllocatedUnfairLock(initialState: []),
        complete: @escaping @Sendable (String, Int) async throws -> String? = { _, _ in " world" }
    ) -> MonarchAiCompletion {
        MonarchAiCompletion(
            debounce: .milliseconds(300),
            minimumCharacters: minimumCharacters,
            isEnabled: isEnabled,
            sleep: { _ in },
            complete: { text, cursor in
                calls.withLock { $0.append(text) }
                return try await complete(text, cursor)
            }
        )
    }

    private func eventually(_ condition: () -> Bool) async -> Bool {
        for _ in 0..<3000 {
            if condition() { return true }
            try? await Task.sleep(for: .milliseconds(1))
        }
        return condition()
    }

    // MARK: The completion

    @Test("Typing asks for a continuation after the pause and shows it")
    func requestAndShow() async {
        let calls = OSAllocatedUnfairLock(initialState: [String]())
        let completion = make(calls: calls)
        #expect(completion.status == .idle)

        completion.update(text: "Hello", cursor: 5)
        #expect(completion.status == .waiting)
        #expect(completion.suggestion.isEmpty)
        await completion.settled()

        #expect(completion.status == .ready)
        #expect(completion.suggestion == " world")
        #expect(completion.text == "Hello")
        #expect(completion.cursor == 5)
        #expect(calls.withLock { $0 } == ["Hello"])

        // The same text and cursor again is a caret event: nothing is asked.
        completion.update(text: "Hello", cursor: 5)
        await completion.settled()
        #expect(calls.withLock { $0 }.count == 1)
    }

    @Test("Too little text before the cursor asks for nothing")
    func minimumCharacters() async {
        let calls = OSAllocatedUnfairLock(initialState: [String]())
        let completion = make(calls: calls)
        completion.update(text: "Hi", cursor: 2)
        completion.update(text: "  Hi  ", cursor: 6)
        // The white space around the text does not count, and neither does text after the cursor.
        completion.update(text: "Hi there", cursor: 2)
        await completion.settled()
        #expect(completion.status == .idle)
        #expect(calls.withLock { $0 }.isEmpty)

        completion.update(text: "Hi there", cursor: 8)
        await completion.settled()
        #expect(completion.status == .ready)

        let eager = make(minimumCharacters: 0, calls: calls)
        eager.update(text: "", cursor: 0)
        eager.update(text: "a", cursor: 1)
        await eager.settled()
        #expect(eager.status == .ready)
    }

    @Test("No continuation, or an empty one, shows nothing")
    func noSuggestion() async {
        for answer in [nil, ""] as [String?] {
            let completion = make { _, _ in answer }
            completion.update(text: "Hello", cursor: 5)
            await completion.settled()
            #expect(completion.status == .idle)
            #expect(completion.suggestion.isEmpty)
            #expect(completion.accept() == nil)
        }
    }

    @Test("A request that throws is an error and is reported; a cancelled one is not")
    func failure() async {
        struct Down: Error {}
        let completion = make { _, _ in throw Down() }
        var reported = 0
        completion.onError = { error in
            #expect(error is Down)
            reported += 1
        }
        completion.update(text: "Hello", cursor: 5)
        await completion.settled()
        #expect(completion.status == .error)
        #expect(completion.suggestion.isEmpty)
        #expect(reported == 1)

        let cancelled = make { _, _ in throw CancellationError() }
        cancelled.onError = { _ in reported += 1 }
        cancelled.update(text: "Hello", cursor: 5)
        await cancelled.settled()
        #expect(cancelled.status == .loading)
        #expect(reported == 1)
    }

    @Test("Typing through the suggestion keeps the rest of it without a new request")
    func typeThrough() async {
        let calls = OSAllocatedUnfairLock(initialState: [String]())
        let completion = make(calls: calls)
        completion.update(text: "Hello", cursor: 5)
        await completion.settled()

        completion.update(text: "Hello ", cursor: 6)
        #expect(completion.status == .ready)
        #expect(completion.suggestion == "world")
        completion.update(text: "Hello wor", cursor: 9)
        #expect(completion.suggestion == "ld")
        #expect(completion.text == "Hello wor")
        await completion.settled()
        #expect(calls.withLock { $0 } == ["Hello"])

        // Typing something else drops it and asks again.
        completion.update(text: "Hello wox", cursor: 9)
        #expect(completion.status == .waiting)
        #expect(completion.suggestion.isEmpty)
        await completion.settled()
        #expect(calls.withLock { $0 } == ["Hello", "Hello wox"])

        // Typing the whole suggestion leaves nothing to keep, so it asks again.
        let whole = make(calls: calls)
        whole.update(text: "Hello", cursor: 5)
        await whole.settled()
        whole.update(text: "Hello world", cursor: 11)
        #expect(whole.status == .waiting)
        await whole.settled()
    }

    @Test("Only the answer to the latest text is shown")
    func staleAnswers() async {
        let calls = OSAllocatedUnfairLock(initialState: [String]())
        let gate = MonarchAiGhostTextGate()
        let completion = make(calls: calls) { text, _ in
            if text == "First" {
                await gate.wait()
                return " stale"
            }
            return " fresh"
        }
        // A newer edit during the pause: the first request is never sent.
        completion.update(text: "Zero", cursor: 4)
        completion.update(text: "First", cursor: 5)
        #expect(await eventually { completion.status == .loading && calls.withLock { $0 } == ["First"] })

        // A newer edit while the request is in flight: its answer is ignored.
        completion.update(text: "Second", cursor: 6)
        await gate.open()
        await completion.settled()
        try? await Task.sleep(for: .milliseconds(20))
        #expect(completion.status == .ready)
        #expect(completion.suggestion == " fresh")
        #expect(completion.text == "Second")
    }

    @Test("A dismissed suggestion stays away until the text changes")
    func dismiss() async {
        let calls = OSAllocatedUnfairLock(initialState: [String]())
        let completion = make(calls: calls)
        completion.update(text: "Hello", cursor: 5)
        await completion.settled()
        completion.dismiss()
        #expect(completion.status == .idle)
        #expect(completion.suggestion.isEmpty)
        #expect(completion.text == "Hello")

        completion.update(text: "Hello", cursor: 5)
        await completion.settled()
        #expect(completion.status == .idle)
        #expect(calls.withLock { $0 }.count == 1)

        completion.update(text: "Hello!", cursor: 6)
        await completion.settled()
        #expect(completion.status == .ready)

        // Dismissing while the request waits cancels it.
        completion.update(text: "Hello again", cursor: 11)
        completion.dismiss()
        await completion.settled()
        #expect(completion.status == .idle)
        #expect(calls.withLock { $0 }.count == 2)
    }

    @Test("Accepting inserts the suggestion at the cursor")
    func accept() async {
        let completion = make { _, _ in ", dear" }
        completion.update(text: "Hello", cursor: 5)
        await completion.settled()
        let acceptance = completion.accept()
        #expect(acceptance == MonarchAiCompletionAcceptance(inserted: ", dear", text: "Hello, dear", cursor: 11))
        #expect(completion.status == .idle)
        #expect(completion.suggestion.isEmpty)
        #expect(completion.text == "Hello, dear")
        #expect(completion.cursor == 11)
        #expect(completion.accept() == nil)
        #expect(completion.acceptWord() == nil)

        // The field echoes the insertion: nothing is asked for it.
        completion.update(text: "Hello, dear", cursor: 11)
        #expect(completion.status == .idle)
    }

    @Test("A suggestion in the middle of the text goes in at the cursor, which is a UTF-16 offset")
    func acceptInTheMiddle() async {
        let completion = make { _, _ in "," }
        completion.update(text: "Hello world", cursor: 5)
        await completion.settled()
        #expect(completion.accept() == MonarchAiCompletionAcceptance(inserted: ",", text: "Hello, world", cursor: 6))

        let emoji = make { _, _ in " 🎉 done" }
        let text = "Hi 👋 all"
        let cursor = ("Hi 👋" as NSString).length
        #expect(cursor == 5)
        emoji.update(text: text, cursor: cursor)
        await emoji.settled()
        let acceptance = emoji.accept()
        #expect(acceptance?.text == "Hi 👋 🎉 done all")
        #expect(acceptance?.cursor == ("Hi 👋 🎉 done" as NSString).length)

        // A cursor outside the text is kept inside it.
        let clamped = make()
        clamped.update(text: "Hello", cursor: 99)
        #expect(clamped.cursor == 5)
        clamped.update(text: "Hello", cursor: -4)
        #expect(clamped.cursor == 0)
    }

    @Test("Accepting word by word keeps the rest showing")
    func acceptWord() async {
        let completion = make { _, _ in " quick brown fox" }
        completion.update(text: "The", cursor: 3)
        await completion.settled()

        #expect(
            completion.acceptWord() == MonarchAiCompletionAcceptance(inserted: " quick", text: "The quick", cursor: 9)
        )
        #expect(completion.status == .ready)
        #expect(completion.suggestion == " brown fox")
        #expect(completion.acceptWord()?.inserted == " brown")
        #expect(completion.acceptWord()?.text == "The quick brown fox")
        #expect(completion.status == .idle)
    }

    @Test(
        "The next word is the leading space, the word and the punctuation attached to it",
        arguments: [
            ("", ""),
            (" hello world", " hello"),
            ("hello, world", "hello,"),
            ("(hello) there", "(hello)"),
            ("don't stop", "don't"),
            ("foo-bar baz", "foo-"),
            ("   ", "   "),
            ("... and then", "..."),
            ("\n\nNext paragraph", "\n\nNext"),
            ("word", "word"),
        ]
    )
    func nextWord(suggestion: String, word: String) {
        #expect(MonarchAiCompletion.nextWord(of: suggestion) == word)
    }

    @Test("Text without spaces is split at the system's word boundaries")
    func nextWordWithoutSpaces() {
        let suggestion = "今日は天気がいいですね"
        let word = MonarchAiCompletion.nextWord(of: suggestion)
        #expect(!word.isEmpty)
        #expect(suggestion.hasPrefix(word))
        #expect(word.count < suggestion.count)
    }

    @Test("Nothing is asked or shown while an input method composes, or while suggestions are off")
    func composingAndDisabled() async {
        let calls = OSAllocatedUnfairLock(initialState: [String]())
        let completion = make(calls: calls)
        completion.update(text: "Hello", cursor: 5)
        await completion.settled()
        #expect(completion.status == .ready)

        completion.setComposing(true)
        #expect(completion.status == .idle)
        completion.update(text: "Helloに", cursor: 6)
        await completion.settled()
        #expect(completion.status == .idle)
        #expect(completion.text == "Helloに")
        #expect(calls.withLock { $0 }.count == 1)

        completion.setComposing(false)
        completion.update(text: "Hello日本", cursor: 7)
        await completion.settled()
        #expect(completion.status == .ready)

        completion.setEnabled(false)
        #expect(!completion.isEnabled)
        #expect(completion.status == .idle)
        completion.update(text: "Hello again", cursor: 11)
        await completion.settled()
        #expect(completion.status == .idle)
        #expect(calls.withLock { $0 }.count == 2)

        completion.setEnabled(true)
        completion.update(text: "Hello again!", cursor: 12)
        await completion.settled()
        #expect(completion.status == .ready)

        let off = make(isEnabled: false, calls: calls)
        off.update(text: "Hello", cursor: 5)
        await off.settled()
        #expect(off.status == .idle)
    }

    // MARK: The field's rules

    @Test("A suggestion belongs only at the end of the text, with a caret, outside composition")
    func evaluate() {
        #expect(Tokens.evaluate(text: "Hello", caret: 5, isComposing: false, isEditable: true) == .update(cursor: 5))
        #expect(
            Tokens.evaluate(text: "Hello  \n", caret: 5, isComposing: false, isEditable: true) == .update(cursor: 5)
        )
        #expect(Tokens.evaluate(text: "", caret: 0, isComposing: false, isEditable: true) == .update(cursor: 0))
        #expect(Tokens.evaluate(text: "Hello world", caret: 5, isComposing: false, isEditable: true) == .dismiss)
        #expect(Tokens.evaluate(text: "Hello", caret: nil, isComposing: false, isEditable: true) == .dismiss)
        #expect(Tokens.evaluate(text: "Hello", caret: 5, isComposing: true, isEditable: true) == .dismiss)
        #expect(Tokens.evaluate(text: "Hello", caret: 5, isComposing: false, isEditable: false) == .dismiss)
        #expect(Tokens.evaluate(text: "Hello", caret: 99, isComposing: false, isEditable: true) == .update(cursor: 5))
    }

    @Test("Tab and the forward arrow accept, Control or Option with the arrow accept a word, Escape dismisses")
    func keys() {
        #expect(Tokens.keyAction(.tab, modifiers: [], isRightToLeft: false) == .accept(.all))
        #expect(Tokens.keyAction(.tab, modifiers: .shift, isRightToLeft: false) == nil)
        #expect(Tokens.keyAction(.escape, modifiers: [], isRightToLeft: false) == .dismiss)
        #expect(Tokens.keyAction(.escape, modifiers: .command, isRightToLeft: false) == nil)
        #expect(Tokens.keyAction(.rightArrow, modifiers: [], isRightToLeft: false) == .accept(.all))
        #expect(Tokens.keyAction(.rightArrow, modifiers: .control, isRightToLeft: false) == .accept(.word))
        #expect(Tokens.keyAction(.rightArrow, modifiers: .option, isRightToLeft: false) == .accept(.word))
        #expect(Tokens.keyAction(.rightArrow, modifiers: .command, isRightToLeft: false) == nil)
        #expect(Tokens.keyAction(.rightArrow, modifiers: [.option, .shift], isRightToLeft: false) == nil)
        #expect(Tokens.keyAction(.leftArrow, modifiers: [], isRightToLeft: false) == nil)

        // In a right-to-left layout the forward arrow is the left one.
        #expect(Tokens.keyAction(.leftArrow, modifiers: [], isRightToLeft: true) == .accept(.all))
        #expect(Tokens.keyAction(.leftArrow, modifiers: .option, isRightToLeft: true) == .accept(.word))
        #expect(Tokens.keyAction(.rightArrow, modifiers: [], isRightToLeft: true) == nil)

        // Every key the field takes from the system does something.
        for isRightToLeft in [false, true] {
            let handled = Tokens.handledKeys(isRightToLeft: isRightToLeft)
            #expect(handled.count == 5)
            for entry in handled {
                #expect(Tokens.keyAction(entry.key, modifiers: entry.modifiers, isRightToLeft: isRightToLeft) != nil)
            }
        }
    }

    @Test("Only a ready suggestion in an enabled field is drawn, in the muted colour")
    func shown() {
        #expect(Tokens.shownSuggestion(status: .ready, suggestion: " world", isEnabled: true) == " world")
        #expect(Tokens.shownSuggestion(status: .ready, suggestion: " world", isEnabled: false).isEmpty)
        for status in MonarchAiCompletionStatus.allCases where status != .ready {
            #expect(Tokens.shownSuggestion(status: status, suggestion: " world", isEnabled: true).isEmpty)
        }
        #expect(Tokens.suggestionRole == \MonarchColorTokens.textMuted)
        #expect(Tokens.textRole(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.textRole(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.suggestionRole != Tokens.textRole(isEnabled: true))
    }

    @Test("A new suggestion is announced; the rest of one being typed through is not")
    func announcements() {
        #expect(Tokens.shouldAnnounce(suggestion: " world", lastAnnounced: ""))
        #expect(!Tokens.shouldAnnounce(suggestion: "world", lastAnnounced: " world"))
        #expect(!Tokens.shouldAnnounce(suggestion: "ld", lastAnnounced: " world"))
        #expect(Tokens.shouldAnnounce(suggestion: " there", lastAnnounced: " world"))
        #expect(!Tokens.shouldAnnounce(suggestion: "", lastAnnounced: " world"))
    }

    @Test("VoiceOver reads how to accept while a suggestion shows, otherwise the helper text")
    func accessibilityHint() {
        #expect(Tokens.accessibilityHint(helper: "Help", instruction: "Tap it.", isShowing: false) == "Help")
        #expect(Tokens.accessibilityHint(helper: nil, instruction: "Tap it.", isShowing: false).isEmpty)
        #expect(Tokens.accessibilityHint(helper: "Help", instruction: "Tap it.", isShowing: true) == "Tap it.")
    }

    @Test("After accepting, the caret goes to the completion's cursor; after another change, to the end")
    func caret() {
        #expect(Tokens.caret(for: "Hello, world", completionText: "Hello, world", completionCursor: 6) == 6)
        #expect(Tokens.caret(for: "Other text", completionText: "Hello, world", completionCursor: 6) == 10)
        #expect(Tokens.caret(for: "Hi", completionText: "Hi", completionCursor: 40) == 2)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(
            MonarchAiGhostTextStrings.announce(" world") == "Suggestion:  world. Use Accept suggestion to take it."
        )
        #expect(MonarchAiGhostTextStrings.instruction.hasPrefix("A suggestion is showing after the cursor."))
        #expect(MonarchAiGhostTextStrings.accept == "Accept suggestion")
        #expect(MonarchAiGhostTextStrings.dismiss == "Dismiss suggestion")
        #expect(MonarchAiGhostTextStrings.accepted == "Suggestion accepted.")
        #expect(MonarchAiGhostTextStrings.dismissed == "Suggestion dismissed.")
    }

    @Test("The field is at least as tall as the touch target, and the accept button keeps its room")
    func layout() {
        let bare = Host.layout(
            MonarchAiGhostText("Message", text: .constant(""), showsAcceptButton: false) { _, _ in nil }
        )
        #expect(bare.height >= MonarchAccessibility.minimumHitTarget)
        let idle = Host.layout(MonarchAiGhostText("Message", text: .constant("")) { _, _ in nil })
        #expect(idle.height >= bare.height + MonarchAccessibility.minimumHitTarget)
        // Nothing moves when a suggestion appears.
        let showing = Host.layout(
            MonarchAiGhostText(
                "Message",
                text: .constant(""),
                completion: MonarchAiGhostTextFixtures.ready(" world", after: "")
            )
        )
        #expect(showing.height == idle.height)
    }

    // MARK: The text view (iOS only; these run on the simulator)

    #if os(iOS)
        private func style(isRightToLeft: Bool = false, isEnabled: Bool = true) -> MonarchAiGhostTextFieldStyle {
            MonarchAiGhostTextFieldStyle(
                fontSize: 16,
                fontFamily: nil,
                lineSpacing: 0,
                textColor: Color(monarch: MonarchTokens.light.colors.textDefault),
                suggestionColor: Color(monarch: MonarchTokens.light.colors.textMuted),
                isRightToLeft: isRightToLeft,
                isEnabled: isEnabled,
                label: "Message",
                hint: "Hint",
                acceptActionName: "Accept suggestion",
                dismissActionName: "Dismiss suggestion",
                caretAfterExternalChange: 0
            )
        }

        private func textView(_ text: String, isRightToLeft: Bool = false) -> MonarchAiGhostTextView {
            let view = MonarchAiGhostTextView()
            view.frame = CGRect(x: 0, y: 0, width: 300, height: 120)
            view.apply(style(isRightToLeft: isRightToLeft))
            view.setText(text, caret: (text as NSString).length)
            view.layoutIfNeeded()
            return view
        }

        @Test("The text view is described for VoiceOver and offers its actions only while a suggestion shows")
        func textViewAccessibility() {
            let view = textView("Hello")
            #expect(view.accessibilityLabel == "Message")
            #expect(view.accessibilityHint == "Hint")
            #expect(view.accessibilityCustomActions == nil)

            var accepted: [MonarchAiGhostTextAcceptKind] = []
            var dismissed = 0
            view.onAccept = { accepted.append($0) }
            view.onDismiss = { dismissed += 1 }
            view.suggestion = " world"
            let actions = view.accessibilityCustomActions ?? []
            #expect(actions.map(\.name) == ["Accept suggestion", "Dismiss suggestion"])
            _ = actions.first?.actionHandler?(actions[0])
            _ = actions.last?.actionHandler?(actions[1])
            #expect(accepted == [.all])
            #expect(dismissed == 1)

            view.suggestion = ""
            #expect(view.accessibilityCustomActions == nil)
        }

        @Test("The text view takes Tab, Escape and the forward arrow only while a suggestion shows")
        func textViewKeys() {
            let view = textView("Hello")
            let before = view.keyCommands ?? []
            #expect(!before.contains { $0.input == "\t" })

            view.suggestion = " world"
            let during = view.keyCommands ?? []
            #expect(during.count == before.count + 5)
            #expect(during.contains { $0.input == "\t" && $0.modifierFlags.isEmpty })
            #expect(during.contains { $0.input == UIKeyCommand.inputEscape })
            #expect(during.contains { $0.input == UIKeyCommand.inputRightArrow && $0.modifierFlags == .alternate })
            #expect(!during.contains { $0.input == UIKeyCommand.inputLeftArrow })

            let mirrored = textView("שלום", isRightToLeft: true)
            mirrored.suggestion = " עולם"
            let commands = mirrored.keyCommands ?? []
            #expect(commands.contains { $0.input == UIKeyCommand.inputLeftArrow })
            #expect(!commands.contains { $0.input == UIKeyCommand.inputRightArrow })
        }

        @Test("Setting the text from outside places the caret, and the system's inline predictions are off")
        func textViewText() {
            let view = textView("Hello")
            #expect(view.selectedRange == NSRange(location: 5, length: 0))
            view.setText("Hello, world", caret: 6)
            #expect(view.text == "Hello, world")
            #expect(view.selectedRange == NSRange(location: 6, length: 0))
            view.setText("Hi", caret: 40)
            #expect(view.selectedRange == NSRange(location: 2, length: 0))
            #expect(view.inlinePredictionType == .no)
            #expect(!view.isScrollEnabled)

            view.apply(style(isEnabled: false))
            #expect(!view.isEditable)
        }

        @Test("A tap accepts only on the suggestion")
        func textViewTap() {
            let view = textView("Hello")
            #expect(!view.isOnSuggestion(CGPoint(x: 4, y: 4)))
            view.suggestion = " world"
            view.layoutIfNeeded()
            // Far from any text, and on the typed text, a tap is the text view's own.
            #expect(!view.isOnSuggestion(CGPoint(x: 290, y: 110)))
            #expect(!view.isOnSuggestion(CGPoint(x: 2, y: 8)))
            // Just after the end of the typed text is the suggestion.
            let end = view.caretRect(for: view.endOfDocument)
            #expect(view.isOnSuggestion(CGPoint(x: end.maxX + 6, y: end.midY)))
        }
    #endif
}
