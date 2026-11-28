import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAiInsightCalloutFixtures: ComponentFixtures {
    static let id = "ai-insight-callout"
    static let fixtures: [Fixture] =
        MonarchTone.allCases.map { tone in
            Fixture("tone \(tone), ai") {
                MonarchAiInsightCallout("Renewal at risk", why: "Usage fell 40% in 30 days.", origin: .ai, tone: tone)
            }
        } + [
            Fixture("rule, with action, dismiss and evidence") {
                MonarchAiInsightCallout(
                    "Invoice is 30 days overdue",
                    why: "Invoice 1042 was due on 1 October and is unpaid.",
                    origin: .rule,
                    tone: .danger,
                    actionTitle: "Send reminder",
                    isHeading: true,
                    action: {},
                    onDismiss: {},
                    evidence: { Text("Amount: 4,200. Due: 1 October.") }
                )
            },
            Fixture("long text, own labels") {
                MonarchAiInsightCallout(
                    "Three accounts in the north-east region renewed early this quarter after the onboarding change",
                    why:
                        "Renewals came in an average of 21 days before the due date, compared with 4 days last quarter, across every plan size.",
                    origin: .ai,
                    tone: .success,
                    labels: MonarchAiInsightCalloutLabels(
                        tone: "Good news",
                        ai: "From the assistant",
                        evidence: "Data"
                    ),
                    onDismiss: {},
                    evidence: { Text("Average days early: 21 (was 4).") }
                )
            },
        ]
}

@Suite("MonarchAiInsightCallout")
struct MonarchAiInsightCalloutTests {
    typealias Tokens = MonarchAiInsightCalloutTokens

    @Test("Each tone uses its own subtle fill, border, strong accent and symbol", arguments: MonarchTone.allCases)
    func tones(tone: MonarchTone) {
        let palette = MonarchTonePalette.palette(for: tone)
        let surface = Tokens.surface(tone: tone)
        #expect(surface.fill == palette.subtle)
        #expect(surface.border == palette.border)
        #expect(surface.accent == palette.strong)
        #expect(Tokens.symbol(tone: tone) == palette.symbol)
    }

    @Test("Tones have distinct symbols, so the tone is not colour alone")
    func distinctSymbols() {
        let symbols = [MonarchTone.info, .success, .warning, .danger].map(Tokens.symbol(tone:))
        #expect(Set(symbols).count == symbols.count)
    }

    @Test("Only the AI origin uses the assistant colours")
    func origin() {
        let ai = Tokens.origin(.ai)
        #expect(ai.background == \MonarchColorTokens.aiSubtle)
        #expect(ai.foreground == \MonarchColorTokens.aiStrong)
        #expect(ai.border == \MonarchColorTokens.aiBorder)
        let rule = Tokens.origin(.rule)
        #expect(rule.background == \MonarchColorTokens.bgSurface)
        #expect(rule.foreground == \MonarchColorTokens.textMuted)
        #expect(rule.border == \MonarchColorTokens.borderDefault)
    }

    @Test("The origin text is built in or replaced")
    func originText() {
        #expect(Tokens.originText(.ai, labels: MonarchAiInsightCalloutLabels()) == "AI insight")
        #expect(Tokens.originText(.rule, labels: MonarchAiInsightCalloutLabels()) == "Rule-based")
        #expect(Tokens.originText(.ai, labels: MonarchAiInsightCalloutLabels(ai: "Model")) == "Model")
        #expect(Tokens.originText(.rule, labels: MonarchAiInsightCalloutLabels(rule: "Rule")) == "Rule")
    }

    @Test("The spoken title starts with the tone name; neutral has none")
    func spokenTitle() {
        let labels = MonarchAiInsightCalloutLabels()
        #expect(Tokens.spokenTitle("Renewal at risk", tone: .warning, labels: labels) == "Warning: Renewal at risk")
        #expect(Tokens.spokenTitle("Renewal at risk", tone: .neutral, labels: labels) == "Renewal at risk")
        #expect(
            Tokens.spokenTitle("Up", tone: .success, labels: MonarchAiInsightCalloutLabels(tone: "Good news"))
                == "Good news: Up"
        )
        #expect(Tokens.spokenTitle("Up", tone: .success, labels: MonarchAiInsightCalloutLabels(tone: "")) == "Up")
    }

    @Test("The built-in strings come from the strings table")
    func strings() {
        #expect(MonarchAiInsightCalloutStrings.evidence == "Show evidence")
        #expect(MonarchAiInsightCalloutStrings.dismiss == "Dismiss insight")
        #expect(MonarchAiInsightCalloutStrings.titled(tone: "Info", title: "T") == "Info: T")
    }

    @Test("Evidence and an action add room")
    @MainActor
    func parts() {
        let plain = Host.layout(MonarchAiInsightCallout("Title", why: "Why", origin: .ai))
        let full = Host.layout(
            MonarchAiInsightCallout(
                "Title",
                why: "Why",
                origin: .ai,
                actionTitle: "Act",
                action: {},
                evidence: { Text("Data") }
            )
        )
        #expect(full.height > plain.height + MonarchAccessibility.minimumHitTarget)
    }
}
