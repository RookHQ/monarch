import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiStopButtonFixtures: ComponentFixtures {
    static let id = "ai-stop-button"
    static let fixtures = [
        Fixture("running: sizes, hidden label, disabled") {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonSize.allCases, id: \.self) { size in
                    MonarchAiStopButton(size: size) {}
                }
                MonarchAiStopButton(showsLabel: false) {}
                MonarchAiStopButton {}.disabled(true)
            }
        },
        Fixture("stopped, without and with Continue") {
            VStack(alignment: .leading) {
                MonarchAiStopButton(isStopped: true) {}
                MonarchAiStopButton(size: .md, isStopped: true, onContinue: {}, action: {})
            }
        },
        Fixture("overridden strings, long text") {
            MonarchAiStopButton(
                isStopped: true,
                stoppedLabel: "The answer was stopped before the assistant finished writing it.",
                continueLabel: "Carry on from here",
                onContinue: {},
                action: {}
            )
        },
    ]
}

@Suite("MonarchAiStopButton")
struct MonarchAiStopButtonTests {
    typealias Tokens = MonarchAiStopButtonTokens

    @Test("A running response shows only Stop")
    func running() {
        #expect(Tokens.parts(isStopped: false, hasContinue: false) == [.stop])
        #expect(Tokens.parts(isStopped: false, hasContinue: true) == [.stop])
    }

    @Test("A stopped response replaces Stop with the status, and Continue only when there is an action")
    func stopped() {
        #expect(Tokens.parts(isStopped: true, hasContinue: false) == [.status])
        #expect(Tokens.parts(isStopped: true, hasContinue: true) == [.status, .resume])
    }

    @Test("The status text is muted and regular weight, the symbol is a filled square")
    func tokens() {
        #expect(Tokens.statusColor == \MonarchColorTokens.textMuted)
        #expect(Tokens.statusWeight == .regular)
        #expect(Tokens.symbol == "stop.fill")
        #expect(Tokens.spacing == 8)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAiStopButtonStrings.stop == "Stop")
        #expect(MonarchAiStopButtonStrings.stopped == "Stopped.")
        #expect(MonarchAiStopButtonStrings.resume == "Continue")
    }

    #if os(iOS)
        @Test("Stop keeps a 44-point touch target without its title", arguments: MonarchButtonSize.allCases)
        @MainActor
        func hitTarget(size: MonarchButtonSize) {
            let button = MonarchAiStopButton(size: size, showsLabel: false) {}.fixedSize()
            #expect(Host.layout(button).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
