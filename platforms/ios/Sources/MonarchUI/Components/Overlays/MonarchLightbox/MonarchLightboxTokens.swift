import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchLightbox``.
enum MonarchLightboxTokens {
    // MARK: Layout

    /// Padding around the viewer.
    static let padding: CGFloat = MonarchTokens.Space.space3
    /// The gap between the toolbar, the stage, the buttons and the caption.
    static let sectionSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between two buttons, and the padding of the group of tools.
    static let toolSpacing: CGFloat = MonarchTokens.Space.space1
    /// Padding above and below the position count.
    static let positionVerticalPadding: CGFloat = MonarchTokens.Space.space1
    /// Padding before and after the position count.
    static let positionHorizontalPadding: CGFloat = MonarchTokens.Space.space2_5
    /// Padding above and below the caption.
    static let captionVerticalPadding: CGFloat = MonarchTokens.Space.space2
    /// Padding before and after the caption.
    static let captionHorizontalPadding: CGFloat = MonarchTokens.Space.space3
    /// The largest height of the caption before Dynamic Type; a longer
    /// caption scrolls.
    static let captionMaximumHeight: CGFloat = MonarchTokens.Space.space24
    /// Padding around the text shown instead of media.
    static let noticePadding: CGFloat = MonarchTokens.Space.space4
    /// The diameter of the round previous and next buttons.
    static let stepSize: CGFloat = 36
    /// The fill role behind the viewer.
    static var canvas: MonarchColorRole { \.bgCanvas }
    /// The fill role of the position count, the tools, the caption and the
    /// notice.
    static var raised: MonarchColorRole { \.bgRaised }

    /// The corner of the media and of the caption.
    static var mediaShape: RoundedRectangle { MonarchShapes.control }

    // MARK: Symbols

    /// The SF Symbol of the zoom-in button.
    static let zoomInSymbol = "plus"
    /// The SF Symbol of the zoom-out button.
    static let zoomOutSymbol = "minus"
    /// The SF Symbol of the share button.
    static let shareSymbol = "square.and.arrow.up"
    /// The SF Symbol of the close button.
    static let closeSymbol = "xmark"
    /// The SF Symbol of the previous button. It mirrors in right-to-left.
    static let previousSymbol = "chevron.backward"
    /// The SF Symbol of the next button. It mirrors in right-to-left.
    static let nextSymbol = "chevron.forward"

    // MARK: Zoom

    /// The zoom at which the whole image fits the stage.
    static let fitZoom: Double = 1
    /// How much one press of a zoom button changes the zoom.
    static let zoomStep: Double = 0.5
    /// The zoom a double tap goes to from "fit".
    static let doubleTapZoom: Double = 2
    /// The number of taps that toggle the zoom.
    static let zoomTapCount = 2
    /// Zoom values closer than this are treated as equal.
    static let zoomTolerance: Double = 0.001

    /// The largest zoom for the value the app gave: never below "fit".
    /// Not-a-number counts as "fit".
    static func limit(_ maximumZoom: Double) -> Double {
        guard !maximumZoom.isNaN else { return fitZoom }
        return max(fitZoom, maximumZoom)
    }

    /// A zoom limited to "fit" through the largest zoom. Not-a-number counts
    /// as "fit".
    static func clamped(_ zoom: Double, maximum: Double) -> Double {
        guard !zoom.isNaN else { return fitZoom }
        return min(max(zoom, fitZoom), limit(maximum))
    }

    /// Whether the image is larger than the stage.
    static func isZoomed(_ zoom: Double) -> Bool {
        zoom > fitZoom + zoomTolerance
    }

    /// The zoom one step in, or `nil` at the largest zoom.
    static func zoomedIn(from zoom: Double, maximum: Double) -> Double? {
        let current = clamped(zoom, maximum: maximum)
        guard current < limit(maximum) - zoomTolerance else { return nil }
        return clamped(current + zoomStep, maximum: maximum)
    }

    /// The zoom one step out, or `nil` at "fit".
    static func zoomedOut(from zoom: Double, maximum: Double) -> Double? {
        let current = clamped(zoom, maximum: maximum)
        guard isZoomed(current) else { return nil }
        return clamped(current - zoomStep, maximum: maximum)
    }

    /// The zoom a double tap goes to: "fit" when zoomed, otherwise the
    /// double-tap zoom, limited to the largest zoom.
    static func toggled(from zoom: Double, maximum: Double) -> Double {
        isZoomed(zoom) ? fitZoom : clamped(doubleTapZoom, maximum: maximum)
    }

    /// The zoom as a percentage, such as "150%".
    static func percentText(_ zoom: Double, locale: Locale = .current) -> String {
        max(zoom, fitZoom).formatted(.percent.precision(.fractionLength(0)).locale(locale))
    }

    /// Whether an item can be zoomed: an image that has a source.
    static func isZoomable(_ item: MonarchLightboxItem?) -> Bool {
        guard let item, item.kind == .image else { return false }
        return item.image != nil || item.url != nil
    }

    // MARK: Position

    /// An index limited to the items. With no items it is 0.
    static func clampedIndex(_ index: Int, count: Int) -> Int {
        min(max(index, 0), max(count - 1, 0))
    }

    /// The index one step before (`offset` -1) or after (`offset` 1) the
    /// current one, or `nil` when there is nowhere to go: fewer than two
    /// items, or the end of the items without looping.
    static func index(from index: Int, offset: Int, count: Int, isLooping: Bool) -> Int? {
        guard count > 1 else { return nil }
        let current = clampedIndex(index, count: count)
        var next = current + offset
        if isLooping {
            next = ((next % count) + count) % count
        }
        next = clampedIndex(next, count: count)
        return next == current ? nil : next
    }

    /// The visible position, such as "2 of 5", or `nil` with fewer than two
    /// items.
    static func positionText(index: Int, count: Int, labels: MonarchLightboxLabels) -> String? {
        guard count > 1 else { return nil }
        let position = clampedIndex(index, count: count) + 1
        return labels.position?(position, count) ?? MonarchLightboxStrings.position(position, count)
    }

    /// What VoiceOver hears when the item changes: the position and what the
    /// new item shows, such as "2 of 5: Team photo".
    static func announcement(index: Int, items: [MonarchLightboxItem], labels: MonarchLightboxLabels) -> String {
        guard !items.isEmpty else { return labels.unavailable ?? MonarchLightboxStrings.unavailable }
        let alt = items[clampedIndex(index, count: items.count)].alt
        guard let position = positionText(index: index, count: items.count, labels: labels) else { return alt }
        return alt.isEmpty ? position : position + ": " + alt
    }
}

#if DEBUG
    extension MonarchLightboxTokens {
        /// The height of the viewer in the Xcode preview.
        static let previewHeight: CGFloat = MonarchTokens.Breakpoint.sm
    }
#endif
