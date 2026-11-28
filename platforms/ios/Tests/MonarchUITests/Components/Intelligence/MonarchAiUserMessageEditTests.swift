import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiUserMessageEditFixtures: ComponentFixtures {
    static let id = "ai-user-message-edit"
    static let fixtures = [
        Fixture("a short prompt") {
            MonarchAiUserMessageEdit(
                "Summarise the open risks.",
                focusesOnAppear: false,
                onCancel: {},
                onSubmit: { _ in }
            )
        },
        Fixture("a long prompt, capped at three lines") {
            MonarchAiUserMessageEdit(
                String(repeating: "Compare the quarterly revenue with the forecast and list every gap. ", count: 8),
                maxLines: 3,
                focusesOnAppear: false,
                onCancel: {},
                onSubmit: { _ in }
            )
        },
        Fixture("empty, overridden strings") {
            MonarchAiUserMessageEdit(
                "",
                label: "Edit your question",
                branchHint: "Your first question is kept. You can switch back to it at any time from the message.",
                sendLabel: "Ask again",
                cancelLabel: "Keep the original",
                focusesOnAppear: false,
                onCancel: {},
                onSubmit: { _ in }
            )
        },
        Fixture("disabled") {
            MonarchAiUserMessageEdit("Waiting for the reply", focusesOnAppear: false, onCancel: {}, onSubmit: { _ in })
                .disabled(true)
        },
    ]
}

@Suite("MonarchAiUserMessageEdit")
struct MonarchAiUserMessageEditTests {
    typealias Tokens = MonarchAiUserMessageEditTokens

    @Test("An edited prompt is resent trimmed")
    func submission() {
        #expect(Tokens.submission(text: "  New question \n", original: "Old question") == "New question")
        #expect(Tokens.submission(text: "Line one\nLine two", original: "Line one") == "Line one\nLine two")
    }

    @Test("An empty or unchanged prompt cannot be resent")
    func noSubmission() {
        #expect(Tokens.submission(text: "", original: "Old question") == nil)
        #expect(Tokens.submission(text: " \n\t ", original: "Old question") == nil)
        #expect(Tokens.submission(text: "Old question", original: "Old question") == nil)
        #expect(Tokens.submission(text: "  Old question\n", original: "Old question  ") == nil)
        #expect(Tokens.submission(text: "", original: "") == nil)
    }

    @Test("The field grows from two lines to the limit, and a limit below two is raised")
    func lineLimit() {
        #expect(Tokens.lineLimit(maxLines: 10) == 2...10)
        #expect(Tokens.lineLimit(maxLines: 2) == 2...2)
        #expect(Tokens.lineLimit(maxLines: 1) == 2...2)
        #expect(Tokens.lineLimit(maxLines: -4) == 2...2)
    }

    @Test("The field uses the input roles, and the disabled roles when disabled")
    func colours() {
        #expect(Tokens.border(isEnabled: true) == \MonarchColorTokens.borderInput)
        #expect(Tokens.border(isEnabled: false) == \MonarchColorTokens.borderSubtle)
        #expect(Tokens.background(isEnabled: true) == \MonarchColorTokens.bgSurface)
        #expect(Tokens.background(isEnabled: false) == \MonarchColorTokens.bgSubtle)
        #expect(Tokens.text(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.text(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.hintColor == \MonarchColorTokens.textMuted)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiUserMessageEditStrings.label == "Edit message")
        #expect(
            MonarchAiUserMessageEditStrings.branchHint
                == "Editing will create a new branch. The original stays available."
        )
        #expect(MonarchAiUserMessageEditStrings.send == "Send")
        #expect(MonarchAiUserMessageEditStrings.cancel == "Cancel")
    }

    @Test("A long prompt makes the editor taller, up to the line limit")
    @MainActor
    func grows() {
        let long = String(repeating: "Compare the quarterly revenue with the forecast. ", count: 30)
        func height(_ text: String, maxLines: Int) -> CGFloat {
            Host.layout(
                MonarchAiUserMessageEdit(
                    text,
                    maxLines: maxLines,
                    focusesOnAppear: false,
                    onCancel: {},
                    onSubmit: { _ in }
                )
            ).height
        }
        let short = height("Hi", maxLines: 10)
        let capped = height(long, maxLines: 4)
        let tall = height(long, maxLines: 10)
        #expect(capped > short)
        #expect(tall > capped)
    }
}
