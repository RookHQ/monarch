import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiErrorMessageFixtures: ComponentFixtures {
    static let id = "ai-error-message"
    static let fixtures = [
        Fixture("every kind, with every action") {
            VStack {
                ForEach(MonarchAiErrorKind.allCases, id: \.self) { kind in
                    MonarchAiErrorMessage(
                        kind,
                        announces: false,
                        onRetry: {},
                        onEditPrompt: {},
                        onSwitchModel: {},
                        onNewConversation: {},
                        onSummarize: {}
                    )
                }
            }
        },
        Fixture("no actions") {
            MonarchAiErrorMessage(.network, announces: false)
        },
        Fixture("counting down, with retry") {
            MonarchAiErrorMessage(.rateLimit, retryAfter: 12, announces: false, onRetry: {})
        },
        Fixture("counting down, without retry") {
            MonarchAiErrorMessage(.rateLimit, retryAfter: 90, announces: false)
        },
        Fixture("retrying") {
            MonarchAiErrorMessage(.unknown, isRetrying: true, announces: false, onRetry: {}, onEditPrompt: {})
        },
        Fixture("details and custom text") {
            MonarchAiErrorMessage(
                .toolFailure,
                title: "The calendar could not be read",
                message: "The assistant asked the calendar for next week's meetings and got no answer in time.",
                details: "request_id: req_8f3a2c\ntool: calendar.search\nupstream: 504 Gateway Timeout",
                announces: false,
                labels: MonarchAiErrorMessageLabels(
                    tone: "Problem",
                    retry: "Ask the calendar again",
                    details: "Details"
                ),
                onRetry: {}
            )
        },
        Fixture("extra actions") {
            MonarchAiErrorMessage(.safetyBlock, announces: false, onEditPrompt: {}) {
                Button("Read the usage policy") {}
                    .buttonStyle(.monarch(.ghost, size: .sm))
            }
        },
    ]
}

@Suite("MonarchAiErrorMessage")
struct MonarchAiErrorMessageTests {
    @Test(
        "A raw code maps to a kind",
        arguments: [
            ("429", MonarchAiErrorKind.rateLimit),
            ("http-429", .rateLimit),
            ("529", .rateLimit),
            ("rate_limit_exceeded", .rateLimit),
            ("RateLimit", .rateLimit),
            ("Too Many Requests", .rateLimit),
            ("insufficient_quota", .rateLimit),
            ("overloaded_error", .rateLimit),
            ("context_length_exceeded", .contextOverflow),
            ("413", .contextOverflow),
            ("prompt too long", .contextOverflow),
            ("max_context", .contextOverflow),
            ("content_filter", .safetyBlock),
            ("safety", .safetyBlock),
            ("refusal", .safetyBlock),
            ("tool_error", .toolFailure),
            ("tool-result-failed", .toolFailure),
            ("ETIMEDOUT", .network),
            ("network", .network),
            ("503", .network),
            ("http-504", .network),
            ("fetch failed", .network),
            ("0", .network),
            ("500", .unknown),
            ("4290", .unknown),
            ("something_else", .unknown),
            ("", .unknown),
        ]
    )
    func kindFromCode(code: String, kind: MonarchAiErrorKind) {
        #expect(MonarchAiErrorKind(code: code) == kind)
    }

    @Test("No code is an unknown failure, and separators are normalised")
    func kindEdges() {
        #expect(MonarchAiErrorKind(code: nil) == .unknown)
        #expect(MonarchAiErrorKind.normalised("  Rate_Limit.Exceeded: now/later ") == "rate-limit-exceeded-now-later")
        // A rate limit wins over the words that follow it, because the rules are tried in order.
        #expect(MonarchAiErrorKind(code: "rate_limit_network") == .rateLimit)
    }

    @Test("Failures a person can wait out or rephrase are warnings; the rest are errors")
    func tones() {
        #expect(MonarchAiErrorMessageTokens.tone(.rateLimit) == .warning)
        #expect(MonarchAiErrorMessageTokens.tone(.contextOverflow) == .warning)
        #expect(MonarchAiErrorMessageTokens.tone(.safetyBlock) == .warning)
        #expect(MonarchAiErrorMessageTokens.tone(.network) == .danger)
        #expect(MonarchAiErrorMessageTokens.tone(.toolFailure) == .danger)
        #expect(MonarchAiErrorMessageTokens.tone(.unknown) == .danger)
    }

    @Test("Every kind has an icon, so the tone does not depend on colour")
    func symbols() {
        for kind in MonarchAiErrorKind.allCases {
            #expect(!MonarchAiErrorMessageTokens.symbol(kind).isEmpty)
        }
        #expect(MonarchAiErrorMessageTokens.symbol(.rateLimit) == "clock")
        #expect(MonarchAiErrorMessageTokens.symbol(.network) == "wifi.slash")
        #expect(
            MonarchAiErrorMessageTokens.symbol(.unknown) == MonarchBannerTokens.symbol(tone: .danger)
        )
        #expect(
            MonarchAiErrorMessageTokens.symbol(.safetyBlock) == MonarchBannerTokens.symbol(tone: .warning)
        )
    }

    @Test("Only handled actions are shown, in the order of the kind, and the first is primary")
    func actions() {
        typealias Action = MonarchAiErrorMessageTokens.Action
        let all = Set(Action.allCases)
        #expect(
            MonarchAiErrorMessageTokens.actions(for: .contextOverflow, handled: all)
                == [.newConversation, .summarize, .editPrompt, .switchModel, .retry]
        )
        #expect(
            MonarchAiErrorMessageTokens.actions(for: .safetyBlock, handled: all)
                == [.editPrompt, .retry, .newConversation, .switchModel]
        )
        #expect(
            MonarchAiErrorMessageTokens.actions(for: .rateLimit, handled: all)
                == [.retry, .switchModel, .editPrompt, .newConversation]
        )
        for kind in [MonarchAiErrorKind.network, .toolFailure, .unknown] {
            #expect(
                MonarchAiErrorMessageTokens.actions(for: kind, handled: all)
                    == [.retry, .editPrompt, .switchModel, .newConversation]
            )
        }
        #expect(
            MonarchAiErrorMessageTokens.actions(for: .network, handled: [.newConversation, .editPrompt]) == [
                .editPrompt, .newConversation,
            ]
        )
        #expect(MonarchAiErrorMessageTokens.actions(for: .network, handled: []).isEmpty)
        // Summarize is only in the order of a conversation that is too long.
        for kind in MonarchAiErrorKind.allCases where kind != .contextOverflow {
            #expect(!MonarchAiErrorMessageTokens.order(kind).contains(.summarize))
        }
        #expect(MonarchAiErrorMessageTokens.variant(at: 0) == .primary)
        #expect(MonarchAiErrorMessageTokens.variant(at: 1) == .secondary)
        #expect(MonarchAiErrorMessageTokens.variant(at: 4) == .secondary)
    }

    @Test("The countdown rounds up and never goes below zero")
    func countdown() {
        #expect(MonarchAiErrorMessageTokens.initialSeconds(nil) == 0)
        #expect(MonarchAiErrorMessageTokens.initialSeconds(0) == 0)
        #expect(MonarchAiErrorMessageTokens.initialSeconds(-5) == 0)
        #expect(MonarchAiErrorMessageTokens.initialSeconds(.infinity) == 0)
        #expect(MonarchAiErrorMessageTokens.initialSeconds(0.2) == 1)
        #expect(MonarchAiErrorMessageTokens.initialSeconds(12) == 12)
        #expect(MonarchAiErrorMessageTokens.initialSeconds(12.4) == 13)

        let now = Date(timeIntervalSince1970: 1000)
        #expect(MonarchAiErrorMessageTokens.secondsLeft(until: now.addingTimeInterval(11.5), now: now) == 12)
        #expect(MonarchAiErrorMessageTokens.secondsLeft(until: now.addingTimeInterval(0.4), now: now) == 1)
        #expect(MonarchAiErrorMessageTokens.secondsLeft(until: now, now: now) == 0)
        #expect(MonarchAiErrorMessageTokens.secondsLeft(until: now.addingTimeInterval(-30), now: now) == 0)
    }

    @Test("The announcement is the title, then the message")
    func announcement() {
        #expect(
            MonarchAiErrorMessageTokens.announcement(title: "Connection lost", message: "Check your connection.")
                == "Connection lost Check your connection."
        )
        #expect(MonarchAiErrorMessageTokens.announcement(title: "Connection lost", message: "") == "Connection lost")
    }

    @Test("Every kind and action has built-in text from the strings table")
    func strings() {
        let titles = MonarchAiErrorKind.allCases.map(MonarchAiErrorMessageStrings.title)
        let messages = MonarchAiErrorKind.allCases.map(MonarchAiErrorMessageStrings.message)
        #expect(Set(titles).count == MonarchAiErrorKind.allCases.count)
        #expect(Set(messages).count == MonarchAiErrorKind.allCases.count)
        for text in titles + messages {
            #expect(!text.hasPrefix("monarch."))
        }
        #expect(MonarchAiErrorMessageStrings.title(.rateLimit) == "Too many requests")
        #expect(MonarchAiErrorMessageStrings.title(.unknown) == "Something went wrong")
        #expect(MonarchAiErrorMessageStrings.action(.retry) == "Try again")
        #expect(MonarchAiErrorMessageStrings.action(.editPrompt) == "Edit message")
        #expect(MonarchAiErrorMessageStrings.action(.switchModel) == "Switch model")
        #expect(MonarchAiErrorMessageStrings.action(.newConversation) == "New conversation")
        #expect(MonarchAiErrorMessageStrings.action(.summarize) == "Summarize and continue")
        #expect(MonarchAiErrorMessageStrings.retryIn(12) == "Try again in 12s")
        #expect(MonarchAiErrorMessageStrings.retryReady == "You can try again now.")
        #expect(MonarchAiErrorMessageStrings.details == "Technical details")
    }

    @Test("Actions and details make the card taller")
    @MainActor
    func layout() {
        let plain = Host.layout(MonarchAiErrorMessage(.network, announces: false))
        let withAction = Host.layout(MonarchAiErrorMessage(.network, announces: false, onRetry: {}))
        let withDetails = Host.layout(
            MonarchAiErrorMessage(.network, details: "req_1", announces: false, onRetry: {})
        )
        #expect(withAction.height > plain.height)
        #expect(withDetails.height > withAction.height)
        // Summarize is not offered outside a conversation that is too long, so it adds nothing here.
        let ignored = Host.layout(MonarchAiErrorMessage(.network, announces: false, onSummarize: {}))
        #expect(ignored.height == plain.height)
    }
}
