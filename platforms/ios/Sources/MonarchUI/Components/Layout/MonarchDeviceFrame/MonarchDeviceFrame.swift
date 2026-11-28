import SwiftUI

/// A phone or tablet outline around a preview: a link page, an email, a
/// mobile layout shown inside a builder.
///
/// The frame keeps the device's aspect ratio, is never wider than the
/// device's screen width, and shrinks to the width it is given. The content
/// is laid out in the screen area and clipped to it.
///
/// ```swift
/// MonarchDeviceFrame("Preview of your link page on a phone", statusBarTime: "9:41") {
///     LinkPagePreview(page: page)
/// }
/// ```
///
/// By default the content is a picture: it cannot be touched and is read as
/// one image. With `isInteractive` the content scrolls and can be used.
///
/// Accessibility: the island, the status bar and the side buttons are
/// decorative and hidden. A frame that is not interactive is one element
/// with the image trait, named by `label`; an interactive frame is a
/// container named by `label` whose content stays reachable. The frame is a
/// drawing of a device, so its bezel and status bar do not grow with
/// Dynamic Type; the content inside does.
public struct MonarchDeviceFrame<Content: View>: View {
    private let label: String
    private let device: MonarchDeviceFrameDevice
    private let screenSize: CGSize
    private let statusBarTime: String?
    private let isInteractive: Bool
    private let isFlat: Bool
    private let content: Content

    @Environment(\.monarchTheme) private var theme

    /// A device frame.
    ///
    /// - Parameters:
    ///   - label: What the frame shows, for example "Preview of your link page on a phone".
    ///   - device: A phone, with an island, or a tablet, with a wider bezel.
    ///   - screenSize: The width the frame never exceeds and the height that
    ///     sets its aspect ratio, in points. The device's own size by default:
    ///     390 by 844 for a phone, 820 by 1180 for a tablet.
    ///   - statusBarTime: Shows a decorative status bar with this time text, for example "9:41".
    ///   - isInteractive: Whether people can scroll and use the content.
    ///   - isFlat: Whether to leave out the side buttons and the shadow, for dense layouts.
    ///   - content: The screen content.
    public init(
        _ label: String,
        device: MonarchDeviceFrameDevice = .phone,
        screenSize: CGSize? = nil,
        statusBarTime: String? = nil,
        isInteractive: Bool = false,
        isFlat: Bool = false,
        @ViewBuilder content: () -> Content
    ) {
        self.label = label
        self.device = device
        self.screenSize = MonarchDeviceFrameTokens.resolvedScreenSize(screenSize, device: device)
        self.statusBarTime = statusBarTime
        self.isInteractive = isInteractive
        self.isFlat = isFlat
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        let outline = RoundedRectangle(
            cornerRadius: MonarchDeviceFrameTokens.cornerRadius(device),
            style: .continuous
        )
        let frame = Color.clear
            .monarchElevation(
                MonarchDeviceFrameTokens.elevation(isFlat: isFlat),
                fill: MonarchDeviceFrameTokens.bezelColor,
                in: outline
            )
            .overlay { screen.padding(MonarchDeviceFrameTokens.bezel(device)) }
            .overlay { sideButtons }
            .aspectRatio(screenSize.width / screenSize.height, contentMode: .fit)
            .frame(maxWidth: screenSize.width)
        if isInteractive {
            frame
                .accessibilityElement(children: .contain)
                .accessibilityLabel(label)
        } else {
            frame
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityAddTraits(.isImage)
        }
    }

    private var screen: some View {
        let shape = RoundedRectangle(
            cornerRadius: MonarchDeviceFrameTokens.screenCornerRadius(device),
            style: .continuous
        )
        // The colour is the base, so the screen is drawn even with no content.
        return theme.colors[role: MonarchDeviceFrameTokens.screenColor]
            .overlay(alignment: .top) {
                VStack(spacing: 0) {
                    if let statusBarTime {
                        MonarchDeviceFrameStatusBar(time: statusBarTime)
                    }
                    screenContent
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
                }
                .foregroundStyle(theme.colors[role: MonarchDeviceFrameTokens.screenForeground])
            }
            .clipShape(shape)
            .overlay(alignment: .top) {
                if MonarchDeviceFrameTokens.showsIsland(device) {
                    island
                }
            }
    }

    @ViewBuilder private var screenContent: some View {
        if isInteractive {
            ScrollView(.vertical) { content }
                .scrollIndicators(.hidden)
        } else {
            content.allowsHitTesting(false)
        }
    }

    private var island: some View {
        GeometryReader { proxy in
            Capsule()
                .fill(theme.colors[role: MonarchDeviceFrameTokens.bezelColor])
                .frame(
                    width: min(
                        proxy.size.width * MonarchDeviceFrameTokens.islandWidthRatio,
                        MonarchDeviceFrameTokens.islandMaxWidth
                    ),
                    height: MonarchDeviceFrameTokens.islandHeight
                )
                .frame(maxWidth: .infinity)
                .padding(.top, MonarchDeviceFrameTokens.islandInset)
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    /// Thin bars outside the frame's edges. Leading and trailing follow the layout direction.
    private var sideButtons: some View {
        GeometryReader { proxy in
            ForEach(MonarchDeviceFrameTokens.sideButtons(device: device, isFlat: isFlat), id: \.self) { button in
                Capsule()
                    .fill(theme.colors[role: MonarchDeviceFrameTokens.bezelColor])
                    .frame(
                        width: MonarchDeviceFrameTokens.sideButtonWidth,
                        height: proxy.size.height * button.height
                    )
                    .padding(.top, proxy.size.height * button.top)
                    .padding(button.isLeading ? .leading : .trailing, -MonarchDeviceFrameTokens.sideButtonWidth)
                    .frame(
                        maxWidth: .infinity,
                        maxHeight: .infinity,
                        alignment: button.isLeading ? .topLeading : .topTrailing
                    )
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

#if DEBUG
    #Preview("MonarchDeviceFrame") {
        MonarchPreviewMatrix {
            HStack(alignment: .top) {
                MonarchDeviceFrame("Phone preview", statusBarTime: "9:41") {
                    Text("Phone").padding()
                }
                MonarchDeviceFrame("Tablet preview", device: .tablet, isFlat: true) {
                    Text("Tablet").padding()
                }
            }
        }
    }
#endif
