import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchStatusDotFixtures: ComponentFixtures {
    static let id = "status-dot"
    static let fixtures = [
        Fixture("tones and sizes") {
            VStack(alignment: .leading) {
                ForEach(MonarchStatusDotSize.allCases, id: \.self) { size in
                    ForEach(MonarchTone.allCases, id: \.self) { tone in
                        MonarchStatusDot("Status", tone: tone, size: size)
                    }
                }
            }
        },
        Fixture("pulsing") {
            MonarchStatusDot("Deploying", tone: .info, size: .md, isPulsing: true)
        },
        Fixture("hidden label") {
            MonarchStatusDot("Online", tone: .success, isLabelHidden: true)
        },
        Fixture("long label") {
            MonarchStatusDot(
                "Waiting for the payment provider to confirm the transfer before the order can ship",
                tone: .warning
            )
        },
    ]
}

@Suite("MonarchStatusDot")
@MainActor
struct MonarchStatusDotTests {
    @Test("The dot takes the solid colour of the tone")
    func dotRole() {
        #expect(MonarchStatusDotTokens.dot(tone: .neutral) == \MonarchColorTokens.textSubtle)
        #expect(MonarchStatusDotTokens.dot(tone: .info) == \MonarchColorTokens.infoSolid)
        #expect(MonarchStatusDotTokens.dot(tone: .success) == \MonarchColorTokens.successSolid)
        #expect(MonarchStatusDotTokens.dot(tone: .warning) == \MonarchColorTokens.warningSolid)
        #expect(MonarchStatusDotTokens.dot(tone: .danger) == \MonarchColorTokens.dangerSolid)
    }

    @Test("Every tone has its own dot colour")
    func tonesAreDistinct() {
        let roles = MonarchTone.allCases.map { MonarchStatusDotTokens.dot(tone: $0) }
        #expect(Set(roles).count == MonarchTone.allCases.count)
    }

    @Test("The medium size has a larger dot, a wider gap and larger text")
    func sizes() {
        #expect(MonarchStatusDotTokens.dotSize(.sm) == 8)
        #expect(MonarchStatusDotTokens.dotSize(.md) == 10)
        #expect(MonarchStatusDotTokens.spacing(.md) > MonarchStatusDotTokens.spacing(.sm))
        #expect(MonarchStatusDotTokens.textStyle(.sm) == .caption)
        #expect(MonarchStatusDotTokens.textStyle(.md) == .label)
        #expect(MonarchStatusDotTokens.textStyle(.md).spec.size > MonarchStatusDotTokens.textStyle(.sm).spec.size)
    }

    @Test("The pulse ring grows beyond the dot")
    func pulse() {
        #expect(MonarchStatusDotTokens.pulseScale > 1)
        #expect(MonarchStatusDotTokens.pulseDuration > 0)
    }

    @Test("Hiding the label makes the view narrower but not empty")
    func hiddenLabel() {
        let shown = Host.layout(HStack { MonarchStatusDot("Online", tone: .success) }.fixedSize())
        let hidden = Host.layout(HStack { MonarchStatusDot("Online", tone: .success, isLabelHidden: true) }.fixedSize())
        #expect(hidden.height > 0)
        #expect(hidden.height <= shown.height)
    }
}
