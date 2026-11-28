import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchDeviceFrame``.
enum MonarchDeviceFrameTokens {
    /// One side button, as parts of the frame's height.
    struct SideButton: Hashable {
        /// Whether the button is on the leading edge.
        let isLeading: Bool
        /// The distance from the top, as a part of the frame's height.
        let top: CGFloat
        /// The length, as a part of the frame's height.
        let height: CGFloat
    }

    /// The colour role of the bezel, the island and the side buttons.
    static var bezelColor: MonarchColorRole { \.bgInverse }
    /// The colour role of the screen.
    static var screenColor: MonarchColorRole { \.bgSurface }
    /// The colour role of text and symbols on the screen.
    static var screenForeground: MonarchColorRole { \.textDefault }

    /// The part of the screen's width the island takes.
    static let islandWidthRatio: CGFloat = 0.3
    /// The widest the island gets: 120 points.
    static let islandMaxWidth: CGFloat = MonarchTokens.Space.space24 + MonarchTokens.Space.space6
    /// The height of the island: 22 points.
    static let islandHeight: CGFloat = MonarchTokens.Space.space5 + MonarchTokens.Space.space0_5
    /// The gap between the top of the screen and the island.
    static let islandInset: CGFloat = MonarchTokens.Space.space2
    /// The width of a side button: 3 points.
    static let sideButtonWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The least height of the status bar.
    static let statusBarHeight: CGFloat = MonarchTokens.Space.space10
    /// Padding before and after the content of the status bar.
    static let statusBarPadding: CGFloat = MonarchTokens.Space.space6
    /// The weight of the time text.
    static let statusBarWeight = MonarchTextStyleSpec.fontWeight(MonarchTokens.FontWeight.semibold)
    /// The height of the tallest signal bar.
    static let signalHeight: CGFloat = MonarchTokens.Space.space2_5
    /// The width of a signal bar: 3 points.
    static let signalBarWidth: CGFloat = MonarchTokens.Space.space0_5 + MonarchTokens.Space.px
    /// The gap between signal bars.
    static let signalSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The heights of the signal bars, as parts of the tallest.
    static let signalHeights: [CGFloat] = [0.4, 0.7, 1]

    /// The screen size of a device in points.
    static func screenSize(_ device: MonarchDeviceFrameDevice) -> CGSize {
        switch device {
        case .phone: CGSize(width: 390, height: 844)
        case .tablet: CGSize(width: 820, height: 1180)
        }
    }

    /// The size to use: the given one when both sides are positive and
    /// finite, otherwise the device's own.
    static func resolvedScreenSize(_ size: CGSize?, device: MonarchDeviceFrameDevice) -> CGSize {
        guard let size, size.width.isFinite, size.height.isFinite, size.width > 0, size.height > 0 else {
            return screenSize(device)
        }
        return size
    }

    /// The thickness of the bezel: 10 points for a phone, 14 for a tablet.
    static func bezel(_ device: MonarchDeviceFrameDevice) -> CGFloat {
        switch device {
        case .phone: MonarchTokens.Space.space2_5
        case .tablet: MonarchTokens.Space.space3 + MonarchTokens.Space.space0_5
        }
    }

    /// The corner radius of the frame: 44 points for a phone, 28 for a tablet.
    static func cornerRadius(_ device: MonarchDeviceFrameDevice) -> CGFloat {
        switch device {
        case .phone: MonarchTokens.Space.space10 + MonarchTokens.Space.space1
        case .tablet: MonarchTokens.Space.space6 + MonarchTokens.Space.space1
        }
    }

    /// The corner radius of the screen, concentric with the frame.
    static func screenCornerRadius(_ device: MonarchDeviceFrameDevice) -> CGFloat {
        cornerRadius(device) - bezel(device)
    }

    /// Whether the device has a camera island.
    static func showsIsland(_ device: MonarchDeviceFrameDevice) -> Bool {
        device == .phone
    }

    /// The shadow under the frame.
    static func elevation(isFlat: Bool) -> MonarchElevationLevel {
        isFlat ? .none : .xl
    }

    /// The side buttons: three on the leading edge and one on the trailing
    /// edge of a phone, one on the trailing edge of a tablet, none when flat.
    static func sideButtons(device: MonarchDeviceFrameDevice, isFlat: Bool) -> [SideButton] {
        guard !isFlat else { return [] }
        let power = SideButton(isLeading: false, top: 0.22, height: 0.1)
        switch device {
        case .phone:
            return [
                SideButton(isLeading: true, top: 0.14, height: 0.04),
                SideButton(isLeading: true, top: 0.21, height: 0.07),
                SideButton(isLeading: true, top: 0.3, height: 0.07),
                power,
            ]
        case .tablet:
            return [power]
        }
    }
}
