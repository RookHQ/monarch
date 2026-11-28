import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let me = MonarchCallStageParticipant("me", name: "Sam Lee", isSelf: true)
private let robin = MonarchCallStageParticipant("robin", name: "Robin Park", isSpeaking: true, quality: .good)
private let alex = MonarchCallStageParticipant("alex", name: "Alex Chen", isMicOff: true, quality: .fair)
private let jordan = MonarchCallStageParticipant("jordan", name: "Jordan Diaz", isHandRaised: true, quality: .poor)
private let team = [me, robin, alex, jordan]
private let english = Locale(identifier: "en_US")
/// A fixed start, so the timer never depends on when the test runs.
private let started = Date(timeIntervalSinceReferenceDate: 800_000_000)

enum MonarchCallStageFixtures: ComponentFixtures {
    static let id = "call-stage"
    static let fixtures = [
        Fixture("group grid with header buttons, low data, controls") {
            MonarchCallStage(
                "Weekly planning",
                participants: team,
                startedAt: started,
                mode: .grid,
                pinnedID: .constant(nil),
                isLowData: true,
                onSurfaceChange: { _ in },
                onPictureInPicture: {},
                onFullScreen: { _ in },
                controls: {
                    MonarchCallControls(isMicOn: .constant(true), isCameraOn: .constant(false), onLeave: {})
                }
            )
        },
        Fixture("one-to-one spotlight with a video slot") {
            MonarchCallStage(
                "Robin Park",
                participants: [me, robin],
                startedAt: started,
                video: { person in
                    Text(person.name).monarchTextStyle(.caption)
                },
                controls: { MonarchCallControls(isMicOn: .constant(false), onLeave: {}) }
            )
        },
        Fixture("pinned speaker, screen share, seven people, long names") {
            MonarchCallStage(
                "Quarterly business review with the extended leadership team",
                participants: team
                    + (1...3).map {
                        MonarchCallStageParticipant("guest\($0)", name: "Guest number \($0) with a long display name")
                    } + [MonarchCallStageParticipant("share", name: "Alex's screen", isScreen: true)],
                pinnedID: .constant("jordan"),
                onFullScreen: { _ in },
                controls: { EmptyView() }
            )
        },
        Fixture("docked and picture in picture surfaces") {
            VStack {
                MonarchCallStage(
                    "Support call",
                    participants: team,
                    mode: .grid,
                    surface: .docked,
                    onSurfaceChange: { _ in },
                    onMinimize: {},
                    onPictureInPicture: {},
                    controls: { MonarchCallControls(isMicOn: .constant(true), isMinimal: true, onLeave: {}) }
                )
                MonarchCallStage(participants: [me, robin], surface: .pip)
            }
        },
        Fixture("alone, reconnecting, error, muted hint, custom labels") {
            MonarchCallStage(
                participants: [
                    me, MonarchCallStageParticipant("x", name: "Alex", status: "Connecting…", statusTone: .warning),
                ],
                layout: .grid(ids: ["me", "x"], columns: 2),
                isReconnecting: true,
                error: "A connection couldn't be established. Leave and rejoin.",
                isSpeakingWhileMuted: true,
                labels: MonarchCallStageLabels(region: "Anruf", inCall: { "\($0) im Anruf" })
            )
            MonarchCallStage(participants: [me])
        },
    ]
}

@Suite("MonarchCallStage")
struct MonarchCallStageTests {
    @Test("Grid columns: one, two up to four people, then three")
    func gridColumns() {
        #expect(MonarchCallStageLayout.gridColumns(for: 0) == 1)
        #expect(MonarchCallStageLayout.gridColumns(for: 1) == 1)
        #expect(MonarchCallStageLayout.gridColumns(for: 2) == 2)
        #expect(MonarchCallStageLayout.gridColumns(for: 4) == 2)
        #expect(MonarchCallStageLayout.gridColumns(for: 5) == 3)
        #expect(MonarchCallStageLayout.gridColumns(for: 12) == 3)
    }

    @Test("Automatic layout: grid by default, then pin, remote screen, speaker and one-to-one")
    func automatic() {
        let ids = ["me", "robin", "alex", "jordan"]
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ids, mode: .automatic) == .grid(ids: ids, columns: 2)
        )
        #expect(MonarchCallStageLayout.automatic(selfID: "me", ids: ids, mode: .grid) == .grid(ids: ids, columns: 2))
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ids, mode: .grid, pinnedID: "me", sharerID: "alex")
                == .spotlight(main: "me", strip: ["robin", "alex", "jordan"])
        )
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ids, mode: .grid, sharerID: "alex")
                == .spotlight(main: "alex", strip: ["me", "robin", "jordan"])
        )
        // Your own screen never takes the spotlight.
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ids, mode: .grid, sharerID: "me")
                == .grid(ids: ids, columns: 2)
        )
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ids, mode: .speaker, activeSpeakerID: "jordan")
                == .spotlight(main: "jordan", strip: ["me", "robin", "alex"])
        )
        // The speaker is never yourself; the first other person stands in.
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ids, mode: .speaker, activeSpeakerID: "me")
                == .spotlight(main: "robin", strip: ["me", "alex", "jordan"])
        )
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ["me", "robin"], mode: .automatic)
                == .spotlight(main: "robin", strip: ["me"])
        )
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ["me", "robin"], mode: .grid)
                == .grid(ids: ["me", "robin"], columns: 2)
        )
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ["me"], mode: .speaker)
                == .grid(ids: ["me"], columns: 1)
        )
        // A pin of someone who left is ignored.
        #expect(
            MonarchCallStageLayout.automatic(selfID: "me", ids: ids, mode: .grid, pinnedID: "gone")
                == .grid(ids: ids, columns: 2)
        )
    }

    @Test("The stage uses the app's layout, or derives self and sharer from the participants")
    func resolvedLayout() {
        let screen = MonarchCallStageParticipant("alex", name: "Alex", isScreen: true)
        #expect(
            MonarchCallStageTokens.layout(
                nil,
                participants: [me, robin, screen],
                mode: .grid,
                pinnedID: nil,
                activeSpeakerID: nil
            )
                == .spotlight(main: "alex", strip: ["me", "robin"])
        )
        let given = MonarchCallStageLayout.grid(ids: ["me"], columns: 3)
        #expect(
            MonarchCallStageTokens.layout(
                given,
                participants: team,
                mode: .speaker,
                pinnedID: "robin",
                activeSpeakerID: nil
            ) == given
        )
    }

    @Test("Columns are capped at two on compact surfaces and narrow screens")
    func columns() {
        #expect(MonarchCallStageTokens.columns(3, surface: .stage, isNarrow: false) == 3)
        #expect(MonarchCallStageTokens.columns(3, surface: .stage, isNarrow: true) == 2)
        #expect(MonarchCallStageTokens.columns(3, surface: .docked, isNarrow: false) == 2)
        #expect(MonarchCallStageTokens.columns(3, surface: .pip, isNarrow: false) == 2)
        #expect(MonarchCallStageTokens.columns(1, surface: .docked, isNarrow: true) == 1)
        #expect(MonarchCallStageTokens.columns(0, surface: .stage, isNarrow: false) == 1)
    }

    @Test("Surfaces: tile buttons, padding and the filmstrip's place")
    func surfaces() {
        #expect(MonarchCallStageTokens.showsTileActions(.stage))
        #expect(!MonarchCallStageTokens.showsTileActions(.docked))
        #expect(!MonarchCallStageTokens.showsTileActions(.pip))
        #expect(MonarchCallStageTokens.padding(.stage) > MonarchCallStageTokens.padding(.docked))
        #expect(MonarchCallStageTokens.stripIsBeside(surface: .stage, isNarrow: false))
        #expect(!MonarchCallStageTokens.stripIsBeside(surface: .stage, isNarrow: true))
        #expect(!MonarchCallStageTokens.stripIsBeside(surface: .docked, isNarrow: false))
    }

    @Test("Header buttons depend on the surface and the callbacks")
    func tools() {
        typealias T = MonarchCallStageTokens
        #expect(
            T.tools(surface: .stage, hasPictureInPicture: true, hasSurfaceChange: true, hasMinimize: true) == [
                .pictureInPicture, .shrink,
            ]
        )
        #expect(
            T.tools(surface: .docked, hasPictureInPicture: true, hasSurfaceChange: true, hasMinimize: true) == [
                .pictureInPicture, .expand, .minimize,
            ]
        )
        #expect(T.tools(surface: .pip, hasPictureInPicture: true, hasSurfaceChange: true, hasMinimize: true).isEmpty)
        #expect(
            T.tools(surface: .stage, hasPictureInPicture: false, hasSurfaceChange: false, hasMinimize: true).isEmpty
        )
        #expect(
            T.tools(surface: .docked, hasPictureInPicture: false, hasSurfaceChange: false, hasMinimize: true) == [
                .minimize
            ]
        )
        let labels = MonarchCallStageLabels()
        for tool in T.Tool.allCases {
            #expect(!T.toolLabel(tool, labels: labels).hasPrefix("monarch."))
            #expect(!T.toolSymbol(tool).isEmpty)
        }
        #expect(T.toolLabel(.shrink, labels: MonarchCallStageLabels(shrink: "Verkleinern")) == "Verkleinern")
    }

    @Test("Head count: waiting while alone, then everyone counted")
    func headCount() {
        typealias T = MonarchCallStageTokens
        let labels = MonarchCallStageLabels()
        #expect(T.headCount([me], locale: english, labels: labels) == MonarchCallStageStrings.waiting)
        #expect(T.headCount([], locale: english, labels: labels) == MonarchCallStageStrings.waiting)
        #expect(T.headCount(team, locale: english, labels: labels) == "4 in call")
        #expect(T.headCount([robin], locale: english, labels: labels) == "1 in call")
        #expect(
            T.headCount(team, locale: english, labels: MonarchCallStageLabels(inCall: { "\($0) dabei" })) == "4 dabei"
        )
    }

    @Test("Announcements: an error, then losing the connection, then the muted hint, then the head count")
    func announcements() {
        typealias S = MonarchCallStageTokens.Snapshot
        let calm = S(headCount: "2 in call", reconnecting: nil, error: nil, mutedHint: nil)
        #expect(MonarchCallStageTokens.announcement(from: calm, to: calm) == nil)
        let joined = S(headCount: "3 in call", reconnecting: nil, error: nil, mutedHint: nil)
        #expect(MonarchCallStageTokens.announcement(from: calm, to: joined) == "3 in call")
        let offline = S(headCount: "3 in call", reconnecting: "Offline", error: nil, mutedHint: nil)
        #expect(MonarchCallStageTokens.announcement(from: calm, to: offline) == "Offline")
        #expect(MonarchCallStageTokens.announcement(from: offline, to: offline) == nil)
        let failed = S(headCount: "3 in call", reconnecting: "Offline", error: "Failed", mutedHint: "Muted")
        #expect(MonarchCallStageTokens.announcement(from: calm, to: failed) == "Failed")
        let muted = S(headCount: "2 in call", reconnecting: nil, error: nil, mutedHint: "Muted")
        #expect(MonarchCallStageTokens.announcement(from: calm, to: muted) == "Muted")
        // Recovering is not announced.
        #expect(MonarchCallStageTokens.announcement(from: offline, to: joined) == nil)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let all = [
            MonarchCallStageStrings.region, MonarchCallStageStrings.duration, MonarchCallStageStrings.waiting,
            MonarchCallStageStrings.lowData, MonarchCallStageStrings.reconnecting, MonarchCallStageStrings.mutedHint,
            MonarchCallStageStrings.pictureInPicture, MonarchCallStageStrings.shrink, MonarchCallStageStrings.expand,
            MonarchCallStageStrings.minimize, MonarchCallStageStrings.inCall("2"),
        ]
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchCallStageStrings.inCall("2") == "2 in call")
    }
}
