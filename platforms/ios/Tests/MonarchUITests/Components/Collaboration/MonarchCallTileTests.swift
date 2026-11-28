import Foundation
import SwiftUI
import Testing

@testable import MonarchUI

private let sent = Date(timeIntervalSinceReferenceDate: 800_000_000)

enum MonarchCallTileFixtures: ComponentFixtures {
    static let id = "call-tile"
    static let fixtures = [
        Fixture("avatar, every indicator, actions") {
            MonarchCallTile(
                "Ana Silva",
                isPinned: .constant(true),
                isMicOff: true,
                isSpeaking: true,
                isHandRaised: true,
                status: "Presenting",
                quality: .poor,
                onFullScreen: {}
            )
        },
        Fixture("video slot, yourself, mirrored") {
            MonarchCallTile("Ben Okafor", isPinned: .constant(false), isSelf: true, quality: .good) {
                Rectangle().fill(.tint)
            }
        },
        Fixture("shared screen") {
            MonarchCallTile("Chen Wei", isScreen: true, quality: .fair) {
                Rectangle().fill(.tint)
            }
        },
        Fixture("sizes and status tones") {
            VStack {
                ForEach(MonarchCallTileSize.allCases, id: \.self) { size in
                    MonarchCallTile("Dara", status: "Reconnecting…", statusTone: .warning, size: size, onFullScreen: {})
                }
                ForEach(MonarchTone.allCases, id: \.self) { tone in
                    MonarchCallTile("Eli", status: "Status", statusTone: tone, size: .sm)
                }
            }
        },
        Fixture("floating reaction") {
            MonarchCallTile("Fay Ng", reaction: MonarchCallReaction("👏", sentAt: Date()))
        },
        Fixture("long name") {
            MonarchCallTile(
                "Maximilian Alexander von Hohenberg-Lichtenstein",
                isSelf: true,
                isMicOff: true,
                status: "Connecting from a train with a weak signal",
                quality: .poor
            )
        },
    ]
}

@Suite("MonarchCallTile")
@MainActor
struct MonarchCallTileTests {
    typealias Tokens = MonarchCallTileTokens
    let labels = MonarchCallTileLabels()

    @Test("A reaction floats for three seconds after it was sent, by the injected time")
    func reactionVisibility() {
        let reaction = MonarchCallReaction("👏", sentAt: sent)
        #expect(Tokens.isReactionVisible(reaction, at: sent))
        #expect(Tokens.isReactionVisible(reaction, at: sent.addingTimeInterval(2.9)))
        #expect(!Tokens.isReactionVisible(reaction, at: sent.addingTimeInterval(3)))
        #expect(!Tokens.isReactionVisible(reaction, at: sent.addingTimeInterval(-1)))
        #expect(!Tokens.isReactionVisible(nil, at: sent))
    }

    @Test("The reaction's timeline wakes when it appears and when it ends")
    func reactionSchedule() {
        let reaction = MonarchCallReaction("👏", sentAt: sent)
        #expect(Tokens.reactionSchedule(reaction) == [sent, sent.addingTimeInterval(3)])
        #expect(Tokens.reactionSchedule(nil).isEmpty)
    }

    @Test("Your own tile is named with (you) and mirrored unless it is a screen")
    func yourself() {
        #expect(Tokens.displayName("Ana", isSelf: true, labels: labels) == "Ana (you)")
        #expect(Tokens.displayName("Ana", isSelf: false, labels: labels) == "Ana")
        #expect(Tokens.mirrors(isSelf: true, isScreen: false))
        #expect(!Tokens.mirrors(isSelf: true, isScreen: true))
        #expect(!Tokens.mirrors(isSelf: false, isScreen: false))
    }

    @Test("The value lists what is true now, so nothing depends on colour")
    func value() {
        let all = Tokens.accessibilityValue(
            isSpeaking: true,
            isMicOff: true,
            isHandRaised: true,
            isScreen: true,
            status: "Presenting",
            quality: .poor,
            labels: labels
        )
        #expect(all == "Speaking, Muted, Hand raised, Sharing screen, Presenting, Poor connection")
        let none = Tokens.accessibilityValue(
            isSpeaking: false,
            isMicOff: false,
            isHandRaised: false,
            isScreen: false,
            status: "",
            quality: nil,
            labels: labels
        )
        #expect(none.isEmpty)
    }

    @Test("Quality is the number of lit bars and a distinct name")
    func quality() {
        #expect(MonarchCallQuality.allCases.map(Tokens.bars) == [3, 2, 1])
        let names = MonarchCallQuality.allCases.map { Tokens.qualityLabel($0, labels: labels) }
        #expect(names == ["Good connection", "Unstable connection", "Poor connection"])
    }

    @Test("Pin and full screen show only when given and never on the filmstrip size")
    func actions() {
        #expect(Tokens.showsActions(.md, hasPin: true, hasFullScreen: false))
        #expect(Tokens.showsActions(.lg, hasPin: false, hasFullScreen: true))
        #expect(!Tokens.showsActions(.sm, hasPin: true, hasFullScreen: true))
        #expect(!Tokens.showsActions(.md, hasPin: false, hasFullScreen: false))
    }

    @Test("Status colours are distinct per tone on the inverse bar")
    func statusColors() {
        let roles = MonarchTone.allCases.map(Tokens.statusColor)
        #expect(Set(roles).count == MonarchTone.allCases.count)
    }

    @Test("The tile is 16 by 9 and grows when its content is taller")
    func height() {
        #expect(Tokens.height(width: 320, contentHeight: 50) == 180)
        #expect(Tokens.height(width: 320, contentHeight: 240) == 240)
        let size = Host.layout(MonarchCallTile("Ana"))
        #expect(abs(size.height - Host.width / Tokens.aspectRatio) < 1)
    }

    @Test("Overrides replace the built-in strings")
    func overrides() {
        let custom = MonarchCallTileLabels(
            you: { "\($0) (du)" },
            muted: "Stumm",
            reacted: { "\($0): \($1)" },
            quality: { _ in "Verbindung" }
        )
        #expect(Tokens.displayName("Ana", isSelf: true, labels: custom) == "Ana (du)")
        #expect(Tokens.reactedAnnouncement("Ana", "👏", labels: custom) == "Ana: 👏")
        #expect(
            Tokens.accessibilityValue(
                isSpeaking: false,
                isMicOff: true,
                isHandRaised: false,
                isScreen: false,
                status: nil,
                quality: .good,
                labels: custom
            ) == "Stumm, Verbindung"
        )
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchCallTileStrings.pin("Ana") == "Pin Ana")
        #expect(MonarchCallTileStrings.unpin("Ana") == "Unpin Ana")
        #expect(MonarchCallTileStrings.fullScreen("Ana") == "Show Ana full screen")
        #expect(MonarchCallTileStrings.reacted("Ana", "👏") == "Ana reacted 👏")
    }
}
