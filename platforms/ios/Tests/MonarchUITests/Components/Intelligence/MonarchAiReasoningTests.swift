import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiReasoningFixtures: ComponentFixtures {
    static let id = "ai-reasoning"
    static let start = Date(timeIntervalSinceReferenceDate: 800_000_000)
    static let text =
        "The person asks for the cheapest flight to Lisbon in May. I should compare the three fares, then check the baggage rules."

    static let fixtures = [
        Fixture("streaming, open") {
            MonarchAiReasoning(MonarchAiReasoningPart(text: text, state: .streaming), startedAt: start)
        },
        Fixture("done, with duration, collapsed") {
            MonarchAiReasoning(MonarchAiReasoningPart(text: text, durationMs: 12_000))
        },
        Fixture("done, minutes, expanded") {
            MonarchAiReasoning(MonarchAiReasoningPart(text: text, durationMs: 65_000), startsExpanded: true)
        },
        Fixture("done, no duration") {
            MonarchAiReasoning(MonarchAiReasoningPart(text: text))
        },
        Fixture("redacted") {
            MonarchAiReasoning(MonarchAiReasoningPart(text: "", durationMs: 4_000, isRedacted: true))
        },
        Fixture("custom labels") {
            MonarchAiReasoning(
                MonarchAiReasoningPart(text: text, durationMs: 3_000),
                labels: MonarchAiReasoningLabels(thoughtFor: { "Réfléchi pendant \($0)" })
            )
        },
    ]
}

@Suite("MonarchAiReasoning")
struct MonarchAiReasoningTests {
    private let labels = MonarchAiReasoningLabels()
    private let english = Locale(identifier: "en_US")
    private let start = MonarchAiReasoningFixtures.start

    @Test("The summary follows the state, the redaction and the duration")
    func summary() {
        let streaming = MonarchAiReasoningPart(text: "a", state: .streaming, durationMs: 9_000)
        #expect(
            MonarchAiReasoningTokens.summary(part: streaming, measuredMs: nil, locale: english, labels: labels)
                == "Thinking…"
        )
        let redacted = MonarchAiReasoningPart(text: "", durationMs: 4_000, isRedacted: true)
        #expect(
            MonarchAiReasoningTokens.summary(part: redacted, measuredMs: nil, locale: english, labels: labels)
                == "Reasoning hidden"
        )
        let timed = MonarchAiReasoningPart(text: "a", durationMs: 12_000)
        #expect(
            MonarchAiReasoningTokens.summary(part: timed, measuredMs: 99_000, locale: english, labels: labels)
                == "Thought for 12s"
        )
        let untimed = MonarchAiReasoningPart(text: "a")
        #expect(
            MonarchAiReasoningTokens.summary(part: untimed, measuredMs: 65_000, locale: english, labels: labels)
                == "Thought for 1m 5s"
        )
        #expect(
            MonarchAiReasoningTokens.summary(part: untimed, measuredMs: nil, locale: english, labels: labels)
                == "Thought"
        )
    }

    @Test("Durations round to whole seconds, at least one")
    func durations() {
        #expect(MonarchAiReasoningTokens.seconds(ms: 0) == 1)
        #expect(MonarchAiReasoningTokens.seconds(ms: 400) == 1)
        #expect(MonarchAiReasoningTokens.seconds(ms: 1_600) == 2)
        #expect(MonarchAiReasoningTokens.durationText(ms: 120_000, locale: english, labels: labels) == "2m")
        let custom = MonarchAiReasoningLabels(duration: { "\($0) sec" })
        #expect(MonarchAiReasoningTokens.durationText(ms: 5_000, locale: english, labels: custom) == "5 sec")
    }

    @Test("The spoken header names the region")
    func accessibility() {
        #expect(
            MonarchAiReasoningTokens.accessibilityLabel(summary: "Thought for 12s", labels: labels)
                == "Assistant reasoning: Thought for 12s"
        )
        let custom = MonarchAiReasoningLabels(prefix: "Raisonnement")
        #expect(MonarchAiReasoningTokens.accessibilityLabel(summary: "x", labels: custom) == "Raisonnement: x")
    }

    @Test("Only unredacted text is disclosed")
    func body() {
        #expect(MonarchAiReasoningTokens.hasBody(MonarchAiReasoningPart(text: "a")))
        #expect(!MonarchAiReasoningTokens.hasBody(MonarchAiReasoningPart(text: "")))
        #expect(!MonarchAiReasoningTokens.hasBody(MonarchAiReasoningPart(text: "a", isRedacted: true)))
    }

    @Test("The timeline measures a stream with injected times")
    func timeline() {
        var timeline = MonarchAiReasoningTimeline(isStreaming: true, start: start)
        #expect(timeline.start == start)
        #expect(timeline.update(isStreaming: true, now: start.addingTimeInterval(3)) == .none)
        #expect(timeline.update(isStreaming: false, now: start.addingTimeInterval(12.4)) == .finished)
        #expect(timeline.measuredMs == 12_400)
        #expect(timeline.start == nil)
        #expect(timeline.update(isStreaming: false, now: start.addingTimeInterval(20)) == .none)
        // A regeneration starts a new measurement.
        let restart = start.addingTimeInterval(30)
        #expect(timeline.update(isStreaming: true, now: restart) == .started)
        #expect(timeline.start == restart)
        #expect(timeline.update(isStreaming: false, now: restart.addingTimeInterval(2)) == .finished)
        #expect(timeline.measuredMs == 2_000)
    }

    @Test("A finished part has no running timer")
    func finishedTimeline() {
        var timeline = MonarchAiReasoningTimeline(isStreaming: false, start: start)
        #expect(timeline.start == nil)
        #expect(timeline.measuredMs == nil)
        #expect(timeline.update(isStreaming: false, now: start) == .none)
    }

    @Test("It closes when done unless the person touched it, and reopens on a restart")
    func expansion() {
        #expect(!MonarchAiReasoningTokens.expanded(after: .finished, isExpanded: true, isTouched: false))
        #expect(MonarchAiReasoningTokens.expanded(after: .finished, isExpanded: true, isTouched: true))
        #expect(!MonarchAiReasoningTokens.expanded(after: .finished, isExpanded: false, isTouched: true))
        #expect(MonarchAiReasoningTokens.expanded(after: .started, isExpanded: false, isTouched: true))
        #expect(MonarchAiReasoningTokens.expanded(after: .none, isExpanded: true, isTouched: false))
        #expect(!MonarchAiReasoningTokens.expanded(after: .none, isExpanded: false, isTouched: false))
    }

    @Test("Built-in strings resolve")
    func strings() {
        #expect(MonarchAiReasoningStrings.prefix == "Assistant reasoning")
        #expect(MonarchAiReasoningStrings.thinking == "Thinking…")
        #expect(MonarchAiReasoningStrings.thoughtFor("3s") == "Thought for 3s")
        #expect(MonarchAiReasoningStrings.thought == "Thought")
        #expect(MonarchAiReasoningStrings.reasoningHidden == "Reasoning hidden")
    }

    @Test("Colours are AI and muted text roles")
    func appearance() {
        #expect(MonarchAiReasoningTokens.glyph == \MonarchColorTokens.aiStrong)
        #expect(MonarchAiReasoningTokens.bodyText == \MonarchColorTokens.textMuted)
        #expect(MonarchAiReasoningTokens.rule == \MonarchColorTokens.aiBorder)
    }
}
