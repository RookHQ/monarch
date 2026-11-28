import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchCelebrationFixtures: ComponentFixtures {
    static let id = "celebration"
    static let fixtures = [
        Fixture("confetti with actions") {
            MonarchCelebration("Domain verified", message: "Email from acme.com is now signed.") {
                MonarchButton("View settings", variant: .secondary) {}
                MonarchButton("Continue", variant: .primary) {}
            }
        },
        Fixture("every tone, no decoration") {
            VStack {
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchCelebration("Done", tone: tone, decoration: .none, announces: false)
                }
            }
        },
        Fixture("custom symbol, long text") {
            MonarchCelebration(
                "Your first project has been published to everyone in the workspace",
                message:
                    "People you invited can open it now. You can change who sees it at any time from the sharing settings.",
                systemImage: "star.fill",
                announces: false
            ) {
                MonarchButton("Open the project in a new window", variant: .primary, fullWidth: true) {}
            }
        },
    ]
}

@Suite("MonarchCelebration")
struct MonarchCelebrationTests {
    private let piece = MonarchCelebrationTokens.pieces[3]

    @Test("Twelve pieces inside the area, each with a data colour")
    func pieces() {
        let pieces = MonarchCelebrationTokens.pieces
        #expect(pieces.count == 12)
        #expect(pieces.allSatisfy { $0.position > 0 && $0.position < 1 })
        #expect(pieces.allSatisfy { $0.color >= 0 && $0.color < MonarchCelebrationTokens.colors.count })
        #expect(pieces.contains { $0.shape == .dot })
        #expect(MonarchCelebrationTokens.colors.count == 5)
    }

    @Test("A piece is hidden before its delay, falls, turns and fades out")
    func fall() {
        let before = MonarchCelebrationTokens.state(of: piece, elapsed: 0.1, reduceMotion: false)
        #expect(before.opacity == 0)
        #expect(before.y == MonarchCelebrationTokens.startOffset)
        let middle = MonarchCelebrationTokens.state(of: piece, elapsed: 0.2 + 0.8, reduceMotion: false)
        #expect(middle.opacity == 1)
        #expect(middle.y > before.y)
        #expect(middle.rotation > piece.rotation)
        let end = MonarchCelebrationTokens.state(of: piece, elapsed: 10, reduceMotion: false)
        #expect(end.opacity == 0)
        #expect(end.y == MonarchCelebrationTokens.fallDistance)
        #expect(end.x == piece.drift)
        #expect(end.rotation == piece.rotation + MonarchCelebrationTokens.fallTurn)
    }

    @Test("The fade: in over the first 15%, out over the last 30%")
    func fade() {
        #expect(MonarchCelebrationTokens.opacity(at: 0) == 0)
        #expect(abs(MonarchCelebrationTokens.opacity(at: 0.075) - 0.5) < 0.0001)
        #expect(MonarchCelebrationTokens.opacity(at: 0.5) == 1)
        #expect(abs(MonarchCelebrationTokens.opacity(at: 0.85) - 0.5) < 0.0001)
        #expect(MonarchCelebrationTokens.opacity(at: 1) == 0)
    }

    @Test("Under Reduce Motion the pieces rest still, whatever the time")
    func reduceMotion() {
        let early = MonarchCelebrationTokens.state(of: piece, elapsed: 0, reduceMotion: true)
        let late = MonarchCelebrationTokens.state(of: piece, elapsed: 100, reduceMotion: true)
        #expect(early == late)
        #expect(early.opacity == MonarchCelebrationTokens.restingOpacity)
        #expect(early.x == piece.drift)
        #expect(early.rotation == piece.rotation)
    }

    @Test("The confetti falls once and stops")
    func oneShot() {
        let total = MonarchCelebrationTokens.totalDuration
        #expect(abs(total - (0.26 + 1.6)) < 0.0001)
        #expect(!MonarchCelebrationTokens.isFinished(elapsed: 1))
        #expect(MonarchCelebrationTokens.isFinished(elapsed: total))
    }

    @Test("The tile takes the tone's subtle fill and strong foreground")
    func tile() {
        let success = MonarchCelebrationTokens.tile(tone: .success)
        #expect(success.fill == \.successSubtle)
        #expect(success.foreground == \.successStrong)
        let info = MonarchCelebrationTokens.tile(tone: .info)
        #expect(info.fill == \.infoSubtle)
    }

    @Test("The announcement reads the title, then the message")
    func announcement() {
        #expect(
            MonarchCelebrationTokens.announcement(title: "Domain verified", message: "Email is signed.")
                == "Domain verified. Email is signed."
        )
        #expect(MonarchCelebrationTokens.announcement(title: "Done", message: nil) == "Done")
        #expect(MonarchCelebrationTokens.announcement(title: "Done", message: "") == "Done")
    }
}
