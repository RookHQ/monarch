import Foundation
import SwiftUI
import Testing

@_spi(Experimental) @testable import MonarchUI

private let utc: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC") ?? .gmt
    return calendar
}()
/// Tuesday 10 March 2026, 12:00 UTC.
private let now = Date(timeIntervalSince1970: 1_773_144_000)
private let day: TimeInterval = 86_400

private func message(
    _ id: String,
    _ role: MonarchAiRole,
    _ text: String,
    at offset: TimeInterval,
    state: MonarchAiPartState = .done
) -> MonarchAiConversationMessage {
    MonarchAiConversationMessage(
        id: id,
        role: role,
        parts: [.text(MonarchAiTextPart(text: text, state: state))],
        createdAt: MonarchAiConversationMessage.timestamp(now.addingTimeInterval(offset))
    )
}

private let conversation: [MonarchAiConversationMessage] = [
    message("u1", .user, "When is invoice 42 due?", at: -2 * day),
    message("a1", .assistant, "Invoice 42 is due on **4 December**.", at: -2 * day + 5),
    message("u2", .user, "And invoice 43?", at: -day),
    message("a2", .assistant, "Invoice 43 is due on 10 December.", at: -day + 4),
    message("u3", .user, "Remind the client today.", at: -60),
]

private let streaming: [MonarchAiConversationMessage] =
    conversation + [message("a3", .assistant, "I will send a reminder", at: -50, state: .streaming)]

private let long = String(repeating: "A long message that wraps across many lines at the largest text size. ", count: 6)

enum MonarchAiThreadFixtures: ComponentFixtures {
    static let id = "ai-thread"
    static let fixtures = [
        Fixture("days, Today and Yesterday") {
            MonarchAiThread(conversation, now: now)
                .environment(\.timeZone, TimeZone(identifier: "UTC") ?? .gmt)
                .frame(height: 480)
        },
        Fixture("submitted with the thinking indicator and the slow notice") {
            MonarchAiThread(conversation, status: .submitted, isSlow: true, now: now, onStop: {})
                .frame(height: 480)
        },
        Fixture("streaming reply") {
            MonarchAiThread(streaming, status: .streaming, now: now, onStop: {})
                .frame(height: 480)
        },
        Fixture("earlier history and loading earlier") {
            VStack {
                MonarchAiThread(conversation, hasEarlier: true, now: now, onLoadEarlier: {})
                    .frame(height: 320)
                MonarchAiThread(conversation, hasEarlier: true, isLoadingEarlier: true, now: now, onLoadEarlier: {})
                    .frame(height: 320)
            }
        },
        Fixture("offline, sending queued messages") {
            VStack {
                MonarchAiThread(conversation, isOffline: true, now: now)
                    .frame(height: 240)
                MonarchAiThread(conversation, isOffline: true, queuedCount: 2, now: now)
                    .frame(height: 240)
                MonarchAiThread(conversation, queuedCount: 1, now: now)
                    .frame(height: 240)
            }
        },
        Fixture("error, stopped, no day labels, own announcements") {
            MonarchAiThread(
                conversation,
                status: .error,
                error: MonarchAiConversationError(
                    code: "network",
                    message: "The connection dropped.",
                    isRetryable: true
                ),
                showsDaySeparators: false,
                now: now,
                announces: false
            )
            .frame(height: 320)
        },
        Fixture("custom rows, long text and invalid times") {
            MonarchAiThread(
                [
                    MonarchAiConversationMessage(
                        id: "x1",
                        role: .user,
                        parts: [.text(MonarchAiTextPart(text: long))],
                        createdAt: "not a time"
                    ),
                    message("x2", .assistant, long, at: -30),
                ],
                now: now,
                labels: MonarchAiThreadLabels(label: "Chat with Ada", today: "Heute"),
                message: { item, context in
                    MonarchAiMessage(item, showsActions: context.isLast, announces: context.announces)
                }
            )
            .frame(height: 480)
        },
        Fixture("empty with a welcome") {
            MonarchAiThread(
                [],
                empty: { MonarchEmptyState("Ask anything", message: "Start a conversation with the assistant.") }
            )
            .frame(height: 320)
        },
        Fixture("empty while a reply is submitted") {
            MonarchAiThread([], status: .submitted).frame(height: 240)
        },
    ]
}

@Suite("MonarchAiThread")
struct MonarchAiThreadTests {
    private typealias Tokens = MonarchAiThreadTokens
    private typealias Announcer = MonarchAiThreadAnnouncer

    private let words = Announcer.Words(
        thinking: "Thinking…",
        complete: "Response complete",
        stopped: "Response stopped"
    )

    private func snapshot(_ id: String, _ text: String, isStreaming: Bool, isAssistant: Bool = true)
        -> Announcer.Snapshot
    {
        Announcer.Snapshot(id: id, isAssistant: isAssistant, spoken: text, isStreaming: isStreaming)
    }

    // MARK: Layout logic

    @Test("A day label above the first message of each day, none for an invalid time")
    func days() {
        let days = Tokens.dayStarts(conversation, calendar: utc)
        #expect(days.map { $0 != nil } == [true, false, true, false, true])
        var withInvalid = conversation
        withInvalid.insert(
            MonarchAiConversationMessage(id: "bad", role: .user, parts: [], createdAt: "yesterday"),
            at: 1
        )
        // The message after an invalid time starts a new day, as on the web.
        #expect(
            Tokens.dayStarts(withInvalid, calendar: utc).map { $0 != nil } == [true, false, true, true, false, true]
        )
    }

    @Test("Today and Yesterday overrides apply only to those days")
    func dayLabels() {
        let labels = MonarchAiThreadLabels(today: "Heute", yesterday: "Gestern")
        #expect(Tokens.dayLabel(now.addingTimeInterval(-60), now: now, calendar: utc, labels: labels) == "Heute")
        #expect(Tokens.dayLabel(now.addingTimeInterval(-day), now: now, calendar: utc, labels: labels) == "Gestern")
        #expect(Tokens.dayLabel(now.addingTimeInterval(-3 * day), now: now, calendar: utc, labels: labels) == nil)
        #expect(Tokens.dayLabel(now, now: now, calendar: utc, labels: MonarchAiThreadLabels()) == nil)
    }

    @Test("Offline wins over sending, and slow shows only while a reply runs")
    func notices() {
        #expect(Tokens.notice(isOffline: true, queuedCount: 2, isSlow: true, status: .streaming) == .offline(queued: 2))
        #expect(
            Tokens.notice(isOffline: false, queuedCount: 1, isSlow: true, status: .streaming) == .sending(queued: 1)
        )
        #expect(Tokens.notice(isOffline: false, queuedCount: 0, isSlow: true, status: .submitted) == .slow)
        #expect(Tokens.notice(isOffline: false, queuedCount: 0, isSlow: true, status: .idle) == nil)
        #expect(Tokens.notice(isOffline: false, queuedCount: 0, isSlow: false, status: .streaming) == nil)
        #expect(Tokens.Notice.offline(queued: 0).tone == .warning)
        #expect(Tokens.Notice.sending(queued: 1).tone == .warning)
        #expect(Tokens.Notice.slow.tone == .neutral)
        #expect(Set([Tokens.Notice.offline(queued: 0), .sending(queued: 1), .slow].map(\.systemImage)).count == 3)
    }

    @Test("Notice texts count the queued messages")
    func noticeTexts() {
        let labels = MonarchAiThreadLabels()
        #expect(
            Tokens.noticeText(.offline(queued: 0), labels: labels)
                == "You are offline. Messages you send will wait until you are back online."
        )
        #expect(
            Tokens.noticeText(.offline(queued: 1), labels: labels)
                == "You are offline. 1 message will send when you are back online."
        )
        #expect(Tokens.noticeText(.sending(queued: 3), labels: labels) == "Back online. Sending 3 queued messages…")
        #expect(Tokens.noticeText(.slow, labels: labels) == "This is taking longer than usual. Still working…")
        let custom = MonarchAiThreadLabels(offlineQueued: { "Offline \($0)" }, slow: "Slow")
        #expect(Tokens.noticeText(.offline(queued: 2), labels: custom) == "Offline 2")
        #expect(Tokens.noticeText(.slow, labels: custom) == "Slow")
    }

    @Test("The empty content shows only with nothing to show, load or wait for")
    func empty() {
        #expect(Tokens.showsEmpty(count: 0, hasEarlier: false, canLoadEarlier: false, status: .idle))
        #expect(!Tokens.showsEmpty(count: 1, hasEarlier: false, canLoadEarlier: false, status: .idle))
        #expect(!Tokens.showsEmpty(count: 0, hasEarlier: true, canLoadEarlier: true, status: .idle))
        #expect(Tokens.showsEmpty(count: 0, hasEarlier: true, canLoadEarlier: false, status: .idle))
        #expect(!Tokens.showsEmpty(count: 0, hasEarlier: false, canLoadEarlier: false, status: .submitted))
    }

    @Test("Load earlier needs the action and something to load")
    func loadEarlier() {
        #expect(Tokens.showsLoadEarlier(hasEarlier: true, isLoadingEarlier: false, canLoadEarlier: true))
        #expect(Tokens.showsLoadEarlier(hasEarlier: false, isLoadingEarlier: true, canLoadEarlier: true))
        #expect(!Tokens.showsLoadEarlier(hasEarlier: true, isLoadingEarlier: false, canLoadEarlier: false))
        #expect(!Tokens.showsLoadEarlier(hasEarlier: false, isLoadingEarlier: false, canLoadEarlier: true))
    }

    @Test("Jump to latest counts messages after the last one seen")
    func jump() {
        let ids = ["a", "b", "c", "d"]
        #expect(Tokens.newCount(ids: ids, lastSeenID: "b") == 2)
        #expect(Tokens.newCount(ids: ids, lastSeenID: "d") == 0)
        #expect(Tokens.newCount(ids: ids, lastSeenID: nil) == 0)
        #expect(Tokens.newCount(ids: ids, lastSeenID: "gone") == 0)
        let labels = MonarchAiThreadLabels()
        #expect(Tokens.jumpTitle(newCount: 0, labels: labels) == "Jump to latest")
        #expect(Tokens.jumpTitle(newCount: 1, labels: labels) == "1 new message")
        #expect(Tokens.jumpAccessibilityLabel(newCount: 2, labels: labels) == "Jump to latest, 2 new messages")
        #expect(Tokens.jumpAccessibilityLabel(newCount: 0, labels: labels) == "Jump to latest")
        #expect(Tokens.showsJump(isAtBottom: false, count: 3))
        #expect(!Tokens.showsJump(isAtBottom: true, count: 3))
        #expect(!Tokens.showsJump(isAtBottom: false, count: 0))
    }

    @Test("A conversation error is announced only when no message shows it")
    func errors() {
        let error = MonarchAiConversationError(code: "network", message: "The connection dropped.", isRetryable: true)
        let labels = MonarchAiThreadLabels()
        #expect(
            Tokens.errorAnnouncement(error, last: conversation.last, labels: labels) == "Error: The connection dropped."
        )
        #expect(Tokens.errorAnnouncement(nil, last: conversation.last, labels: labels) == nil)
        let shown = MonarchAiConversationMessage(
            id: "e",
            role: .assistant,
            parts: [.error(MonarchAiErrorPart(code: "network", message: "Dropped", isRetryable: true))],
            createdAt: "2026-03-10T12:00:00.000Z"
        )
        #expect(Tokens.errorAnnouncement(error, last: shown, labels: labels) == nil)
        let custom = MonarchAiThreadLabels(error: { "Fehler: \($0)" })
        #expect(Tokens.errorAnnouncement(error, last: nil, labels: custom) == "Fehler: The connection dropped.")
    }

    @Test("Running means submitted or streaming")
    func running() {
        let running = MonarchAiConversationStatus.allCases.filter(Tokens.isRunning)
        #expect(running == [.submitted, .streaming])
    }

    // MARK: Announcer

    @Test("Thinking is said once per submission")
    func thinking() {
        var announcer = Announcer()
        let user = snapshot("u", "", isStreaming: false, isAssistant: false)
        #expect(
            announcer.update(message: user, status: .submitted, words: words, interval: 1, now: now)
                == .announce("Thinking…")
        )
        #expect(announcer.update(message: user, status: .submitted, words: words, interval: 1, now: now) == .silent)
        _ = announcer.update(message: user, status: .idle, words: words, interval: 1, now: now)
        #expect(
            announcer.update(
                message: user,
                status: .submitted,
                words: words,
                interval: 1,
                now: now.addingTimeInterval(5)
            )
                == .announce("Thinking…")
        )
    }

    @Test("A streaming reply is read in whole sentences, throttled, then completed")
    func streamingReply() {
        var announcer = Announcer()
        var time = now
        func update(_ text: String, streaming: Bool, status: MonarchAiConversationStatus) -> Announcer.Outcome {
            announcer.update(
                message: snapshot("a", text, isStreaming: streaming),
                status: status,
                words: words,
                interval: 1,
                now: time
            )
        }
        #expect(update("Invoice 42 is", streaming: true, status: .streaming) == .silent)
        #expect(
            update("Invoice 42 is due. It is", streaming: true, status: .streaming) == .announce("Invoice 42 is due.")
        )
        time = time.addingTimeInterval(0.5)
        #expect(
            update("Invoice 42 is due. It is unpaid. The", streaming: true, status: .streaming)
                == .wait(until: now.addingTimeInterval(1))
        )
        time = now.addingTimeInterval(1)
        #expect(
            update("Invoice 42 is due. It is unpaid. The", streaming: true, status: .streaming)
                == .announce("It is unpaid.")
        )
        time = now.addingTimeInterval(1.2)
        // The end is said at once, with the rest of the text.
        #expect(
            update("Invoice 42 is due. It is unpaid. The client knows", streaming: false, status: .idle)
                == .announce("The client knows Response complete")
        )
        #expect(update("Invoice 42 is due. It is unpaid. The client knows", streaming: false, status: .idle) == .silent)
    }

    @Test("A stopped reply ends with Response stopped, an error with nothing")
    func endings() {
        var stopped = Announcer()
        _ = stopped.update(
            message: snapshot("a", "Partial", isStreaming: true),
            status: .streaming,
            words: words,
            interval: 1,
            now: now
        )
        #expect(
            stopped.update(
                message: snapshot("a", "Partial", isStreaming: false),
                status: .stopped,
                words: words,
                interval: 1,
                now: now
            )
                == .announce("Partial Response stopped")
        )
        var failed = Announcer()
        _ = failed.update(
            message: snapshot("a", "Partial", isStreaming: true),
            status: .streaming,
            words: words,
            interval: 1,
            now: now
        )
        #expect(
            failed.update(
                message: snapshot("a", "Partial", isStreaming: false),
                status: .error,
                words: words,
                interval: 1,
                now: now
            )
                == .announce("Partial")
        )
    }

    @Test("A tool call waiting for approval holds the end of the reply")
    func approval() {
        var announcer = Announcer()
        _ = announcer.update(
            message: snapshot("a", "", isStreaming: true),
            status: .streaming,
            words: words,
            interval: 1,
            now: now
        )
        #expect(
            announcer.update(
                message: snapshot("a", "", isStreaming: false),
                status: .awaitingApproval,
                words: words,
                interval: 1,
                now: now
            )
                == .silent
        )
        #expect(
            announcer.update(
                message: snapshot("a", "Done.", isStreaming: false),
                status: .idle,
                words: words,
                interval: 1,
                now: now
            )
                == .announce("Done. Response complete")
        )
    }

    @Test("History and user messages are not read")
    func history() {
        var announcer = Announcer()
        #expect(
            announcer.update(
                message: snapshot("a", "An old answer.", isStreaming: false),
                status: .idle,
                words: words,
                interval: 1,
                now: now
            )
                == .silent
        )
        #expect(
            announcer.update(
                message: snapshot("u", "Hi.", isStreaming: false, isAssistant: false),
                status: .idle,
                words: words,
                interval: 1,
                now: now
            )
                == .silent
        )
        #expect(announcer.update(message: nil, status: .idle, words: words, interval: 1, now: now) == .silent)
    }

    @Test("Sentence boundaries follow punctuation and white space, or a line break")
    func boundaries() {
        #expect(Announcer.lastBoundary("One. Two", from: 0) == 4)
        #expect(Announcer.lastBoundary("One.Two", from: 0) == 0)
        #expect(Announcer.lastBoundary("“Done!” Next", from: 0) == 6)
        #expect(Announcer.lastBoundary("Line one\nLine two", from: 0) == 9)
        #expect(Announcer.lastBoundary("One. Two. Three", from: 4) == 9)
        #expect(Announcer.lastBoundary("Short", from: 10) == 10)
        #expect(Announcer.normalized("  a \n\n b  ") == "a b")
        #expect(Announcer.interval(.milliseconds(1500)) == 1.5)
        #expect(Announcer.interval(.seconds(-1)) == 0)
    }

    @Test("The spoken text names code blocks and tables")
    @MainActor
    func speech() {
        let speech = MonarchAiThreadSpeech()
        let reply = message("a", .assistant, "Run this:\n\n```\nls -la\n```\n\nThen **check** it.", at: 0)
        let spoken = speech.spoken(reply, codeBlock: "Code block", table: "Table")
        #expect(spoken == "Run this:\n\nCode block\n\nThen check it.")
        let other = message("b", .assistant, "Plain", at: 0)
        #expect(speech.spoken(other, codeBlock: "Code block", table: "Table") == "Plain")
    }

    @Test("The message context passes position and announcing")
    func context() {
        let context = MonarchAiThreadMessageContext(index: 2, isLast: true, announces: false)
        #expect(context.index == 2 && context.isLast && !context.announces)
    }

    @Test("Built-in strings resolve")
    func strings() {
        let strings = [
            MonarchAiThreadStrings.label, MonarchAiThreadStrings.jumpToLatest, MonarchAiThreadStrings.loadEarlier,
            MonarchAiThreadStrings.loadingEarlier, MonarchAiThreadStrings.thinking,
            MonarchAiThreadStrings.responseComplete, MonarchAiThreadStrings.responseStopped,
            MonarchAiThreadStrings.codeBlock, MonarchAiThreadStrings.table, MonarchAiThreadStrings.today,
            MonarchAiThreadStrings.yesterday, MonarchAiThreadStrings.offline, MonarchAiThreadStrings.slow,
            MonarchAiThreadStrings.stop, MonarchAiThreadStrings.newMessages(1), MonarchAiThreadStrings.newMessages(2),
            MonarchAiThreadStrings.offlineQueued(1), MonarchAiThreadStrings.offlineQueued(2),
            MonarchAiThreadStrings.sendingQueued(1), MonarchAiThreadStrings.sendingQueued(2),
            MonarchAiThreadStrings.error("x"),
        ]
        for string in strings {
            #expect(!string.hasPrefix("monarch."))
            #expect(!string.isEmpty)
        }
        #expect(MonarchAiThreadStrings.newMessages(2) == "2 new messages")
        #expect(MonarchAiThreadStrings.error("Lost") == "Error: Lost")
    }
}
