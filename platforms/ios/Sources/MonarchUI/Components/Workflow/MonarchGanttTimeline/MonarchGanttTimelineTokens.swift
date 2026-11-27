import MonarchTokens
import SwiftUI

/// The colour roles of a bar.
struct MonarchGanttTimelineBarColors: Hashable {
    /// The fill, a light tint of the tone.
    let container: MonarchColorRole
    /// The label on the fill.
    let content: MonarchColorRole
    /// The outline, in the tone's strong fill.
    let border: MonarchColorRole
    /// The progress strip.
    let progress: MonarchColorRole
    /// Whether the outline is dashed (a conflict).
    let isDashed: Bool
}

/// The colour roles of a milestone diamond.
struct MonarchGanttTimelineMilestoneColors: Hashable {
    let fill: MonarchColorRole
    let border: MonarchColorRole
}

/// Where a bar goes in its row: the visible bar and the touch area around
/// it, which is at least 44 points wide.
struct MonarchGanttTimelineBarBox: Hashable, Sendable {
    let visibleLeft: CGFloat
    let visibleWidth: CGFloat
    let nodeLeft: CGFloat
    let nodeWidth: CGFloat
}

/// Component tokens and logic of ``MonarchGanttTimeline``.
enum MonarchGanttTimelineTokens {
    /// The glyph in a bar with a scheduling conflict.
    static let conflictSymbol = "exclamationmark.triangle"
    /// The gap between that glyph and the bar's label.
    static let barGlyphSpacing: CGFloat = MonarchTokens.Space.space1

    static let spacing: CGFloat = MonarchTokens.Space.space3
    static let rowMinHeight: CGFloat = MonarchAccessibility.minimumHitTarget
    static let headerMinHeight: CGFloat = MonarchTokens.Space.space12
    static let labelMaxWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space16
    static let labelPadding: CGFloat = MonarchTokens.Space.space2
    static let labelVerticalPadding: CGFloat = MonarchTokens.Space.space1
    static let depthIndent: CGFloat = MonarchTokens.Space.space4
    /// The deepest nesting that still indents a row label.
    static let maxIndentDepth = 3
    static let barInset: CGFloat = MonarchTokens.Space.space2
    static let barPadding: CGFloat = MonarchTokens.Space.space1
    static let barMinWidth: CGFloat = MonarchTokens.Space.space1_5
    static let barRadius: CGFloat = MonarchTokens.Radius.sm
    static let pointSize: CGFloat = MonarchTokens.Space.space4
    static let diamondSize: CGFloat = MonarchTokens.Space.space3
    static let diamondBorder: CGFloat = MonarchTokens.Space.space0_5
    static let edgeZone: CGFloat = MonarchTokens.Space.space3
    static let progressHeight: CGFloat = MonarchTokens.Space.space1
    /// How strongly the done part of a bar is filled.
    static let progressOpacity: Double = 0.35
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    static let conflictBorderWidth: CGFloat = MonarchTokens.Space.space0_5
    static let dashLength: CGFloat = MonarchTokens.Space.space1
    static let lineWidth: CGFloat = MonarchTokens.Space.px
    static let todayLineWidth: CGFloat = MonarchTokens.Space.space0_5
    static let arrowStroke: CGFloat = MonarchTokens.Space.space0_5
    static let arrowGap: CGFloat = MonarchTokens.Space.space2_5
    static let arrowTip: CGFloat = MonarchTokens.Space.space0_5
    static let arrowHead: CGFloat = MonarchTokens.Space.space1_5
    static let spanHeight: CGFloat = MonarchTokens.Space.space2
    static let tickPadding: CGFloat = MonarchTokens.Space.space1
    static let laneGap: CGFloat = MonarchTokens.Space.space1
    static let todayLabelPadding: CGFloat = MonarchTokens.Space.space1
    static let todayLabelRadius: CGFloat = MonarchTokens.Radius.xs
    static let offscreenPadding: CGFloat = MonarchTokens.Space.space2
    static let listIndent: CGFloat = MonarchTokens.Space.space4
    static let listGap: CGFloat = MonarchTokens.Space.space1
    static let listRowPadding: CGFloat = MonarchTokens.Space.space2
    /// The share of the width the row-label column takes at most.
    static let labelFraction: CGFloat = 0.36
    /// Bars at least this many edge zones wide show their label.
    static let labelMinEdges: CGFloat = 5
    /// The rotation that makes a square a diamond.
    static let diamondDegrees: Double = 45
    /// The width of a day at the year zoom, in hairlines.
    static let yearDayWidth: CGFloat = 1.6
    /// The coordinate space of the grid, which drags are measured in.
    static let gridSpace = "MonarchGanttTimelineGrid"
    /// The type role of row labels and bar labels.
    static let rowLabelStyle: MonarchTextStyle = .label
    static let captionStyle: MonarchTextStyle = .caption

    /// The width of a day at `zoom`.
    static func dayWidth(_ zoom: MonarchGanttTimelineZoom) -> CGFloat {
        switch zoom {
        case .week: MonarchTokens.Space.space10
        case .month: MonarchTokens.Space.space4
        case .quarter: MonarchTokens.Space.space1 + MonarchTokens.Space.px
        case .year: MonarchTokens.Space.px * yearDayWidth
        }
    }

    /// How many days the window shows at `zoom`.
    static func span(_ zoom: MonarchGanttTimelineZoom) -> Int {
        switch zoom {
        case .week: 28
        case .month: 91
        case .quarter: 273
        case .year: 730
        }
    }

    /// How many days Earlier and Later move the window at `zoom`.
    static func step(_ zoom: MonarchGanttTimelineZoom) -> Int {
        switch zoom {
        case .week: 7
        case .month: 30
        case .quarter: 91
        case .year: 182
        }
    }

    /// Whether gridlines and weekends follow the days (week and month zoom) or the months.
    static func usesMinorGrid(_ zoom: MonarchGanttTimelineZoom) -> Bool {
        zoom == .week || zoom == .month
    }

    /// The width of the row-label column in a chart `width` wide.
    static func labelWidth(_ width: CGFloat) -> CGFloat {
        guard width > 0 else { return labelMaxWidth }
        return min(labelMaxWidth, width * labelFraction)
    }

    /// The leading padding of a row label at a nesting depth; deep levels stop indenting.
    static func indent(depth: Int) -> CGFloat {
        labelPadding + depthIndent * CGFloat(min(max(depth, 0), maxIndentDepth))
    }

    /// Where the bar of `span` goes: a bar spans its days; a one-day marker
    /// is a diamond centred on its day. The touch area is at least 44 points wide.
    static func barBox(
        _ span: MonarchGanttTimelineSpan,
        isPoint: Bool,
        scale: MonarchGanttTimelineScale
    ) -> MonarchGanttTimelineBarBox {
        let visibleWidth = isPoint ? pointSize : max(CGFloat(span.days) * scale.dayWidth, barMinWidth)
        let visibleLeft =
            isPoint ? scale.x(span.start) + scale.dayWidth / 2 - visibleWidth / 2 : scale.x(span.start)
        let nodeWidth = max(visibleWidth, rowMinHeight)
        return MonarchGanttTimelineBarBox(
            visibleLeft: visibleLeft,
            visibleWidth: visibleWidth,
            nodeLeft: visibleLeft - (nodeWidth - visibleWidth) / 2,
            nodeWidth: nodeWidth
        )
    }

    /// Whether a bar `width` wide is wide enough to show its label.
    static func showsBarLabel(width: CGFloat) -> Bool {
        width >= edgeZone * labelMinEdges
    }

    /// A bar: a light tint of its tone with an outline and progress strip in
    /// the tone's solid fill, the label in the default text colour. `nil` is
    /// the accent colour, neutral uses the muted background and the strong
    /// border, and a conflict uses danger with a dashed outline.
    static func barColors(tone: MonarchTone?, hasConflict: Bool) -> MonarchGanttTimelineBarColors {
        let shown = hasConflict ? .danger : tone
        let soft: MonarchColorRole
        let fill: MonarchColorRole
        switch shown {
        case nil:
            soft = \.accentSubtle
            fill = \.accentSolid
        case .neutral?:
            soft = \.bgMuted
            fill = \.borderStrong
        case let tone?:
            let palette = MonarchTonePalette.palette(for: tone)
            soft = palette.subtle
            fill = palette.solid
        }
        return MonarchGanttTimelineBarColors(
            container: soft,
            content: \.textDefault,
            border: fill,
            progress: fill,
            isDashed: hasConflict
        )
    }

    /// A milestone: filled with the accent when done, otherwise the surface with a muted outline.
    static func milestoneColors(isDone: Bool) -> MonarchGanttTimelineMilestoneColors {
        isDone
            ? MonarchGanttTimelineMilestoneColors(fill: \.accentSolid, border: \.accentSolid)
            : MonarchGanttTimelineMilestoneColors(fill: \.bgSurface, border: \.textMuted)
    }

    /// A dependency arrow: the subtle text colour, or the strong danger colour when it is broken.
    static func dependencyColor(isConflict: Bool) -> MonarchColorRole {
        isConflict ? \.dangerStrong : \.textSubtle
    }

    static var surfaceColor: MonarchColorRole { \.bgSurface }
    /// Gridlines, row lines and the outline.
    static var lineColor: MonarchColorRole { \.borderSubtle }
    /// Weekends, group rows and the header.
    static var shadeColor: MonarchColorRole { \.bgSubtle }
    static var todayColor: MonarchColorRole { \.accentSolid }
    static var onTodayColor: MonarchColorRole { \.accentOnSolid }
    /// The bracket of a parent without dates and the outline of a group's span.
    static var bracketColor: MonarchColorRole { \.borderStrong }
    static var groupSpanColor: MonarchColorRole { \.bgMuted }
    /// Header ticks, captions and row texts such as "No dates".
    static var mutedColor: MonarchColorRole { \.textMuted }
    static var textColor: MonarchColorRole { \.textDefault }
    /// The parent's Show or Hide caption.
    static var linkColor: MonarchColorRole { \.textLink }
    static var conflictColor: MonarchColorRole { \.dangerStrong }
}
