import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and the rules of ``MonarchCelebration``.
enum MonarchCelebrationTokens {
    /// The gap between the visual, the title, the message and the actions.
    static let spacing: CGFloat = MonarchTokens.Space.space2
    /// The extra gap above the actions.
    static let actionsTopPadding: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the panel.
    static let verticalPadding: CGFloat = MonarchTokens.Space.space6
    /// Padding before and after the panel.
    static let horizontalPadding: CGFloat = MonarchTokens.Space.space4
    /// The widest the panel grows: 448 points.
    static let maxWidth: CGFloat = MonarchTokens.Space.space16 * 7
    /// The width of the confetti area: 256 points.
    static let visualWidth: CGFloat = MonarchTokens.Space.space16 * 4
    /// The room above the icon where the confetti starts.
    static let visualTopPadding: CGFloat = MonarchTokens.Space.space6
    /// The side of the icon tile at the default text size.
    static let tileSize: CGFloat = MonarchTokens.Space.space16
    /// How far a piece falls.
    static let fallDistance: CGFloat = MonarchTokens.Space.space20
    /// Where a piece starts, above its resting line.
    static let startOffset: CGFloat = -MonarchTokens.Space.space2
    /// The size of a bar piece.
    static let barSize = CGSize(width: MonarchTokens.Space.space1_5, height: MonarchTokens.Space.space3)
    /// The side of a dot piece.
    static let dotSize: CGFloat = MonarchTokens.Space.space1_5
    /// The corner of a bar piece.
    static let barCorner: CGFloat = MonarchTokens.Radius.xs / 2
    /// How long one piece falls, in seconds.
    static let fallDuration: Double = Double(MonarchTokens.Duration.shimmer) / 1000
    /// The turn a piece makes while it falls, in degrees.
    static let fallTurn: Double = 180
    /// The scale the icon tile grows from.
    static let popScale: CGFloat = 0.85
    /// The share of the fall during which a piece fades in.
    static let fadeInEnd: Double = 0.15
    /// The share of the fall after which a piece fades out.
    static let fadeOutStart: Double = 0.7
    /// The opacity of the still pieces under Reduce Motion.
    static let restingOpacity: Double = 0.7
    /// Points of resting offset per millisecond of delay, for the still scatter.
    static let restPerDelay: CGFloat = MonarchTokens.Space.space4 / 100

    /// The shape of a piece.
    enum Shape: Equatable {
        case bar, dot
    }

    /// One piece of confetti: where it starts and how it moves.
    struct Piece: Equatable {
        /// The horizontal position, as a share of the width from the left
        /// edge. Not mirrored: the scatter has no direction.
        let position: CGFloat
        /// The delay before it falls, in milliseconds.
        let delay: Double
        /// Its rotation, in degrees.
        let rotation: Double
        /// How far it drifts sideways while it falls, in points.
        let drift: CGFloat
        /// The index of its data colour, 0 to 4.
        let color: Int
        let shape: Shape
    }

    /// The twelve pieces, fixed so every celebration looks the same.
    static let pieces: [Piece] = [
        Piece(position: 0.06, delay: 0, rotation: 20, drift: -12, color: 0, shape: .bar),
        Piece(position: 0.14, delay: 120, rotation: -35, drift: 10, color: 1, shape: .dot),
        Piece(position: 0.22, delay: 60, rotation: 60, drift: -8, color: 2, shape: .bar),
        Piece(position: 0.31, delay: 200, rotation: -15, drift: 14, color: 3, shape: .bar),
        Piece(position: 0.39, delay: 40, rotation: 45, drift: -10, color: 4, shape: .dot),
        Piece(position: 0.47, delay: 160, rotation: -60, drift: 6, color: 0, shape: .bar),
        Piece(position: 0.55, delay: 20, rotation: 30, drift: -14, color: 1, shape: .bar),
        Piece(position: 0.62, delay: 240, rotation: -25, drift: 12, color: 2, shape: .dot),
        Piece(position: 0.70, delay: 80, rotation: 70, drift: -6, color: 3, shape: .bar),
        Piece(position: 0.78, delay: 180, rotation: -45, drift: 10, color: 4, shape: .bar),
        Piece(position: 0.86, delay: 100, rotation: 15, drift: -12, color: 0, shape: .dot),
        Piece(position: 0.93, delay: 260, rotation: -70, drift: 8, color: 1, shape: .bar),
    ]

    /// The data colour roles the pieces use.
    static var colors: [MonarchColorRole] { [\.data1, \.data2, \.data3, \.data4, \.data5] }

    /// The colour roles of the icon tile: the tone's subtle fill and strong foreground.
    static func tile(tone: MonarchTone) -> (fill: MonarchColorRole, foreground: MonarchColorRole) {
        let palette = MonarchTonePalette.palette(for: tone)
        return (palette.subtle, palette.strong)
    }

    /// When the last piece has finished falling, in seconds.
    static var totalDuration: Double {
        (pieces.map(\.delay).max() ?? 0) / 1000 + fallDuration
    }

    /// Where a piece is and how visible it is at one moment.
    struct PieceState: Equatable {
        /// Sideways movement from its start, in points.
        let x: CGFloat
        /// Downward movement from the top of the area, in points.
        let y: CGFloat
        /// The rotation, in degrees.
        let rotation: Double
        let opacity: Double
    }

    /// The standard ease: fast at first, slow at the end.
    static func ease(_ t: Double) -> Double {
        1 - pow(1 - t, 3)
    }

    /// The opacity at a share of the fall: in over the first 15%, out over the last 30%.
    static func opacity(at t: Double) -> Double {
        if t <= 0 { return 0 }
        if t < fadeInEnd { return t / fadeInEnd }
        if t <= fadeOutStart { return 1 }
        if t >= 1 { return 0 }
        return (1 - t) / (1 - fadeOutStart)
    }

    /// A piece `elapsed` seconds after the celebration appeared, or its
    /// still position under Reduce Motion. Pure, so it is tested without a view.
    static func state(of piece: Piece, elapsed: Double, reduceMotion: Bool) -> PieceState {
        if reduceMotion {
            return PieceState(
                x: piece.drift,
                y: CGFloat(piece.delay) * restPerDelay,
                rotation: piece.rotation,
                opacity: restingOpacity
            )
        }
        let t = min(max((elapsed - piece.delay / 1000) / fallDuration, 0), 1)
        let eased = CGFloat(ease(t))
        return PieceState(
            x: piece.drift * eased,
            y: startOffset + (fallDistance - startOffset) * eased,
            rotation: piece.rotation + fallTurn * Double(eased),
            opacity: opacity(at: t)
        )
    }

    /// Whether the fall is over and the drawing can stop.
    static func isFinished(elapsed: Double) -> Bool {
        elapsed >= totalDuration
    }

    /// What is announced when the celebration appears: the title, then the message.
    static func announcement(title: String, message: String?) -> String {
        guard let message, !message.isEmpty else { return title }
        return "\(title). \(message)"
    }
}
