import MonarchTokens
import SwiftUI
import Testing

@testable import MonarchUI

@Suite("Foundations")
struct FoundationsTests {
    @Test("Durations are the token values in seconds")
    func durations() {
        #expect(MonarchMotionDuration.instant.seconds == 0)
        #expect(MonarchMotionDuration.fast.seconds == 0.12)
        #expect(MonarchMotionDuration.base.seconds == 0.18)
        #expect(MonarchMotionDuration.slow.seconds == 0.26)
        #expect(MonarchMotionDuration.slower.seconds == 0.4)
        #expect(MonarchMotionEasing.standard.curve == MonarchTokens.Easing.standard)
        #expect(MonarchMotionEasing.emphasized.curve == MonarchTokens.Easing.emphasized)
    }

    @Test("Reduce Motion removes the animation")
    func reduceMotion() {
        let motion = MonarchMotion()
        #expect(motion.animation(.base, reduceMotion: true) == nil)
        #expect(motion.animation(.instant, reduceMotion: false) == nil)
        #expect(
            motion.animation(.fast, easing: .enter, reduceMotion: false)
                == Animation.timingCurve(0, 0, 0.2, 1, duration: 0.12)
        )
        // A state change uses the platform spring, with no bounce, at the token duration.
        #expect(motion.animation(.fast, reduceMotion: false) == Animation.snappy(duration: 0.12))
    }

    @Test("Elevation levels map to the theme's shadow layers")
    func elevation() {
        let elevation = MonarchTheme.light.elevation
        #expect(elevation.layers(for: .none).isEmpty)
        #expect(elevation.layers(for: .xs) == MonarchTokens.light.shadows.xs)
        #expect(elevation.layers(for: .md) == MonarchTokens.light.shadows.md)
        #expect(elevation.layers(for: .xl) == MonarchTokens.light.shadows.xl)
        #expect(MonarchTheme.dark.elevation.layers(for: .sm) == MonarchTokens.dark.shadows.sm)
    }

    @Test("The minimum touch target is 44 points")
    func hitTarget() {
        #expect(MonarchAccessibility.minimumHitTarget == 44)
    }

    @Test("The library version is a 0.14 patch version, as monarch-react's")
    func version() {
        // The native version now follows monarch-react (D7): a plain 0.14.N.
        // The value itself is kept in sync by the tooling scripts.
        let parts = MonarchUI.version.split(separator: ".")
        #expect(parts.count == 3)
        #expect(parts.prefix(2) == ["0", "14"])
        #expect(Int(parts.last ?? "") != nil)
    }
}
