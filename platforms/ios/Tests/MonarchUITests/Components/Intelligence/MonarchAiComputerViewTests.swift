import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let start = Date(timeIntervalSinceReferenceDate: 800_000_000)

private let actions = [
    MonarchAiComputerAction(
        id: "open",
        kind: .navigate,
        label: "Opened the sign-in page",
        date: start,
        detail: "https://example.com/login"
    ),
    MonarchAiComputerAction(
        id: "email",
        kind: .type,
        label: "Typed the email address",
        date: start.addingTimeInterval(5),
        point: MonarchAiComputerPoint(x: 640, y: 300),
        detail: "sam@example.com"
    ),
    MonarchAiComputerAction(
        id: "click",
        kind: .click,
        label: "Clicked Continue",
        date: start.addingTimeInterval(9),
        point: MonarchAiComputerPoint(x: 900, y: 420)
    ),
]

private let secret = MonarchAiComputerAction(
    id: "password",
    kind: .type,
    label: "Typed the password",
    date: start.addingTimeInterval(12),
    point: MonarchAiComputerPoint(x: 640, y: 360),
    detail: "hunter2",
    isSensitive: true
)

private var frame: MonarchAiComputerFrame {
    MonarchAiComputerFrame(
        image: Image(systemName: "rectangle.on.rectangle"),
        width: 1280,
        height: 800,
        capturedAt: start.addingTimeInterval(10),
        alt: "The sign-in page with an email field"
    )
}

enum MonarchAiComputerViewFixtures: ComponentFixtures {
    static let id = "ai-computer-view"
    static let fixtures = [
        Fixture("working, every control") {
            MonarchAiComputerView(
                status: .working,
                frame: frame,
                actions: actions,
                currentURL: "https://user:secret@example.com/login",
                windowTitle: "Sign in",
                pointer: MonarchAiComputerPoint(x: 900, y: 420),
                selection: .constant("email"),
                onPauseChange: { _ in },
                onTakeOver: {},
                onHandBack: {},
                onStop: {}
            )
        },
        Fixture("starting, no screenshot") {
            MonarchAiComputerView(status: .starting, onStop: {})
        },
        Fixture("paused") {
            MonarchAiComputerView(status: .paused, frame: frame, actions: actions, onPauseChange: { _ in })
        },
        Fixture("waiting for you, with reason") {
            MonarchAiComputerView(
                status: .waitingForYou,
                frame: frame,
                actions: actions,
                waitingReason: "Sign in to continue",
                onTakeOver: {},
                onStop: {}
            )
        },
        Fixture("taken over, computer") {
            MonarchAiComputerView(status: .takenOver, frame: frame, surface: .computer, onHandBack: {})
        },
        Fixture("hidden after a private action") {
            MonarchAiComputerView(status: .working, frame: frame, actions: actions + [secret], showsTypedText: true)
        },
        Fixture("unavailable address") {
            MonarchAiComputerView(
                status: .done,
                frame: MonarchAiComputerFrame(
                    url: URL(string: "file:///tmp/screen.png"),
                    width: 1280,
                    height: 800,
                    capturedAt: start
                )
            )
        },
        Fixture("failed, compact log") {
            MonarchAiComputerView(status: .failed, frame: frame, actions: actions, isCompact: true)
        },
        Fixture("stopped, windowed log") {
            MonarchAiComputerView(status: .stopped, actions: actions, visibleActions: 1)
        },
        Fixture("custom labels") {
            MonarchAiComputerView(
                status: .working,
                labels: MonarchAiComputerViewLabels(status: { _ in "Busy" }, empty: "Nothing yet")
            )
        },
    ]
}

@Suite("MonarchAiComputerView")
struct MonarchAiComputerViewTests {
    private typealias Tokens = MonarchAiComputerViewTokens
    private let labels = MonarchAiComputerViewLabels()

    @Test("Status groups")
    func statusGroups() {
        let finished = MonarchAiComputerViewStatus.allCases.filter(Tokens.isFinished)
        #expect(finished == [.done, .failed, .stopped])
        #expect(MonarchAiComputerViewStatus.allCases.filter(Tokens.isAgentActing) == [.starting, .working])
        #expect(
            MonarchAiComputerViewStatus.allCases.filter(Tokens.isAgentHolding)
                == [.starting, .working, .paused, .waitingForYou]
        )
    }

    @Test("Each button shows only for its status and handler")
    func controls() {
        let all = { (status: MonarchAiComputerViewStatus) in
            Tokens.controls(status, hasPause: true, hasTakeOver: true, hasHandBack: true, hasStop: true)
        }
        #expect(all(.working) == .init(canPause: true, canTakeOver: true, canStop: true))
        #expect(all(.paused) == .init(canResume: true, canTakeOver: true, canStop: true))
        #expect(all(.waitingForYou) == .init(canTakeOver: true, canStop: true))
        #expect(all(.takenOver) == .init(canHandBack: true, canStop: true))
        #expect(all(.done).isEmpty)
        #expect(all(.failed).isEmpty)
        let none = Tokens.controls(.working, hasPause: false, hasTakeOver: false, hasHandBack: false, hasStop: false)
        #expect(none.isEmpty)
    }

    @Test("Status icons and tones never rely on colour alone")
    func statusLook() {
        #expect(Tokens.symbol(.starting) == nil)
        for status in MonarchAiComputerViewStatus.allCases where status != .starting {
            #expect(Tokens.symbol(status) != nil)
        }
        #expect(Tokens.tone(.failed) == .danger)
        #expect(Tokens.tone(.waitingForYou) == .warning)
        #expect(Tokens.tone(.done) == .success)
        #expect(Tokens.statusName(.waitingForYou, labels: labels) == "Waiting for you")
    }

    @Test("The banner says who is in control")
    func banner() {
        #expect(
            Tokens.banner(.working, surface: .browser, labels: labels) == "The assistant is controlling this browser"
        )
        #expect(Tokens.banner(.takenOver, surface: .computer, labels: labels) == "You are controlling this computer")
        #expect(Tokens.banner(.done, surface: .browser, labels: labels) == nil)
    }

    @Test("Status changes are announced once, with control changes in their own words")
    func statusAnnouncement() {
        #expect(Tokens.statusAnnouncement(from: .working, to: .working, surface: .browser, labels: labels) == nil)
        #expect(
            Tokens.statusAnnouncement(from: .working, to: .takenOver, surface: .browser, labels: labels)
                == "You are in control of this browser. The assistant has stopped acting."
        )
        #expect(
            Tokens.statusAnnouncement(from: .takenOver, to: .working, surface: .computer, labels: labels)
                == "The assistant is in control of this computer again."
        )
        #expect(
            Tokens.statusAnnouncement(from: .working, to: .done, surface: .browser, labels: labels)
                == "Assistant status: Done"
        )
    }

    @Test("Sensitive actions are never named and typed text is withheld by default")
    func privacy() {
        #expect(Tokens.actionLabel(secret, labels: labels) == "Private action")
        #expect(Tokens.detail(secret, showsTypedText: true) == .init(text: nil, isHidden: true))
        #expect(Tokens.detail(actions[1], showsTypedText: false) == .init(text: nil, isHidden: true))
        #expect(Tokens.detail(actions[1], showsTypedText: true) == .init(text: "sam@example.com", isHidden: false))
        #expect(Tokens.detail(actions[0], showsTypedText: false).text == "https://example.com/login")
        #expect(Tokens.detail(actions[2], showsTypedText: false) == .init(text: nil, isHidden: false))
    }

    @Test("Log rows read the kind, the label, the detail and the time")
    func rows() {
        #expect(Tokens.rowLabel(actions[2], labels: labels) == "Click: Clicked Continue")
        #expect(Tokens.rowLabel(secret, labels: labels) == "Type: Private action")
        #expect(Tokens.rowValue(actions[1], showsTypedText: false, time: "9:00", labels: labels) == "Hidden, 9:00")
        #expect(Tokens.rowValue(actions[2], showsTypedText: false, time: "9:00", labels: labels) == "9:00")
        #expect(Tokens.rowAppearance(isSelected: true).background == \.accentSubtle)
        #expect(Tokens.rowAppearance(isSelected: false).background == \.bgSurface)
        #expect(Tokens.rowAppearance(isSelected: true).border != Tokens.rowAppearance(isSelected: false).border)
    }

    @Test("Times use the given locale and time zone")
    func time() throws {
        let utc = try #require(TimeZone(identifier: "UTC"))
        let date = Date(timeIntervalSince1970: 1_700_000_000)
        let english = Tokens.time(date, locale: Locale(identifier: "en_US"), timeZone: utc)
        #expect(english.contains("10:13:20"))
        #expect(english.contains("PM"))
        #expect(Tokens.time(date, locale: Locale(identifier: "de_DE"), timeZone: utc) == "22:13:20")
    }

    @Test("Windowed log and announcements of new actions")
    func windowing() {
        #expect(Tokens.hiddenBefore(total: 10, visible: 4, showsAll: false) == 6)
        #expect(Tokens.hiddenBefore(total: 10, visible: 4, showsAll: true) == 0)
        #expect(Tokens.hiddenBefore(total: 3, visible: 0, showsAll: false) == 2)
        #expect(Tokens.hiddenBefore(total: 2, visible: 200, showsAll: false) == 0)
        #expect(Tokens.freshActions(actions, known: ["open"]).map(\.id) == ["email", "click"])
        #expect(Tokens.actionAnnouncement(count: 0, latest: "x", labels: labels) == nil)
        #expect(Tokens.actionAnnouncement(count: 1, latest: "Clicked Continue", labels: labels) == "Clicked Continue")
        #expect(
            Tokens.actionAnnouncement(count: 3, latest: "Clicked Continue", labels: labels)
                == "3 new actions. Latest: Clicked Continue"
        )
        #expect(Tokens.lastPointAction(actions)?.id == "click")
        #expect(Tokens.lastPointAction([actions[0]]) == nil)
    }

    @Test("Screenshot state: hidden, unavailable, shown or none")
    func frameState() {
        #expect(Tokens.frameState(nil, latest: nil) == .none)
        #expect(Tokens.frameState(frame, latest: actions.last) == .shown)
        #expect(Tokens.frameState(frame, latest: secret) == .hidden)
        let sensitive = MonarchAiComputerFrame(image: nil, width: 1, height: 1, capturedAt: start, isSensitive: true)
        #expect(Tokens.frameState(sensitive, latest: nil) == .hidden)
        let file = MonarchAiComputerFrame(url: URL(string: "file:///x.png"), width: 1, height: 1, capturedAt: start)
        #expect(Tokens.frameState(file, latest: nil) == .unavailable)
        let web = MonarchAiComputerFrame(
            url: URL(string: "HTTPS://example.com/x.png"),
            width: 1,
            height: 1,
            capturedAt: start
        )
        #expect(Tokens.frameState(web, latest: nil) == .shown)
        #expect(!Tokens.isSafe(URL(string: "javascript:alert(1)")))
        #expect(!Tokens.isSafe(nil))
    }

    @Test("The screenshot description joins its text and the latest action")
    func frameDescription() {
        #expect(
            Tokens.frameDescription(frame, state: .shown, surface: .browser, latest: actions[2], labels: labels)
                == "The sign-in page with an email field. Latest action: Clicked Continue"
        )
        let bare = MonarchAiComputerFrame(image: nil, width: 1, height: 1, capturedAt: start)
        #expect(
            Tokens.frameDescription(bare, state: .shown, surface: .computer, latest: nil, labels: labels)
                == "Screenshot of the computer the assistant is using"
        )
        #expect(
            Tokens.frameDescription(frame, state: .hidden, surface: .browser, latest: secret, labels: labels)
                == "Screenshot hidden: it may show private information. Latest action: Private action"
        )
        #expect(Tokens.placeholder(.none, labels: labels) == "No screenshot yet")
        #expect(Tokens.placeholder(.unavailable, labels: labels) == "Screenshot unavailable")
        #expect(Tokens.placeholder(.shown, labels: labels) == nil)
    }

    @Test("Points become clamped fractions of the screenshot")
    func position() {
        #expect(
            Tokens.position(MonarchAiComputerPoint(x: 640, y: 200), width: 1280, height: 800)
                == UnitPoint(x: 0.5, y: 0.25)
        )
        #expect(
            Tokens.position(MonarchAiComputerPoint(x: -10, y: 900), width: 1280, height: 800) == UnitPoint(x: 0, y: 1)
        )
        #expect(Tokens.position(MonarchAiComputerPoint(x: 1, y: 1), width: 0, height: 800) == nil)
        #expect(Tokens.position(MonarchAiComputerPoint(x: .nan, y: 1), width: 10, height: 10) == nil)
        #expect(Tokens.position(nil, width: 10, height: 10) == nil)
        #expect(Tokens.aspectRatio(frame) == 1.6)
        #expect(Tokens.aspectRatio(nil) == Tokens.placeholderAspectRatio)
    }

    @Test("Credentials never show in the address")
    func displayURL() {
        #expect(Tokens.displayURL("https://user:secret@example.com/login") == "https://example.com/login")
        #expect(Tokens.displayURL("https://example.com/a@b") == "https://example.com/a@b")
        #expect(Tokens.displayURL("https://example.com") == "https://example.com")
        #expect(Tokens.displayURL("example.com") == "example.com")
    }

    @Test("Custom labels replace the built-in strings")
    func custom() {
        let custom = MonarchAiComputerViewLabels(
            status: { $0 == .done ? "Finished" : "Other" },
            kind: { _ in "Step" },
            sensitiveLabel: "Secret",
            announceStatus: { "Now \($0)" }
        )
        #expect(Tokens.statusName(.done, labels: custom) == "Finished")
        #expect(Tokens.rowLabel(secret, labels: custom) == "Step: Secret")
        #expect(
            Tokens.statusAnnouncement(from: .working, to: .done, surface: .browser, labels: custom) == "Now Finished"
        )
    }

    @Test("Built-in strings resolve")
    func builtIn() {
        var all: [String] = [
            MonarchAiComputerViewStrings.waitingTitle, MonarchAiComputerViewStrings.waitingFallback,
            MonarchAiComputerViewStrings.pause, MonarchAiComputerViewStrings.resume,
            MonarchAiComputerViewStrings.takeOver, MonarchAiComputerViewStrings.handBack,
            MonarchAiComputerViewStrings.stop, MonarchAiComputerViewStrings.stopQuestion,
            MonarchAiComputerViewStrings.stopConfirm, MonarchAiComputerViewStrings.stopCancel,
            MonarchAiComputerViewStrings.logLabel, MonarchAiComputerViewStrings.logToggle(3),
            MonarchAiComputerViewStrings.empty, MonarchAiComputerViewStrings.hidden,
            MonarchAiComputerViewStrings.sensitiveLabel, MonarchAiComputerViewStrings.noFrame,
            MonarchAiComputerViewStrings.frameUnavailable, MonarchAiComputerViewStrings.frameHidden,
            MonarchAiComputerViewStrings.latestAction("a", "b"), MonarchAiComputerViewStrings.capturedAt("9:00"),
            MonarchAiComputerViewStrings.address, MonarchAiComputerViewStrings.zoom,
            MonarchAiComputerViewStrings.showEarlier(1), MonarchAiComputerViewStrings.showEarlier(4),
            MonarchAiComputerViewStrings.announceActions(2, "x"), MonarchAiComputerViewStrings.announceStatus("x"),
        ]
        for surface in MonarchAiComputerViewSurface.allCases {
            all += [
                MonarchAiComputerViewStrings.label(surface), MonarchAiComputerViewStrings.agentInControl(surface),
                MonarchAiComputerViewStrings.youInControl(surface), MonarchAiComputerViewStrings.frameAlt(surface),
                MonarchAiComputerViewStrings.announceTakenOver(surface),
                MonarchAiComputerViewStrings.announceReleased(surface),
            ]
        }
        all += MonarchAiComputerViewStatus.allCases.map(MonarchAiComputerViewStrings.status)
        all += MonarchAiComputerActionKind.allCases.map(MonarchAiComputerViewStrings.kind)
        for string in all {
            #expect(!string.hasPrefix("monarch."))
        }
        #expect(MonarchAiComputerViewStrings.showEarlier(4) == "Show 4 earlier actions")
        #expect(MonarchAiComputerViewStrings.logToggle(3) == "Actions (3)")
    }
}
