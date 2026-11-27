import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchLabeledContentStyleFixtures: ComponentFixtures {
    static let id = "description-list"
    static let fixtures = [
        Fixture("stacked") {
            VStack(alignment: .leading) {
                LabeledContent("Owner", value: "Ada Lovelace")
                LabeledContent("Stage") { MonarchBadge("Negotiation", tone: .info) }
                LabeledContent(
                    "Notes",
                    value: "Renewal depends on the security review, which the customer moved to the second week of May."
                )
            }
            .labeledContentStyle(.monarch)
        },
        Fixture("inline, short and long") {
            VStack(alignment: .leading) {
                LabeledContent("Created", value: "12 March")
                LabeledContent("Billing address", value: "Flat 4, 221 Long Street Name, Manchester M1 1AA")
            }
            .labeledContentStyle(.monarch(.inline))
        },
        Fixture("copyable and truncated values") {
            VStack(alignment: .leading) {
                LabeledContent("Customer ID") {
                    HStack {
                        Text("cus_01HZX4Q8").monarchTextStyle(.code)
                        MonarchCopyButton(
                            value: "cus_01HZX4Q8",
                            target: "Customer ID",
                            variant: .ghost,
                            size: .sm,
                            isIconOnly: true
                        )
                    }
                }
                LabeledContent("Description") {
                    MonarchTruncatedText(
                        "A long description that is clamped to one line with a button that shows the rest of it.",
                        lineLimit: 1
                    )
                }
            }
            .labeledContentStyle(.monarch)
        },
        Fixture("disabled") {
            LabeledContent("Owner", value: "Ada Lovelace")
                .labeledContentStyle(.monarch(.inline))
                .disabled(true)
        },
    ]
}

@Suite("MonarchLabeledContentStyle")
struct MonarchLabeledContentStyleTests {
    @Test("The label is muted and the value default; disabled uses the disabled role")
    func colours() {
        #expect(MonarchLabeledContentTokens.label(isEnabled: true) == \MonarchColorTokens.textMuted)
        #expect(MonarchLabeledContentTokens.value(isEnabled: true) == \MonarchColorTokens.textDefault)
        #expect(MonarchLabeledContentTokens.label(isEnabled: false) == \MonarchColorTokens.textDisabled)
        #expect(MonarchLabeledContentTokens.value(isEnabled: false) == \MonarchColorTokens.textDisabled)
    }

    @Test("Type roles: caption for the label, body for the value")
    func typeRoles() {
        #expect(MonarchLabeledContentTokens.labelStyle == .caption)
        #expect(MonarchLabeledContentTokens.valueStyle == .body)
    }

    @Test("An inline value aligns to the trailing edge; a stacked one to the leading edge")
    func alignment() {
        #expect(MonarchLabeledContentTokens.valueAlignment(.inline) == .trailing)
        #expect(MonarchLabeledContentTokens.valueAlignment(.stacked) == .leading)
    }

    @Test("Inline puts a short label and value on one row, so it is lower than stacked")
    @MainActor
    func inlineIsOneRow() {
        let stacked = Host.layout(LabeledContent("Owner", value: "Ada").labeledContentStyle(.monarch))
        let inline = Host.layout(LabeledContent("Owner", value: "Ada").labeledContentStyle(.monarch(.inline)))
        #expect(inline.height < stacked.height)
    }

    @Test("Inline falls back to stacked when the value does not fit beside the label")
    @MainActor
    func inlineFallsBack() {
        let value = String(repeating: "A long value that cannot share a row. ", count: 4)
        let stacked = Host.layout(LabeledContent("Address", value: value).labeledContentStyle(.monarch))
        let inline = Host.layout(LabeledContent("Address", value: value).labeledContentStyle(.monarch(.inline)))
        #expect(inline.height == stacked.height)
    }
}
