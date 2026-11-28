import SwiftUI

/// A framed picture of the product: a screenshot or a live view in a bordered window, optionally with a browser
/// bar that shows the page address.
///
/// ```swift
/// MonarchProductFrame("Share dialog with a copyable link", chrome: .browser, title: "example.rookhq.com/share") {
///     Text("Anyone with the link can view").padding()
/// }
/// ```
///
/// Accessibility: the frame is one image element named by `label`; the content inside is not read, because it is
/// a picture of the product rather than a working screen.
public struct MonarchProductFrame<Content: View>: View {
    private let label: String
    private let chrome: MonarchProductFrameChrome
    private let title: String?
    private let content: Content

    @Environment(\.monarchTheme) private var theme

    /// A product frame.
    ///
    /// - Parameters:
    ///   - label: What the picture shows, read by VoiceOver.
    ///   - chrome: The window around the content.
    ///   - title: The address shown in the browser bar.
    ///   - content: The screenshot or view.
    public init(
        _ label: String,
        chrome: MonarchProductFrameChrome = .none,
        title: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.label = label
        self.chrome = chrome
        self.title = title
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        VStack(spacing: 0) {
            if chrome == .browser {
                HStack(spacing: MonarchMarketingSectionTokens.chromeDotSpacing) {
                    ForEach(0..<MonarchMarketingSectionTokens.chromeDotCount, id: \.self) { _ in
                        Circle()
                            .fill(theme.colors[role: MonarchMarketingSectionTokens.chromeDotFill])
                            .frame(
                                width: MonarchMarketingSectionTokens.chromeDot,
                                height: MonarchMarketingSectionTokens.chromeDot
                            )
                    }
                    if let title {
                        Text(title)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(theme.colors[role: MonarchMarketingSectionTokens.supportingColor])
                            .lineLimit(1)
                            .frame(maxWidth: .infinity)
                    }
                }
                .padding(.horizontal, MonarchMarketingSectionTokens.chromePadding)
                .frame(minHeight: MonarchMarketingSectionTokens.chromeHeight)
                .background(theme.colors[role: MonarchMarketingSectionTokens.chromeFill])
                MonarchDivider()
            }
            content
                .frame(maxWidth: .infinity, alignment: .topLeading)
        }
        .background(theme.colors.bgSurface)
        .clipShape(MonarchShapes.container)
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: MonarchMarketingSectionTokens.border],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(label)
        .accessibilityAddTraits(.isImage)
    }
}
