import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

@Suite("MonarchControlMetrics")
struct ControlMetricsTests {
    @Test("field heights are 40, 44 and 52 pt")
    func fieldHeights() {
        #expect(MonarchControlMetrics.fieldMinHeight(.small) == 40)
        #expect(MonarchControlMetrics.fieldMinHeight(.medium) == 44)
        #expect(MonarchControlMetrics.fieldMinHeight(.large) == 52)
    }

    @Test("the default field is at least the touch target minimum")
    func defaultMeetsTouchTarget() {
        #expect(MonarchControlMetrics.fieldMinHeight(.medium) >= MonarchAccessibility.minimumHitTarget)
    }

    @Test("sizes grow with the step")
    func monotonic() {
        let heights = MonarchFieldSize.allCases.map(MonarchControlMetrics.fieldMinHeight)
        #expect(heights == heights.sorted())
        let padding = MonarchFieldSize.allCases.map(MonarchControlMetrics.fieldHorizontalPadding)
        #expect(padding == [10, 12, 16])
    }

    @Test("progress and spinner sizes follow the spec")
    func feedbackSizes() {
        #expect(MonarchControlMetrics.progressBarSmall == 4)
        #expect(MonarchControlMetrics.progressBarMedium == 8)
        #expect(MonarchControlMetrics.spinnerSmall == 14)
        #expect(MonarchControlMetrics.spinnerMedium == 16)
        #expect(MonarchControlMetrics.spinnerLarge == 24)
        #expect(MonarchControlMetrics.spinnerStroke == 2)
        #expect(MonarchControlMetrics.textAreaMinHeight == 72)
    }

    @Test("the focus ring is 2 pt wide with a 2 pt gap")
    func focusRing() {
        #expect(MonarchControlMetrics.focusRingWidth == 2)
        #expect(MonarchControlMetrics.focusRingOffset == 2)
    }

    @Test("presented surfaces use the one soft md shadow")
    func presentedElevation() {
        #expect(MonarchElevationLevel.presented == .md)
        let layers = MonarchElevation(MonarchTokens.light.shadows).layers(for: .presented)
        #expect(!layers.isEmpty)
        for layer in layers {
            #expect(layer.offsetY <= 4)
        }
    }
}
