import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchAiGeneratedBadge`` and ``MonarchAiBotGlyph``.
enum MonarchAiGeneratedBadgeTokens {
    /// The type role of the label, the same as a badge.
    static let textStyle: MonarchTextStyle = MonarchBadgeTokens.textStyle
    /// The weight of the label, the same as a badge.
    static let fontWeight = MonarchBadgeTokens.fontWeight
    /// The gap between the glyph and the label.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the content.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space0_5
    /// Padding around the glyph of an icon-only badge, so the badge is round.
    static let iconOnlyPadding: CGFloat = MonarchTokens.Space.space1
    /// The width of the border.
    static let borderWidth: CGFloat = MonarchTokens.Space.px
    /// The length of a dash and of a gap of the dashed border.
    static let dash: CGFloat = MonarchTokens.Space.space0_5

    /// Padding before and after the content.
    static func horizontalPadding(_ size: MonarchAiGeneratedBadgeSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.Space.space1_5
        case .md: MonarchTokens.Space.space2
        }
    }

    /// The point size of the glyph before Dynamic Type is applied.
    static func glyphSize(_ size: MonarchAiGeneratedBadgeSize) -> CGFloat {
        switch size {
        case .sm: MonarchTokens.FontSize.xs
        case .md: MonarchTokens.FontSize.sm
        }
    }

    /// The colour roles of the badge: the AI intent colours in every kind.
    static var appearance: MonarchSurfaceAppearance {
        MonarchSurfaceAppearance(background: \.aiSubtle, foreground: \.aiStrong, border: \.aiBorder)
    }

    /// Whether the border is dashed. "Edited after AI" is told apart by shape as well as by its label.
    static func isDashed(_ kind: MonarchAiGeneratedBadgeKind) -> Bool {
        kind == .edited
    }

    /// The provenance line from the model name and the formatted time. Empty when neither is known.
    static func details(model: String?, time: String?) -> String {
        let model = model.flatMap { $0.isEmpty ? nil : $0 }
        let time = time.flatMap { $0.isEmpty ? nil : $0 }
        switch (model, time) {
        case (let model?, let time?): return MonarchAiGeneratedBadgeStrings.byModel(model, at: time)
        case (let model?, nil): return MonarchAiGeneratedBadgeStrings.byModel(model)
        case (nil, let time?): return time
        case (nil, nil): return ""
        }
    }

    /// The time as a medium date and a short time.
    static func formattedTime(_ date: Date, locale: Locale, calendar: Calendar, timeZone: TimeZone) -> String {
        date.formatted(
            Date.FormatStyle(
                date: .abbreviated,
                time: .omitted,
                locale: locale,
                calendar: calendar,
                timeZone: timeZone
            ).monarchTime()
        )
    }

    /// The text of the pointer tooltip. An icon-only badge also names itself there.
    static func help(label: String, details: String, isIconOnly: Bool) -> String {
        guard isIconOnly else { return details }
        return details.isEmpty ? label : "\(label). \(details)"
    }
}

/// The geometry of ``MonarchAiBotGlyph`` on a 16-unit grid: a robot head.
enum MonarchAiBotGlyphTokens {
    /// The side of the grid the glyph is drawn on.
    static let grid: CGFloat = 16
    /// The stroke width as a share of the glyph size.
    static let strokeRatio: CGFloat = 1.5 / 16
    /// The head.
    static let head = CGRect(x: 3, y: 5, width: 10, height: 8)
    /// The corner radius of the head.
    static let headRadius: CGFloat = 2
    /// The antenna, the two ears and the two eyes, each a line from one point to another.
    static let lines: [(from: CGPoint, to: CGPoint)] = [
        (CGPoint(x: 8, y: 2.5), CGPoint(x: 8, y: 5)),
        (CGPoint(x: 1.5, y: 9), CGPoint(x: 3, y: 9)),
        (CGPoint(x: 13, y: 9), CGPoint(x: 14.5, y: 9)),
        (CGPoint(x: 6, y: 8.25), CGPoint(x: 6, y: 9.5)),
        (CGPoint(x: 10, y: 8.25), CGPoint(x: 10, y: 9.5)),
    ]
}

/// The outline of the AI identity mark, scaled to its frame.
struct MonarchAiBotGlyphShape: Shape {
    func path(in rect: CGRect) -> Path {
        let unit = min(rect.width, rect.height) / MonarchAiBotGlyphTokens.grid
        let origin = CGPoint(
            x: rect.midX - unit * MonarchAiBotGlyphTokens.grid / 2,
            y: rect.midY - unit * MonarchAiBotGlyphTokens.grid / 2
        )
        func point(_ point: CGPoint) -> CGPoint {
            CGPoint(x: origin.x + point.x * unit, y: origin.y + point.y * unit)
        }
        let head = MonarchAiBotGlyphTokens.head
        let radius = MonarchAiBotGlyphTokens.headRadius * unit
        var path = Path()
        path.addRoundedRect(
            in: CGRect(origin: point(head.origin), size: CGSize(width: head.width * unit, height: head.height * unit)),
            cornerSize: CGSize(width: radius, height: radius)
        )
        for line in MonarchAiBotGlyphTokens.lines {
            path.move(to: point(line.from))
            path.addLine(to: point(line.to))
        }
        return path
    }
}
