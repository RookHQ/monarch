import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiTypingDotsFixtures: ComponentFixtures {
    static let id = "ai-typing-dots"
    static let fixtures = [
        Fixture("sizes") {
            HStack {
                ForEach(MonarchAiTypingDotsSize.allCases, id: \.self) { size in
                    MonarchAiTypingDots(size: size, isAnnounced: false)
                }
            }
        },
        Fixture("custom label, in a row") {
            HStack {
                Text("Ada")
                MonarchAiTypingDots(label: "Ada is typing", size: .sm, isAnnounced: false)
            }
        },
        Fixture("still form, as under Reduce Motion") {
            MonarchAiTypingDotsFrame(dot: MonarchAiTypingDotsTokens.dotSize(.md), time: nil)
        },
        Fixture("one moment of the animation, first dot at its peak") {
            MonarchAiTypingDotsFrame(
                dot: MonarchAiTypingDotsTokens.dotSize(.md),
                time: MonarchAiTypingDotsTokens.period * MonarchAiTypingDotsTokens.riseFraction
            )
        },
    ]
}

@Suite("MonarchAiTypingDots")
struct MonarchAiTypingDotsTests {
    typealias Tokens = MonarchAiTypingDotsTokens

    @Test("One cycle is three quarters of the shimmer duration, with an eighth between dots")
    func timing() {
        #expect(abs(Tokens.period - 1.2) < 1e-9)
        #expect(abs(Tokens.stagger - 0.2) < 1e-9)
    }

    @Test("A dot rests, peaks at 30% of the cycle and is back at rest by 60%")
    func lift() {
        #expect(Tokens.lift(at: 0, index: 0) == 0)
        #expect(abs(Tokens.lift(at: Tokens.period * 0.3, index: 0) - 1) < 1e-9)
        #expect(abs(Tokens.lift(at: Tokens.period * 0.15, index: 0) - 0.5) < 1e-9)
        #expect(abs(Tokens.lift(at: Tokens.period * 0.45, index: 0) - 0.5) < 1e-9)
        #expect(Tokens.lift(at: Tokens.period * 0.6, index: 0) < 1e-9)
        #expect(Tokens.lift(at: Tokens.period * 0.8, index: 0) == 0)
    }

    @Test("Each dot follows the one before it by one stagger, and the cycle repeats")
    func stagger() {
        let peak = Tokens.period * 0.3
        for index in 0..<Tokens.count {
            let time = peak + Double(index) * Tokens.stagger
            #expect(abs(Tokens.lift(at: time, index: index) - 1) < 1e-9)
            #expect(abs(Tokens.lift(at: time + Tokens.period * 7, index: index) - 1) < 1e-6)
        }
        // Before a later dot's first start it is in the resting part of the previous cycle.
        #expect(Tokens.lift(at: 0, index: 2) == 0)
    }

    @Test("The lift stays between 0 and 1 at every moment")
    func liftRange() {
        for step in 0...600 {
            for index in 0..<Tokens.count {
                let value = Tokens.lift(at: Double(step) * 0.01 - 1, index: index)
                #expect(value >= 0 && value <= 1)
            }
        }
    }

    @Test("Opacity runs from the resting value to 1, and is fixed in the still form")
    func opacity() {
        #expect(Tokens.opacity(lift: 0) == 0.4)
        #expect(Tokens.opacity(lift: 1) == 1)
        #expect(abs(Tokens.opacity(lift: 0.5) - 0.7) < 1e-9)
        #expect(Tokens.opacity(lift: 3) == 1)
        #expect(Tokens.opacity(lift: nil) == 0.7)
    }

    @Test("Dot sizes come from the space scale and the colour from the assistant role")
    func tokens() {
        #expect(Tokens.dotSize(.sm) == 4)
        #expect(Tokens.dotSize(.md) == 6)
        #expect(Tokens.color == \MonarchColorTokens.aiStrong)
        #expect(Tokens.count == 3)
    }

    @Test("The spoken label comes from the component's strings table")
    func strings() {
        #expect(MonarchAiTypingDotsStrings.typing == "Assistant is typing")
    }

    @Test("The medium dots take more room than the small ones")
    @MainActor
    func sizes() {
        let small = Host.layout(MonarchAiTypingDots(size: .sm, isAnnounced: false).fixedSize())
        let medium = Host.layout(MonarchAiTypingDots(size: .md, isAnnounced: false).fixedSize())
        #expect(medium.height > small.height)
    }
}
