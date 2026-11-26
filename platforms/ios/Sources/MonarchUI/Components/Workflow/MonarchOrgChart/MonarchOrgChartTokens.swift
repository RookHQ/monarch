import CoreGraphics
import MonarchTokens
import SwiftUI

/// Sizes, colours, zoom steps and the spoken text of ``MonarchOrgChart``.
/// Every decision is a pure function, unit-tested in `MonarchOrgChartTests`.
enum MonarchOrgChartTokens {
    // MARK: Sizes

    /// The card width before Dynamic Type: 14rem, as on the web.
    static let cardWidth: CGFloat = MonarchTokens.Space.space24 * 2 + MonarchTokens.Space.space8
    /// Padding inside a card.
    static let cardPadding: CGFloat = MonarchTokens.Space.space3
    /// Space between the avatar and the text, and between text lines.
    static let cardSpacing: CGFloat = MonarchTokens.Space.space2
    static let textSpacing: CGFloat = MonarchTokens.Space.space0_5
    static let cardRadius: CGFloat = MonarchTokens.Radius.md
    /// The halo around the focused card.
    static let haloWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// Space between sibling subtrees.
    static let siblingGap: CGFloat = MonarchTokens.Space.space4
    /// The length of each connector segment, above and below the chip.
    static let connectorLength: CGFloat = MonarchTokens.Space.space4
    static let lineWidth: CGFloat = MonarchTokens.Space.px
    /// The vacancy icon tile.
    static let vacancyTile: CGFloat = MonarchTokens.Control.sm
    static let vacancyRadius: CGFloat = MonarchTokens.Radius.sm
    /// The dash of a dashed card border and of the vacancy tile.
    static let borderDash: [CGFloat] = [
        MonarchTokens.Space.space1, MonarchTokens.Space.space0_5 + MonarchTokens.Space.px,
    ]
    /// The dotted reporting line: 6 on, 4 off, 1.5 wide, as on the web.
    static let dottedDash: [CGFloat] = [MonarchTokens.Space.space1_5, MonarchTokens.Space.space1]
    static let dottedWidth: CGFloat = MonarchTokens.Space.px * 3 / 2
    /// The dot at the report's end of a dotted line.
    static let dottedDot: CGFloat = MonarchTokens.Space.space1_5
    /// The smallest vertical pull of a dotted curve's control points.
    static let dottedMinimumPull: CGFloat = MonarchTokens.Space.space8
    /// The chip that opens a team.
    static let chipPadding: CGFloat = MonarchTokens.Space.space2
    static let chipSpacing: CGFloat = MonarchTokens.Space.space1
    /// The indent of each level in the list.
    static let indent: CGFloat = MonarchTokens.Space.space6
    static let listRowSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding around the chart inside its scroll view.
    static let canvasPadding: CGFloat = MonarchTokens.Space.space6
    static let toolbarSpacing: CGFloat = MonarchTokens.Space.space2
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space3

    /// The list stops indenting after this many steps, so deep teams keep room on a phone.
    static let maximumIndentSteps = 4

    /// The indent steps of a list row at a level (1 for top level).
    static func indentSteps(level: Int) -> Int {
        min(max(level - 1, 0), maximumIndentSteps)
    }

    /// Top-level nodes up to this many also open their first level by default.
    static let defaultSecondLevelRootLimit = 3

    // MARK: Symbols

    static let vacancySymbol = "briefcase"
    /// A forward chevron on a closed team and a down chevron on an open one.
    /// Separate symbols rather than a rotation, so right to left stays correct.
    static func chevronSymbol(isExpanded: Bool) -> String {
        isExpanded ? "chevron.down" : "chevron.forward"
    }
    static let zoomInSymbol = "plus.magnifyingglass"
    static let zoomOutSymbol = "minus.magnifyingglass"
    static let zoomResetSymbol = "arrow.counterclockwise"
    static let moveSymbol = "arrow.up.and.down.and.arrow.left.and.right"

    // MARK: Zoom

    static let zoomMinimum: CGFloat = 0.4
    static let zoomMaximum: CGFloat = 1.6
    static let zoomStep: CGFloat = 0.1

    /// The side of a chip's touch target in the chart's own points. The chart
    /// is drawn at `zoom`, so a target below 100% grows by the inverse to stay
    /// 44 points on screen; above 100% it stays 44 chart points, which is
    /// more on screen.
    static func chipTarget(zoom: CGFloat) -> CGFloat {
        MonarchAccessibility.minimumHitTarget / min(1, max(zoomMinimum, zoom))
    }

    /// A zoom kept between 40% and 160% and rounded to a tenth.
    static func clampedZoom(_ zoom: CGFloat) -> CGFloat {
        let rounded = (zoom / zoomStep).rounded() * zoomStep
        return min(zoomMaximum, max(zoomMinimum, rounded))
    }

    static func zoomedIn(_ zoom: CGFloat) -> CGFloat { clampedZoom(zoom + zoomStep) }
    static func zoomedOut(_ zoom: CGFloat) -> CGFloat { clampedZoom(zoom - zoomStep) }
    static func canZoomIn(_ zoom: CGFloat) -> Bool { zoom < zoomMaximum - zoomStep / 2 }
    static func canZoomOut(_ zoom: CGFloat) -> Bool { zoom > zoomMinimum + zoomStep / 2 }
    static func canResetZoom(_ zoom: CGFloat) -> Bool { abs(zoom - 1) > zoomStep / 2 }
    static func zoomPercent(_ zoom: CGFloat) -> Int { Int((zoom * 100).rounded()) }

    // MARK: Layout

    /// The layout drawn: `auto` gives the list in a compact width or at an
    /// accessibility text size, and the chart otherwise.
    static func resolvedLayout(
        _ layout: MonarchOrgChartLayout,
        isRegularWidth: Bool,
        isAccessibilitySize: Bool
    ) -> MonarchOrgChartLayout {
        switch layout {
        case .chart, .list: layout
        case .auto: isRegularWidth && !isAccessibilitySize ? .chart : .list
        }
    }

    /// Which halves of the horizontal connector a child draws: the first
    /// child has none toward the leading edge, the last none toward the trailing edge.
    static func connectorHalves(index: Int, count: Int) -> (leading: Bool, trailing: Bool) {
        (leading: index > 0, trailing: index < count - 1)
    }

    // MARK: Colours

    /// How a card is drawn.
    struct CardAppearance: Equatable {
        let fill: MonarchColorRole
        let border: MonarchColorRole
        let isDashed: Bool
        /// The halo of the focused card, if any.
        let halo: MonarchColorRole?
        let name: MonarchColorRole
        let secondary: MonarchColorRole
    }

    static func cardAppearance(
        kind: MonarchOrgChartNodeKind,
        isDashed: Bool,
        isFocused: Bool,
        isDropTarget: Bool,
        isEnabled: Bool
    ) -> CardAppearance {
        let fill: MonarchColorRole =
            isDropTarget ? \.accentSubtle : kind == .vacancy ? \.bgMuted : \.bgSurface
        let border: MonarchColorRole =
            isDropTarget
            ? \.accentStrong
            : isFocused ? \.accentSolid : (isDashed || kind == .vacancy) ? \.borderStrong : \.borderSubtle
        return CardAppearance(
            fill: fill,
            border: isEnabled ? border : \.borderSubtle,
            isDashed: isDashed || kind == .vacancy,
            halo: isFocused && isEnabled ? \.accentSubtle : nil,
            name: isEnabled ? \.textDefault : \.textDisabled,
            secondary: isEnabled ? \.textMuted : \.textDisabled
        )
    }

    /// The connector lines between managers and reports.
    static var lineColor: MonarchColorRole { \.borderDefault }
    /// The dotted secondary reporting lines.
    static var dottedColor: MonarchColorRole { \.textSubtle }
    /// The chip's fill, border and text.
    static var chipFill: MonarchColorRole { \.bgSurface }
    static var chipBorder: MonarchColorRole { \.borderDefault }
    static func chipText(isEnabled: Bool) -> MonarchColorRole { isEnabled ? \.textMuted : \.textDisabled }
    /// The vacancy tile.
    static var vacancyTileFill: MonarchColorRole { \.bgSurface }
    static var vacancyTileForeground: MonarchColorRole { \.textMuted }
    static var hintColor: MonarchColorRole { \.textMuted }

    // MARK: Accessibility

    /// What VoiceOver reads as the name of a card: name, title, meta, and the
    /// facts the card shows with shape (open position, dashed line, dotted lines).
    static func accessibilityLabel(
        _ node: MonarchOrgChartNode,
        dottedManagers: [String],
        labels: MonarchOrgChartLabels
    ) -> String {
        var parts = [node.name]
        if let title = nonBlank(node.title) { parts.append(title) }
        if let meta = nonBlank(node.meta) { parts.append(meta) }
        if node.kind == .vacancy { parts.append(labels.vacant ?? MonarchOrgChartStrings.vacant) }
        if node.isDashed { parts.append(labels.dashed ?? MonarchOrgChartStrings.dashed) }
        for manager in dottedManagers {
            parts.append(labels.alsoReportsTo?(manager) ?? MonarchOrgChartStrings.alsoReportsTo(manager))
        }
        return parts.joined(separator: ", ")
    }

    /// What VoiceOver reads as the value: the team's state and size, or
    /// nothing for a person without reports.
    static func accessibilityValue(
        reportCount: Int,
        hasReports: Bool,
        isExpanded: Bool,
        isLoading: Bool,
        labels: MonarchOrgChartLabels
    ) -> String {
        guard hasReports else { return "" }
        let state: String
        if isLoading {
            state = labels.loading ?? MonarchOrgChartStrings.loading
        } else if isExpanded {
            state = labels.expanded ?? MonarchOrgChartStrings.expanded
        } else {
            state = labels.collapsed ?? MonarchOrgChartStrings.collapsed
        }
        guard reportCount > 0 else { return state }
        return [state, reportsText(reportCount, labels: labels)].joined(separator: ", ")
    }

    static func reportsText(_ count: Int, labels: MonarchOrgChartLabels) -> String {
        labels.reports?(count) ?? MonarchOrgChartStrings.reports(count)
    }

    static func zoomLevelText(_ zoom: CGFloat, labels: MonarchOrgChartLabels) -> String {
        let percent = zoomPercent(zoom)
        return labels.zoomLevel?(percent) ?? MonarchOrgChartStrings.zoomLevel(percent)
    }

    /// The name of the accessibility action that opens or closes a team, if any.
    static func toggleActionName(
        hasReports: Bool,
        isExpanded: Bool,
        isLoading: Bool,
        labels: MonarchOrgChartLabels
    ) -> String? {
        guard hasReports, !isLoading else { return nil }
        return isExpanded
            ? (labels.collapse ?? MonarchOrgChartStrings.collapse)
            : (labels.expand ?? MonarchOrgChartStrings.expand)
    }

    /// The text, or `nil` when it is missing or blank.
    static func nonBlank(_ text: String?) -> String? {
        guard let text, !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        return text
    }

    // MARK: Drag and drop

    private static let payloadPrefix = "monarch.orgChart:"

    /// The text dragged for a card, marked so drops from elsewhere are ignored.
    static func dragPayload(_ id: String) -> String { payloadPrefix + id }

    /// The id in a dragged payload, or `nil` for text that did not come from a chart.
    static func movingID(fromPayload payload: String) -> String? {
        guard payload.hasPrefix(payloadPrefix) else { return nil }
        let id = String(payload.dropFirst(payloadPrefix.count))
        return id.isEmpty ? nil : id
    }

    // MARK: Dotted lines

    /// A cubic curve from the bottom centre of the manager's card to the top
    /// centre of the report's card, as on the web.
    struct Curve: Equatable {
        let start: CGPoint
        let control1: CGPoint
        let control2: CGPoint
        let end: CGPoint
    }

    static func dottedCurve(manager: CGRect, report: CGRect) -> Curve {
        let start = CGPoint(x: manager.midX, y: manager.maxY)
        let end = CGPoint(x: report.midX, y: report.minY)
        let pull = max(dottedMinimumPull, abs(end.y - start.y) / 2)
        return Curve(
            start: start,
            control1: CGPoint(x: start.x, y: start.y + pull),
            control2: CGPoint(x: end.x, y: end.y - pull),
            end: end
        )
    }
}
