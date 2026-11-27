import CoreGraphics
import Foundation
import MonarchTokens
import SwiftUI

/// The position of the picture in the crop area, in crop-area units (the
/// area is 1 wide): the zoom and the offset of the picture's centre from
/// the area's centre.
struct MonarchAvatarUploadViewport: Equatable {
    var zoom: CGFloat = 1
    var x: CGFloat = 0
    var y: CGFloat = 0
}

/// One step of moving or zooming the picture, from a key, an accessibility
/// action or the adjustable action.
enum MonarchAvatarUploadMove: CaseIterable {
    case left, right, up, down, zoomIn, zoomOut, reset
}

/// Why a picked file was refused.
enum MonarchAvatarUploadProblem: Equatable {
    case tooLarge
    case unreadable
}

/// Component tokens and pure logic of ``MonarchAvatarUpload``.
enum MonarchAvatarUploadTokens {
    /// The smallest zoom: the picture's short side fills the crop area.
    static let minimumZoom: CGFloat = 1
    /// The largest zoom.
    static let maximumZoom: CGFloat = 4
    /// One pan step, in crop-area units, as on the web.
    static let panStep: CGFloat = 0.04
    /// One zoom step, as on the web.
    static let zoomStep: CGFloat = 0.1
    /// The largest side a picked picture is decoded at, to bound memory.
    static let maximumDecodedSide = 2048
    /// The gap between the avatar and the buttons, and between the editor's parts.
    static let spacing: CGFloat = MonarchTokens.Space.space4
    /// The gap between buttons.
    static let buttonSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between an error icon and its text.
    static let errorSpacing: CGFloat = MonarchTokens.Space.space1
    /// The largest side of the crop area.
    static let maximumCropSide: CGFloat = MonarchTokens.Breakpoint.sm / 2
    /// The width of the ring around the kept area.
    static let ringWidth: CGFloat = MonarchTokens.Space.space0_5
    /// How much the mask darkens what is cut away, as a fraction.
    static let maskOpacity: Double = 0.6
    /// The corner radius of the square crop.
    static let squareRadius: CGFloat = MonarchTokens.Radius.lg
    /// The avatar size of the current picture.
    static let avatarSize: MonarchAvatarSize = .lg
    /// The SF Symbol of the picker button.
    static let pickSymbol = "photo"
    /// The SF Symbol before an error.
    static let errorSymbol = "exclamationmark.circle"
    /// The fill behind the picture in the crop area.
    static var cropBackground: MonarchColorRole { \.bgMuted }
    /// The colour of the mask over what is cut away.
    static var mask: MonarchColorRole { \.bgInverse }
    /// The ring around the kept area.
    static var ring: MonarchColorRole { \.bgSurface }
    /// The colour of an error.
    static var error: MonarchColorRole { \.dangerStrong }
    /// The colour of the hint.
    static var hint: MonarchColorRole { \.textMuted }

    /// The size of the picture in crop-area units at a zoom: its short side is `zoom`.
    static func imageSize(natural: CGSize, zoom: CGFloat) -> CGSize {
        let short = max(1, min(natural.width, natural.height))
        return CGSize(width: natural.width / short * zoom, height: natural.height / short * zoom)
    }

    /// Keeps the zoom in its range and the picture covering the crop area.
    static func clamp(_ viewport: MonarchAvatarUploadViewport, natural: CGSize) -> MonarchAvatarUploadViewport {
        let zoom = min(maximumZoom, max(minimumZoom, viewport.zoom))
        let size = imageSize(natural: natural, zoom: zoom)
        let maxX = (size.width - 1) / 2
        let maxY = (size.height - 1) / 2
        return MonarchAvatarUploadViewport(
            zoom: zoom,
            x: min(maxX, max(-maxX, viewport.x)),
            y: min(maxY, max(-maxY, viewport.y))
        )
    }

    /// The square of the picture, in its pixels, that the crop area shows.
    static func crop(natural: CGSize, viewport: MonarchAvatarUploadViewport) -> MonarchAvatarCrop {
        let short = min(natural.width, natural.height)
        let size = short / viewport.zoom
        // One crop-area unit is `size` picture pixels.
        return MonarchAvatarCrop(
            x: natural.width / 2 - viewport.x * size - size / 2,
            y: natural.height / 2 - viewport.y * size - size / 2,
            size: size
        )
    }

    /// The viewport after a drag of `translation` points across an area `side` points wide.
    static func dragged(
        _ start: MonarchAvatarUploadViewport,
        by translation: CGSize,
        side: CGFloat,
        natural: CGSize
    ) -> MonarchAvatarUploadViewport {
        guard side > 0 else { return start }
        var next = start
        next.x += translation.width / side
        next.y += translation.height / side
        return clamp(next, natural: natural)
    }

    /// The viewport after a pinch of `magnification` from `start`.
    static func magnified(
        _ start: MonarchAvatarUploadViewport,
        by magnification: CGFloat,
        natural: CGSize
    ) -> MonarchAvatarUploadViewport {
        var next = start
        next.zoom = start.zoom * magnification
        return clamp(next, natural: natural)
    }

    /// The viewport after one step. `isLarge` moves four times as far (Shift with a key).
    static func apply(
        _ move: MonarchAvatarUploadMove,
        to viewport: MonarchAvatarUploadViewport,
        natural: CGSize,
        isLarge: Bool = false
    ) -> MonarchAvatarUploadViewport {
        let step = isLarge ? panStep * 4 : panStep
        var next = viewport
        switch move {
        case .left: next.x -= step
        case .right: next.x += step
        case .up: next.y -= step
        case .down: next.y += step
        case .zoomIn: next.zoom += zoomStep
        case .zoomOut: next.zoom -= zoomStep
        case .reset: next = MonarchAvatarUploadViewport()
        }
        return clamp(next, natural: natural)
    }

    /// The zoom in whole percent.
    static func zoomPercent(_ zoom: CGFloat) -> Int {
        Int((zoom * 100).rounded())
    }

    /// The spoken zoom: "150%".
    static func zoomValue(_ zoom: CGFloat, labels: MonarchAvatarUploadLabels) -> String {
        let percent = zoomPercent(zoom)
        return labels.zoomValue?(percent) ?? MonarchAvatarUploadStrings.zoomValue(percent)
    }

    /// The name of an accessibility action, or `nil` for the zoom steps (the adjustable action).
    static func actionName(_ move: MonarchAvatarUploadMove, labels: MonarchAvatarUploadLabels) -> String? {
        switch move {
        case .left: labels.moveLeft ?? MonarchAvatarUploadStrings.moveLeft
        case .right: labels.moveRight ?? MonarchAvatarUploadStrings.moveRight
        case .up: labels.moveUp ?? MonarchAvatarUploadStrings.moveUp
        case .down: labels.moveDown ?? MonarchAvatarUploadStrings.moveDown
        case .reset: labels.reset ?? MonarchAvatarUploadStrings.reset
        case .zoomIn, .zoomOut: nil
        }
    }

    /// The step a key makes, or `nil` for any other key.
    static func move(forKey key: KeyEquivalent) -> MonarchAvatarUploadMove? {
        switch key {
        case .leftArrow: .left
        case .rightArrow: .right
        case .upArrow: .up
        case .downArrow: .down
        case "+", "=": .zoomIn
        case "-": .zoomOut
        case .home: .reset
        default: nil
        }
    }

    /// The picker button title: change when there is a picture.
    static func pickTitle(hasPicture: Bool, labels: MonarchAvatarUploadLabels) -> String {
        hasPicture
            ? (labels.change ?? MonarchAvatarUploadStrings.change)
            : (labels.upload ?? MonarchAvatarUploadStrings.upload)
    }

    /// Whether a file of `byteCount` bytes is refused for its size.
    static func isTooLarge(byteCount: Int, maxFileSize: Int?) -> Bool {
        guard let maxFileSize else { return false }
        return byteCount > maxFileSize
    }

    /// The message for a refused file.
    static func message(
        for problem: MonarchAvatarUploadProblem,
        maxFileSize: Int?,
        labels: MonarchAvatarUploadLabels
    ) -> String {
        switch problem {
        case .unreadable:
            return labels.unreadable ?? MonarchAvatarUploadStrings.unreadable
        case .tooLarge:
            let limit = Int64(maxFileSize ?? 0).formatted(.byteCount(style: .file))
            return labels.tooLarge?(limit) ?? MonarchAvatarUploadStrings.tooLarge(limit)
        }
    }
}
