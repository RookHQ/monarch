import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchScreenShareView``.
enum MonarchScreenShareViewTokens {
    /// The zoom steps of the buttons. The first is "fit".
    static let zoomLevels: [Double] = [1, 1.5, 2, 3]
    /// The zoom at which the whole screen fits the view.
    static let fitZoom: Double = 1
    /// The largest zoom.
    static let maximumZoom: Double = 3
    /// The zoom a double tap goes to from "fit".
    static let doubleTapZoom: Double = 2
    /// The number of taps that toggle the zoom.
    static let zoomTapCount = 2
    /// Zoom values closer than this are treated as equal.
    static let zoomTolerance: Double = 0.001
    /// The width of the picture area divided by its height.
    static let aspectRatio: CGFloat = 16 / 9
    /// The gap between the bar and the picture.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between two controls.
    static let controlSpacing: CGFloat = MonarchTokens.Space.space1
    /// The gap between an icon and the text after it.
    static let iconSpacing: CGFloat = MonarchTokens.Space.space2
    /// Padding around the text shown instead of the picture.
    static let noticePadding: CGFloat = MonarchTokens.Space.space4
    /// The gap between the notice and its button.
    static let noticeSpacing: CGFloat = MonarchTokens.Space.space3
    /// The fill role behind the picture.
    static var stage: MonarchColorRole { \.bgInverse }
    /// The fill role behind the waiting text and the notice.
    static var placeholder: MonarchColorRole { \.bgMuted }
    /// The SF Symbol before the presenter line.
    static let presenterSymbol = "display"
    /// The SF Symbol of the zoom-in button.
    static let zoomInSymbol = "plus"
    /// The SF Symbol of the zoom-out button.
    static let zoomOutSymbol = "minus"

    /// The SF Symbol of the picture-in-picture button.
    static func pictureInPictureSymbol(isActive: Bool) -> String {
        isActive ? "pip.exit" : "pip.enter"
    }

    /// The SF Symbol of the full-screen button.
    static func fullScreenSymbol(isFullScreen: Bool) -> String {
        isFullScreen ? "arrow.down.right.and.arrow.up.left" : "arrow.up.left.and.arrow.down.right"
    }

    /// A zoom limited to "fit" through the largest zoom. Not-a-number counts
    /// as "fit".
    static func clamped(_ zoom: Double) -> Double {
        guard !zoom.isNaN else { return fitZoom }
        return min(max(zoom, fitZoom), maximumZoom)
    }

    /// The next zoom step above the current zoom, or `nil` at the largest.
    static func stepIn(from zoom: Double) -> Double? {
        zoomLevels.first { $0 > zoom + zoomTolerance }
    }

    /// The next zoom step below the current zoom, or `nil` at "fit".
    static func stepOut(from zoom: Double) -> Double? {
        zoomLevels.last { $0 < zoom - zoomTolerance }
    }

    /// Whether the picture is larger than the view.
    static func isZoomed(_ zoom: Double) -> Bool {
        zoom > fitZoom + zoomTolerance
    }

    /// The zoom a double tap goes to.
    static func toggled(from zoom: Double) -> Double {
        isZoomed(zoom) ? fitZoom : doubleTapZoom
    }

    /// The zoom as a percentage, such as "150%".
    static func percentText(_ zoom: Double, locale: Locale = .current) -> String {
        clamped(zoom).formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    /// The visible presenter line.
    static func presenterText(name: String, isLocal: Bool, labels: MonarchScreenShareViewLabels) -> String {
        if isLocal {
            labels.youArePresenting ?? MonarchScreenShareViewStrings.youArePresenting
        } else {
            labels.presenting ?? MonarchScreenShareViewStrings.presenting(name)
        }
    }

    /// The spoken name of the shared screen.
    static func regionText(name: String, labels: MonarchScreenShareViewLabels) -> String {
        labels.region ?? MonarchScreenShareViewStrings.region(name)
    }

    /// The spoken name of the picture-in-picture button.
    static func pictureInPictureLabel(isActive: Bool, labels: MonarchScreenShareViewLabels) -> String {
        if isActive {
            labels.exitPictureInPicture ?? MonarchScreenShareViewStrings.exitPictureInPicture
        } else {
            labels.pictureInPicture ?? MonarchScreenShareViewStrings.pictureInPicture
        }
    }

    /// The spoken name of the full-screen button.
    static func fullScreenLabel(isFullScreen: Bool, labels: MonarchScreenShareViewLabels) -> String {
        if isFullScreen {
            labels.exitFullScreen ?? MonarchScreenShareViewStrings.exitFullScreen
        } else {
            labels.fullScreen ?? MonarchScreenShareViewStrings.fullScreen
        }
    }
}
