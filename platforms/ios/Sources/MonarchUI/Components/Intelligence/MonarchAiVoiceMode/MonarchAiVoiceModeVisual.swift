import SwiftUI

/// The calm central visual of ``MonarchAiVoiceMode``: a halo that grows with
/// the level, an orb, and a glyph for the state. Decorative: the state text
/// says the same in words.
///
/// The halo, orb and bars are drawn with `Canvas`. They are symmetric, so the
/// drawing needs no mirroring in right-to-left layouts.
struct MonarchAiVoiceModeVisual: View {
    let state: MonarchAiVoiceModeState
    let level: Double
    let size: MonarchAiVoiceModeSize

    @Environment(\.monarchTheme) private var theme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric private var orb: CGFloat

    init(state: MonarchAiVoiceModeState, level: Double, size: MonarchAiVoiceModeSize) {
        self.state = state
        self.level = level
        self.size = size
        _orb = ScaledMetric(wrappedValue: MonarchAiVoiceModeTokens.orbSize(size), relativeTo: .body)
    }

    var body: some View {
        let shown = MonarchAiVoiceModeTokens.shownLevel(level, state: state, reduceMotion: reduceMotion)
        let roles = MonarchAiVoiceModeTokens.visualRoles(state)
        let glyph = MonarchAiVoiceModeTokens.glyph(state)
        let side = orb * (MonarchAiVoiceModeTokens.haloRest + MonarchAiVoiceModeTokens.haloGrowth)
        let halo = theme.colors[role: roles.halo]
        let fill = theme.colors[role: roles.orb]
        let mark = theme.colors[role: roles.glyph]
        let ring = theme.colors[role: roles.ring]
        let orb = orb
        Canvas { context, canvas in
            let center = CGPoint(x: canvas.width / 2, y: canvas.height / 2)
            let haloRadius = orb * MonarchAiVoiceModeTokens.haloScale(level: shown) / 2
            context.fill(circle(center, haloRadius), with: .color(halo))
            context.fill(circle(center, orb / 2), with: .color(fill))
            context.stroke(circle(center, orb / 2 - 1), with: .color(ring), lineWidth: 2)
            if glyph == .bars {
                let heights = MonarchAiVoiceModeTokens.barHeights(level: shown)
                let area = orb * MonarchAiVoiceModeTokens.barArea
                let slot = area / CGFloat(heights.count)
                let width = slot / 2
                for (index, height) in heights.enumerated() {
                    let barHeight = area * height
                    let rect = CGRect(
                        x: center.x - area / 2 + slot * CGFloat(index) + (slot - width) / 2,
                        y: center.y - barHeight / 2,
                        width: width,
                        height: barHeight
                    )
                    context.fill(Path(roundedRect: rect, cornerRadius: width / 2), with: .color(mark))
                }
            }
        }
        .frame(width: side, height: side)
        .overlay {
            switch glyph {
            case .error:
                MonarchIcon(systemName: "exclamationmark", size: .lg).foregroundStyle(mark)
            case .muted:
                MonarchIcon(systemName: "mic.slash", size: .lg).foregroundStyle(mark)
            case .dots:
                MonarchAiTypingDots(isAnnounced: false).foregroundStyle(mark)
            case .bars, .rest:
                EmptyView()
            }
        }
        .monarchAnimation(.fast, value: shown)
        .monarchAnimation(.base, value: state)
        .accessibilityHidden(true)
    }

    private func circle(_ center: CGPoint, _ radius: CGFloat) -> Path {
        Path(ellipseIn: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2))
    }
}
