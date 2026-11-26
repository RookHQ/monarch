import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchMarketingConsentCheckboxFixtures: ComponentFixtures {
    static let id = "marketing-consent-checkbox"
    static let fixtures = [
        Fixture("one channel, unticked") {
            MonarchMarketingConsentCheckbox(selection: .constant([]))
        },
        Fixture("one channel, ticked, description and policy link") {
            MonarchMarketingConsentCheckbox(
                selection: .constant([.sms]),
                channels: [.sms],
                description: "Product updates and offers, about twice a month.",
                policyTitle: "Privacy policy",
                policyURL: URL(string: "https://example.com/privacy")
            )
        },
        Fixture("every channel, some ticked") {
            MonarchMarketingConsentCheckbox(
                selection: .constant([.email, .post]),
                channels: MonarchMarketingChannel.allCases,
                description: "Product updates and offers."
            )
        },
        Fixture("own wording, no unsubscribe note") {
            MonarchMarketingConsentCheckbox(
                "Keep me posted about new features, events and the occasional long announcement",
                selection: .constant([]),
                channels: [.email, .phone],
                showsUnsubscribeNote: false,
                channelLabels: [.phone: "A call from the team"]
            )
        },
        Fixture("own unsubscribe note") {
            MonarchMarketingConsentCheckbox(selection: .constant([]), unsubscribeNote: "Stop any time in Settings.")
        },
        Fixture("disabled") {
            MonarchMarketingConsentCheckbox(
                selection: .constant([.email]),
                channels: [.email, .sms],
                policyTitle: "Privacy policy",
                policyURL: URL(string: "https://example.com/privacy")
            )
            .disabled(true)
        },
    ]
}

@Suite("MonarchMarketingConsentCheckbox")
struct MonarchMarketingConsentCheckboxTests {
    private typealias Tokens = MonarchMarketingConsentCheckboxTokens

    @Test("The question names the channel when there is one, and is general for several")
    func question() {
        #expect(Tokens.question(label: nil, channels: [.email]) == "Send me product news by email")
        #expect(Tokens.question(label: nil, channels: [.sms]) == "Send me product news by text message")
        #expect(Tokens.question(label: nil, channels: [.phone]) == "Send me product news by phone")
        #expect(Tokens.question(label: nil, channels: [.post]) == "Send me product news by post")
        #expect(Tokens.question(label: nil, channels: [.email, .sms]) == "Send me product news")
        #expect(Tokens.question(label: nil, channels: []) == "Send me product news by email")
        #expect(Tokens.question(label: "Keep me posted", channels: [.email, .sms]) == "Keep me posted")
        #expect(Tokens.question(label: "", channels: [.post]) == "Send me product news by post")
    }

    @Test("Channels are shown once, in order, and email stands in for none")
    func shownChannels() {
        #expect(Tokens.shownChannels([.sms, .email, .sms]) == [.sms, .email])
        #expect(Tokens.shownChannels([]) == [.email])
        #expect(Tokens.shownChannels(MonarchMarketingChannel.allCases) == [.email, .sms, .phone, .post])
    }

    @Test("Channel labels come from the strings table unless replaced")
    func channelLabels() {
        #expect(Tokens.channelLabel(.email, labels: [:]) == "Email")
        #expect(Tokens.channelLabel(.sms, labels: [:]) == "Text message (SMS)")
        #expect(Tokens.channelLabel(.phone, labels: [:]) == "Phone")
        #expect(Tokens.channelLabel(.post, labels: [:]) == "Post")
        #expect(Tokens.channelLabel(.post, labels: [.post: "Letters"]) == "Letters")
        #expect(Tokens.channelLabel(.post, labels: [.post: ""]) == "Post")
    }

    @Test("The unsubscribe note can be hidden or replaced")
    func unsubscribeNote() {
        #expect(Tokens.unsubscribeNote(shows: true, override: nil) == "You can unsubscribe at any time.")
        #expect(Tokens.unsubscribeNote(shows: true, override: "Stop any time.") == "Stop any time.")
        #expect(Tokens.unsubscribeNote(shows: false, override: "Stop any time.") == nil)
    }

    @Test("The recorded wording is what is on screen, in reading order, with whitespace collapsed")
    func wording() {
        #expect(
            Tokens.wording(
                question: "Send me product news by email",
                channelLabels: [],
                description: "Product updates,\n  about twice a month.",
                unsubscribeNote: "You can unsubscribe at any time.",
                policyTitle: "Privacy policy"
            )
                == "Send me product news by email Product updates, about twice a month. "
                + "You can unsubscribe at any time. Privacy policy"
        )
        #expect(
            Tokens.wording(
                question: "Send me product news",
                channelLabels: ["Email", "Post"],
                description: nil,
                unsubscribeNote: nil,
                policyTitle: nil
            ) == "Send me product news Email Post"
        )
        #expect(
            Tokens.wording(
                question: "  ",
                channelLabels: [],
                description: "",
                unsubscribeNote: nil,
                policyTitle: nil
            ).isEmpty
        )
    }

    @Test("A change ticks or unticks one channel and records every shown channel")
    func consent() {
        let ticked = Tokens.selection([], setting: .sms, to: true)
        #expect(ticked == [.sms])
        #expect(Tokens.selection(ticked, setting: .sms, to: true) == [.sms])
        #expect(Tokens.selection(ticked, setting: .sms, to: false).isEmpty)
        #expect(Tokens.selection(ticked, setting: .email, to: false) == [.sms])

        let date = Date(timeIntervalSince1970: 1_000)
        let record = Tokens.consent(shown: [.email, .sms], selection: ticked, text: "Wording", date: date)
        #expect(record.channels == [.email: false, .sms: true])
        #expect(record.granted == [.sms])
        #expect(record.text == "Wording")
        #expect(record.date == date)
        let none = Tokens.consent(shown: [.email], selection: [], text: "", date: date)
        #expect(none.granted.isEmpty)
        #expect(none.channels == [.email: false])
    }

    @Test("Text uses the disabled role while disabled; the link colours come from MonarchLinkStyle")
    func colors() {
        #expect(Tokens.question(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(Tokens.question(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(Tokens.notes(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(Tokens.notes(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(
            MonarchLinkTokens.appearance(variant: .inline, isPressed: false, isEnabled: true, showsButtonShapes: false)
                .foreground == \MonarchColorTokens.textLink
        )
    }

    @Test("Every channel has built-in strings")
    func strings() {
        for channel in MonarchMarketingChannel.allCases {
            #expect(!MonarchMarketingConsentCheckboxStrings.question(channel).hasPrefix("monarch."), "\(channel)")
            #expect(!MonarchMarketingConsentCheckboxStrings.channel(channel).hasPrefix("monarch."), "\(channel)")
        }
        #expect(MonarchMarketingConsentCheckboxStrings.groupQuestion == "Send me product news")
        #expect(MonarchMarketingConsentCheckboxStrings.unsubscribe == "You can unsubscribe at any time.")
    }

    #if os(iOS)
        // The touch target applies on the simulator.
        @Test("The single checkbox row is at least as tall as the touch target")
        @MainActor
        func hitTarget() {
            let view = MonarchMarketingConsentCheckbox(selection: .constant([]), showsUnsubscribeNote: false)
            #expect(Host.layout(view).height >= MonarchAccessibility.minimumHitTarget)
        }
    #endif
}
