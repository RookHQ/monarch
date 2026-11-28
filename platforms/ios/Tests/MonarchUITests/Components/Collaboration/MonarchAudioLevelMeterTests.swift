import Foundation
import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

enum MonarchAudioLevelMeterFixtures: ComponentFixtures {
    static let id = "audio-level-meter"
    static let fixtures = [
        Fixture("segments: levels, sizes and counts") {
            VStack(alignment: .leading) {
                ForEach(MonarchAudioLevelMeterSize.allCases, id: \.self) { size in
                    HStack {
                        ForEach([0, 0.05, 0.3, 0.6, 1], id: \.self) { level in
                            MonarchAudioLevelMeter(level, size: size)
                        }
                    }
                }
                HStack {
                    MonarchAudioLevelMeter(0.5, segments: 1)
                    MonarchAudioLevelMeter(0.5, segments: 12)
                    MonarchAudioLevelMeter(0.5, segments: 0)
                }
            }
        },
        Fixture("bar") {
            VStack {
                MonarchAudioLevelMeter(0, variant: .bar)
                MonarchAudioLevelMeter(0.6, label: "Speaker level", variant: .bar)
                MonarchAudioLevelMeter(1, variant: .bar)
            }
        },
        Fixture("muted, decorative and out of range") {
            VStack(alignment: .leading) {
                MonarchAudioLevelMeter(0.8, isMuted: true, mutedLabel: "Microphone off")
                MonarchAudioLevelMeter(0.8, variant: .bar, isMuted: true)
                MonarchAudioLevelMeter(0.8, isDecorative: true)
                MonarchAudioLevelMeter(7)
                MonarchAudioLevelMeter(-1, variant: .bar)
                MonarchAudioLevelMeter(.nan)
            }
        },
    ]
}

@Suite("MonarchAudioLevelMeter")
struct MonarchAudioLevelMeterTests {
    private let english = Locale(identifier: "en_US")
    private let metrics = MonarchAudioLevelMeterTokens.metrics(.md, scale: 1)

    @Test("The level is limited to 0 through 1, and a level that is not finite is silence")
    func limits() {
        #expect(MonarchAudioLevelMeterTokens.state(level: 0.4, isMuted: false, reduceMotion: false).level == 0.4)
        #expect(MonarchAudioLevelMeterTokens.state(level: 7, isMuted: false, reduceMotion: false).level == 1)
        #expect(MonarchAudioLevelMeterTokens.state(level: -1, isMuted: false, reduceMotion: false).level == 0)
        #expect(MonarchAudioLevelMeterTokens.state(level: .nan, isMuted: false, reduceMotion: false).level == 0)
        #expect(MonarchAudioLevelMeterTokens.state(level: .infinity, isMuted: false, reduceMotion: false).level == 0)
    }

    @Test("A muted meter is empty whatever the level")
    func muted() {
        let state = MonarchAudioLevelMeterTokens.state(level: 0.9, isMuted: true, reduceMotion: false)
        #expect(state.level == 0)
        #expect(state.shown == 0)
        #expect(!state.isSpeaking)
        #expect(MonarchAudioLevelMeterTokens.state(level: 0.9, isMuted: true, reduceMotion: true).shown == 0)
    }

    @Test("With Reduce Motion the meter is full while there is sound and empty otherwise")
    func reduceMotion() {
        let quiet = MonarchAudioLevelMeterTokens.state(level: 0.05, isMuted: false, reduceMotion: true)
        #expect(quiet.shown == 0)
        #expect(!quiet.isSpeaking)
        let threshold = MonarchAudioLevelMeterTokens.state(
            level: MonarchAudioLevelMeterTokens.speakingThreshold,
            isMuted: false,
            reduceMotion: true
        )
        #expect(threshold.shown == 1)
        let loud = MonarchAudioLevelMeterTokens.state(level: 0.3, isMuted: false, reduceMotion: true)
        #expect(loud.shown == 1)
        // The spoken value is still the real level.
        #expect(loud.level == 0.3)
        // Without Reduce Motion the meter follows the level.
        #expect(MonarchAudioLevelMeterTokens.state(level: 0.3, isMuted: false, reduceMotion: false).shown == 0.3)
    }

    @Test("VoiceOver reads the level as a whole percentage, or the muted text")
    func accessibilityValue() {
        let state = MonarchAudioLevelMeterTokens.state(level: 0.426, isMuted: false, reduceMotion: true)
        #expect(
            MonarchAudioLevelMeterTokens.accessibilityValue(
                state: state,
                isMuted: false,
                mutedLabel: "Muted",
                locale: english
            ) == "43%"
        )
        #expect(
            MonarchAudioLevelMeterTokens.accessibilityValue(
                state: state,
                isMuted: true,
                mutedLabel: "Microphone off",
                locale: english
            ) == "Microphone off"
        )
    }

    @Test("The number of segments that are on follows the level")
    func litCount() {
        #expect(MonarchAudioLevelMeterTokens.litCount(shown: 0, count: 5) == 0)
        #expect(MonarchAudioLevelMeterTokens.litCount(shown: 0.09, count: 5) == 0)
        #expect(MonarchAudioLevelMeterTokens.litCount(shown: 0.5, count: 5) == 3)
        #expect(MonarchAudioLevelMeterTokens.litCount(shown: 1, count: 5) == 5)
        #expect(MonarchAudioLevelMeterTokens.litCount(shown: 1, count: 1) == 1)
    }

    @Test("Segments that are on rise; segments that are off are dots, so the level does not need colour")
    func segments() {
        let size = CGSize(width: metrics.width(count: 5), height: metrics.height)
        let bars = MonarchAudioLevelMeterTokens.segments(
            count: 5,
            shown: 0.6,
            in: size,
            metrics: metrics,
            isRightToLeft: false
        )
        #expect(bars.map(\.isLit) == [true, true, true, false, false])
        let lit = bars.filter(\.isLit).map(\.frame.height)
        #expect(lit == lit.sorted() && Set(lit).count == lit.count, "segments that are on rise")
        #expect(bars.filter { !$0.isLit }.allSatisfy { $0.frame.height == metrics.segmentWidth })
        // Every segment stands on the bottom edge, inside the canvas, without overlapping the next.
        #expect(bars.allSatisfy { abs($0.frame.maxY - size.height) < 0.001 && $0.frame.minY >= -0.001 })
        #expect(bars.allSatisfy { $0.frame.minX >= -0.001 && $0.frame.maxX <= size.width + 0.001 })
        for (first, second) in zip(bars, bars.dropFirst()) {
            #expect(abs(second.frame.minX - first.frame.maxX - metrics.gap) < 0.001)
        }
        // A full meter reaches the top.
        let full = MonarchAudioLevelMeterTokens.segments(
            count: 5,
            shown: 1,
            in: size,
            metrics: metrics,
            isRightToLeft: false
        )
        #expect(abs((full.last?.frame.minY ?? 1)) < 0.001)
    }

    @Test("Right to left, the segments are mirrored")
    func rightToLeft() {
        let size = CGSize(width: metrics.width(count: 4), height: metrics.height)
        func bars(_ isRightToLeft: Bool) -> [MonarchAudioLevelMeterTokens.Segment] {
            MonarchAudioLevelMeterTokens.segments(
                count: 4,
                shown: 0.5,
                in: size,
                metrics: metrics,
                isRightToLeft: isRightToLeft
            )
        }
        let leading = bars(false)
        let mirrored = bars(true)
        for (first, second) in zip(leading, mirrored) {
            #expect(abs(second.frame.minX - (size.width - first.frame.maxX)) < 0.001)
            #expect(second.frame.height == first.frame.height)
            #expect(second.isLit == first.isLit)
        }
        // The first segment, the shortest, is at the right edge.
        #expect(abs((mirrored.first?.frame.maxX ?? 0) - size.width) < 0.001)
    }

    @Test("The bar fills from the leading edge")
    func barFill() {
        let size = CGSize(width: 100, height: 6)
        #expect(
            MonarchAudioLevelMeterTokens.barFill(shown: 0.25, in: size, isRightToLeft: false)
                == CGRect(x: 0, y: 0, width: 25, height: 6)
        )
        #expect(
            MonarchAudioLevelMeterTokens.barFill(shown: 0.25, in: size, isRightToLeft: true)
                == CGRect(x: 75, y: 0, width: 25, height: 6)
        )
        #expect(MonarchAudioLevelMeterTokens.barFill(shown: 3, in: size, isRightToLeft: false).width == 100)
        #expect(MonarchAudioLevelMeterTokens.barFill(shown: 0, in: size, isRightToLeft: true).width == 0)
        #expect(MonarchAudioLevelMeterTokens.barRadius(size) == 3)
    }

    @Test("Sizes scale with Dynamic Type, and the small size is smaller")
    func metricsScale() {
        let small = MonarchAudioLevelMeterTokens.metrics(.sm, scale: 1)
        #expect(metrics.height == 16)
        #expect(small.height == 12)
        #expect(small.segmentWidth < metrics.segmentWidth)
        let doubled = MonarchAudioLevelMeterTokens.metrics(.md, scale: 2)
        #expect(doubled.height == 32)
        #expect(doubled.width(count: 5) == metrics.width(count: 5) * 2)
        #expect(metrics.width(count: 1) == metrics.segmentWidth)
        #expect(metrics.cornerRadius == metrics.segmentWidth / 2)
    }

    @Test("A muted meter has no status colour")
    func roles() {
        let live = MonarchAudioLevelMeterTokens.roles(isMuted: false)
        #expect(live.lit == \MonarchColorTokens.successSolid)
        #expect(live.unlit == \MonarchColorTokens.borderStrong)
        let muted = MonarchAudioLevelMeterTokens.roles(isMuted: true)
        #expect(muted.lit == \MonarchColorTokens.textDisabled)
        #expect(muted.unlit == \MonarchColorTokens.bgMuted)
    }

    @Test("The built-in strings come from the component's strings table")
    func strings() {
        #expect(MonarchAudioLevelMeterStrings.label == "Microphone level")
        #expect(MonarchAudioLevelMeterStrings.muted == "Muted")
    }
}
