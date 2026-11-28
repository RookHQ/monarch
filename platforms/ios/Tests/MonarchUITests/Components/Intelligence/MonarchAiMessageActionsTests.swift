import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiMessageActionsFixtures: ComponentFixtures {
    static let id = "ai-message-actions"

    static let text = "Paris is the capital of France."

    static let fixtures: [Fixture] = [
        Fixture("every action, rated up") {
            MonarchAiMessageActions(
                text,
                feedback: .constant(.up),
                isReadingAloud: .constant(false),
                isShareable: true,
                onRegenerate: {},
                onEdit: {},
                more: { Button("Report", systemImage: "flag") {} }
            )
        },
        Fixture("rated down, reading aloud") {
            MonarchAiMessageActions(text, feedback: .constant(.down), isReadingAloud: .constant(true), onRegenerate: {})
        },
        Fixture("copy only") {
            MonarchAiMessageActions(text)
        },
        Fixture("own message: copy and edit") {
            MonarchAiMessageActions(text, onEdit: {})
        },
        Fixture("narrow width moves actions into the menu") {
            MonarchAiMessageActions(
                text,
                feedback: .constant(nil),
                isReadingAloud: .constant(false),
                isShareable: true,
                onRegenerate: {},
                onEdit: {}
            )
            .frame(width: 200, alignment: .leading)
        },
        Fixture("custom labels, disabled") {
            MonarchAiMessageActions(
                text,
                feedback: .constant(nil),
                labels: MonarchAiMessageActionsLabels(toolbar: "Answer tools", copy: "Copy answer"),
                onRegenerate: {}
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchAiMessageActions")
struct MonarchAiMessageActionsTests {
    private typealias Tokens = MonarchAiMessageActionsTokens

    @Test("An action appears only when it is turned on, in the web order")
    func actions() {
        #expect(
            Tokens.actions(
                isCopyable: true,
                hasRegenerate: true,
                hasEdit: true,
                hasFeedback: true,
                isShareable: true,
                hasReadAloud: true
            ) == [.copy, .regenerate, .edit, .good, .bad, .share, .readAloud]
        )
        #expect(
            Tokens.actions(
                isCopyable: false,
                hasRegenerate: false,
                hasEdit: true,
                hasFeedback: false,
                isShareable: false,
                hasReadAloud: false
            ) == [.edit]
        )
        #expect(
            Tokens.actions(
                isCopyable: false,
                hasRegenerate: false,
                hasEdit: false,
                hasFeedback: false,
                isShareable: false,
                hasReadAloud: false
            ).isEmpty
        )
    }

    @Test("When the row does not fit, copy and the thumbs stay and the rest move into the menu")
    func compact() {
        let all = Tokens.Action.allCases
        #expect(Tokens.compactRow(all) == [.copy, .good, .bad])
        #expect(Tokens.compactMenu(all) == [.regenerate, .edit, .share, .readAloud])
        #expect(Tokens.compactMenu([.copy]).isEmpty)
        #expect(!Tokens.showsMenu(menuActions: [], hasMore: false))
        #expect(Tokens.showsMenu(menuActions: [], hasMore: true))
        #expect(Tokens.showsMenu(menuActions: [.edit], hasMore: false))
    }

    @Test("Pressing a thumb rates, pressing the pressed thumb clears, the other thumb switches")
    func rating() {
        #expect(Tokens.rating(after: .good, current: nil) == .up)
        #expect(Tokens.rating(after: .good, current: .up) == nil)
        #expect(Tokens.rating(after: .bad, current: .up) == .down)
        #expect(Tokens.rating(after: .bad, current: .down) == nil)
        #expect(Tokens.rating(after: .copy, current: .down) == .down)
    }

    @Test("A pressed thumb is filled and reading shows a stop symbol, so state is not colour alone")
    func symbols() {
        #expect(Tokens.symbol(.good, feedback: .up, isReading: false) == "hand.thumbsup.fill")
        #expect(Tokens.symbol(.good, feedback: .down, isReading: false) == "hand.thumbsup")
        #expect(Tokens.symbol(.bad, feedback: .down, isReading: false) == "hand.thumbsdown.fill")
        #expect(Tokens.symbol(.readAloud, feedback: nil, isReading: true) == "stop.circle")
        #expect(Tokens.symbol(.readAloud, feedback: nil, isReading: false) == "speaker.wave.2")
    }

    @Test("Titles are the spoken names, read aloud changes while reading, labels override")
    func titles() {
        let none = MonarchAiMessageActionsLabels()
        #expect(Tokens.title(.copy, isReading: false, labels: none) == "Copy")
        #expect(Tokens.title(.regenerate, isReading: false, labels: none) == "Regenerate")
        #expect(Tokens.title(.edit, isReading: false, labels: none) == "Edit")
        #expect(Tokens.title(.good, isReading: false, labels: none) == "Good response")
        #expect(Tokens.title(.bad, isReading: false, labels: none) == "Bad response")
        #expect(Tokens.title(.share, isReading: false, labels: none) == "Share")
        #expect(Tokens.title(.readAloud, isReading: false, labels: none) == "Read aloud")
        #expect(Tokens.title(.readAloud, isReading: true, labels: none) == "Stop reading")
        let custom = MonarchAiMessageActionsLabels(copy: "Copier", readAloud: "Lire", stopReading: "Arrêter")
        #expect(Tokens.title(.copy, isReading: false, labels: custom) == "Copier")
        #expect(Tokens.title(.readAloud, isReading: false, labels: custom) == "Lire")
        #expect(Tokens.title(.readAloud, isReading: true, labels: custom) == "Arrêter")
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchAiMessageActionsStrings.toolbar, MonarchAiMessageActionsStrings.copy,
            MonarchAiMessageActionsStrings.copied, MonarchAiMessageActionsStrings.regenerate,
            MonarchAiMessageActionsStrings.edit, MonarchAiMessageActionsStrings.good,
            MonarchAiMessageActionsStrings.bad, MonarchAiMessageActionsStrings.share,
            MonarchAiMessageActionsStrings.readAloud, MonarchAiMessageActionsStrings.stopReading,
            MonarchAiMessageActionsStrings.more,
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchAiMessageActionsStrings.toolbar == "Message actions")
        #expect(MonarchAiMessageActionsStrings.more == "More actions")
    }
}
