import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiFeedbackFixtures: ComponentFixtures {
    static let id = "ai-feedback"

    static let fixtures = [
        Fixture("unrated") {
            MonarchAiFeedback(selection: .constant(nil))
        },
        Fixture("thumbs up pressed") {
            MonarchAiFeedback(selection: .constant(.up))
        },
        Fixture("thumbs down pressed, disabled") {
            MonarchAiFeedback(selection: .constant(.down)).disabled(true)
        },
        Fixture("details sheet, thumbs down with reasons") {
            MonarchAiFeedbackForm(
                value: .down,
                options: MonarchAiFeedbackTokens.defaultReasons(for: .down),
                chosen: .constant(["inaccurate", "unsafe"]),
                comment: .constant(""),
                isPending: false,
                hasFailed: false,
                labels: MonarchAiFeedbackLabels(),
                onSubmit: {},
                onCancel: {}
            )
        },
        Fixture("details sheet, sending") {
            MonarchAiFeedbackForm(
                value: .down,
                options: MonarchAiFeedbackTokens.defaultReasons(for: .down),
                chosen: .constant(["incomplete"]),
                comment: .constant("It left out the totals for March."),
                isPending: true,
                hasFailed: false,
                labels: MonarchAiFeedbackLabels(),
                onSubmit: {},
                onCancel: {}
            )
        },
        Fixture("details sheet, thumbs up without reasons, failed") {
            MonarchAiFeedbackForm(
                value: .up,
                options: [],
                chosen: .constant([]),
                comment: .constant(
                    "A long comment that wraps over several lines so the field grows with its text up to its limit."
                ),
                isPending: false,
                hasFailed: true,
                labels: MonarchAiFeedbackLabels(),
                onSubmit: {},
                onCancel: {}
            )
        },
    ]
}

@Suite("MonarchAiFeedback")
struct MonarchAiFeedbackTests {
    private let labels = MonarchAiFeedbackLabels()

    @Test("Pressing a thumb sets the rating; pressing it again clears it")
    func rating() {
        #expect(MonarchAiFeedbackTokens.rating(after: .up, current: nil) == .up)
        #expect(MonarchAiFeedbackTokens.rating(after: .down, current: .up) == .down)
        #expect(MonarchAiFeedbackTokens.rating(after: .up, current: .up) == nil)
    }

    @Test("Details are asked for the ratings that asksDetails names")
    func details() {
        #expect(MonarchAiFeedbackTokens.next(for: .down, asks: .down) == .askDetails)
        #expect(MonarchAiFeedbackTokens.next(for: .up, asks: .down) == .send)
        #expect(MonarchAiFeedbackTokens.next(for: .up, asks: .both) == .askDetails)
        #expect(MonarchAiFeedbackTokens.next(for: .down, asks: .none) == .send)
        #expect(MonarchAiFeedbackTokens.next(for: nil, asks: .both) == .clear)
    }

    @Test("The sheet shows while details are asked for and sent from it")
    func sheet() {
        #expect(MonarchAiFeedbackTokens.showsSheet(.details, fromForm: false))
        #expect(MonarchAiFeedbackTokens.showsSheet(.pending, fromForm: true))
        #expect(!MonarchAiFeedbackTokens.showsSheet(.pending, fromForm: false))
        #expect(!MonarchAiFeedbackTokens.showsSheet(.idle, fromForm: true))
        #expect(!MonarchAiFeedbackTokens.showsSheet(.thanks, fromForm: true))
        #expect(MonarchAiFeedbackTokens.phaseAfterFailure(fromForm: true) == .details)
        #expect(MonarchAiFeedbackTokens.phaseAfterFailure(fromForm: false) == .idle)
    }

    @Test("The submission keeps listed order and trims the comment")
    func submission() {
        let options = MonarchAiFeedbackTokens.defaultReasons(for: .down)
        let submission = MonarchAiFeedbackTokens.submission(
            value: .down,
            chosen: ["unsafe", "inaccurate", "unknown"],
            options: options,
            comment: "  Wrong total.\n"
        )
        #expect(
            submission
                == MonarchAiFeedbackSubmission(
                    value: .down,
                    reasons: ["inaccurate", "unsafe"],
                    comment: "Wrong total."
                )
        )
        #expect(
            MonarchAiFeedbackTokens.submission(value: .up, chosen: [], options: [], comment: "   ")
                == MonarchAiFeedbackSubmission(value: .up)
        )
    }

    @Test("Five built-in reasons follow a thumbs down; none follow a thumbs up")
    func defaultReasons() {
        let down = MonarchAiFeedbackTokens.defaultReasons(for: .down)
        #expect(down.map(\.id) == ["inaccurate", "not-helpful", "incomplete", "hard-to-follow", "unsafe"])
        #expect(
            down.map(\.label) == ["Inaccurate", "Not helpful", "Incomplete", "Hard to follow", "Harmful or unsafe"]
        )
        #expect(MonarchAiFeedbackTokens.defaultReasons(for: .up).isEmpty)
    }

    @Test("The app's reasons replace the built-in ones; a rating without an entry has none")
    func options() {
        let custom: [MonarchAiMessageFeedback: [MonarchCheckboxGroupOption]] = [
            .up: [MonarchCheckboxGroupOption(id: "clear", label: "Clear")]
        ]
        #expect(MonarchAiFeedbackTokens.options(for: .up, reasons: custom).map(\.id) == ["clear"])
        #expect(MonarchAiFeedbackTokens.options(for: .down, reasons: custom).isEmpty)
        #expect(MonarchAiFeedbackTokens.options(for: .down, reasons: nil).count == 5)
    }

    @Test("A pressed thumb is filled, so the state is not colour alone")
    func symbols() {
        #expect(MonarchAiFeedbackTokens.symbol(.up, isPressed: false) == "hand.thumbsup")
        #expect(MonarchAiFeedbackTokens.symbol(.up, isPressed: true) == "hand.thumbsup.fill")
        #expect(MonarchAiFeedbackTokens.symbol(.down, isPressed: true) == "hand.thumbsdown.fill")
    }

    @Test("Thumbs and questions are named, and the names can be replaced")
    func names() {
        #expect(MonarchAiFeedbackTokens.thumbLabel(.up, labels: labels) == "Good response")
        #expect(MonarchAiFeedbackTokens.thumbLabel(.down, labels: labels) == "Bad response")
        #expect(MonarchAiFeedbackTokens.question(.down, labels: labels) == "What went wrong?")
        #expect(MonarchAiFeedbackTokens.question(.up, labels: labels) == "What went well?")
        let custom = MonarchAiFeedbackLabels(good: "Útil", reasonsUp: "¿Qué estuvo bien?")
        #expect(MonarchAiFeedbackTokens.thumbLabel(.up, labels: custom) == "Útil")
        #expect(MonarchAiFeedbackTokens.question(.up, labels: custom) == "¿Qué estuvo bien?")
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiFeedbackStrings.group == "Rate this response")
        #expect(MonarchAiFeedbackStrings.comment == "Tell us more (optional)")
        #expect(MonarchAiFeedbackStrings.commentPlaceholder == "What would have made it better?")
        #expect(MonarchAiFeedbackStrings.submit == "Send feedback")
        #expect(MonarchAiFeedbackStrings.cancel == "Cancel")
        #expect(MonarchAiFeedbackStrings.thanks == "Thanks for your feedback.")
        #expect(MonarchAiFeedbackStrings.error == "Your feedback could not be sent. Try again.")
    }

    #if os(iOS)
        @Test("The thumbs are at least 44 points high")
        @MainActor
        func hitTarget() {
            let size = Host.layout(MonarchAiFeedback(selection: .constant(nil)))
            #expect(size.height >= MonarchAccessibility.minimumHitTarget)
            #expect(size.width >= 2 * MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
