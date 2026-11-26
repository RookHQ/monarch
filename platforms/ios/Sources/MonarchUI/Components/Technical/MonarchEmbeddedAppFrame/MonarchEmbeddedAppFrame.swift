import SwiftUI

/// A screen of another app shown inside this one, in a bordered frame that says when it is loading and when it
/// did not open, with a way to open the page directly.
///
/// ```swift
/// MonarchEmbeddedAppFrame(
///     "Example Desk: Email notifications",
///     state: state,
///     openURL: URL(string: "https://desk.example.com/settings/email"),
///     onRetry: { reload() }
/// ) {
///     EmailNotificationSettings()
/// }
/// ```
///
/// The content is the app's own view: a screen built from Monarch components with the other app's data, or a
/// web view the app manages. The frame draws no web content itself and sets the state the app gives it.
///
/// Accessibility: the frame is a container named by `title`; while it loads or after a failure its value is
/// "Loading…" or "This page didn’t open". The spinner is hidden from VoiceOver. Try again and Open page are
/// separate buttons; Open page is a link.
public struct MonarchEmbeddedAppFrame<Content: View>: View {
    private let title: String
    private let state: MonarchEmbeddedAppFrameState
    private let openURL: URL?
    private let height: CGFloat?
    private let labels: MonarchEmbeddedAppFrameLabels
    private let onRetry: (@MainActor () -> Void)?
    private let content: Content

    @Environment(\.monarchTheme) private var theme

    /// An embedded app frame.
    ///
    /// - Parameters:
    ///   - title: The name of the embedded screen, such as "Example Desk: Email notifications". Read by VoiceOver.
    ///   - state: Loading, ready or failed.
    ///   - openURL: The page, opened by the Open page button of the failure message.
    ///   - height: A fixed height for the frame. Without it the frame is as tall as its content.
    ///   - labels: Replacements for the built-in strings.
    ///   - onRetry: Loads the screen again. Without it Try again is not shown.
    ///   - content: The embedded screen.
    public init(
        _ title: String,
        state: MonarchEmbeddedAppFrameState = .ready,
        openURL: URL? = nil,
        height: CGFloat? = nil,
        labels: MonarchEmbeddedAppFrameLabels = MonarchEmbeddedAppFrameLabels(),
        onRetry: (@MainActor () -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.state = state
        self.openURL = openURL
        self.height = height
        self.labels = labels
        self.onRetry = onRetry
        self.content = content()
    }

    /// The content of the view.
    public var body: some View {
        Group {
            switch state {
            case .ready:
                content
                    .padding(MonarchEmbeddedAppFrameTokens.padding)
                    .frame(maxWidth: .infinity, alignment: .topLeading)
            case .loading:
                VStack(spacing: MonarchEmbeddedAppFrameTokens.spacing) {
                    ProgressView().accessibilityHidden(true)
                    Text(labels.loading ?? MonarchEmbeddedAppFrameStrings.loading)
                        .monarchTextStyle(.bodySmall)
                        .foregroundStyle(theme.colors[role: MonarchEmbeddedAppFrameTokens.supportingColor])
                }
                .frame(maxWidth: .infinity, minHeight: height ?? MonarchEmbeddedAppFrameTokens.placeholderHeight)
            case .failed:
                failure
            }
        }
        .frame(height: height, alignment: .topLeading)
        .background(theme.colors[role: MonarchEmbeddedAppFrameTokens.fill])
        .clipShape(MonarchShapes.container)
        .overlay(
            MonarchShapes.container.strokeBorder(
                theme.colors[role: MonarchEmbeddedAppFrameTokens.border],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .contain)
        .accessibilityLabel(title)
        .accessibilityValue(MonarchEmbeddedAppFrameTokens.accessibilityValue(state, labels: labels))
    }

    private var failure: some View {
        VStack(alignment: .leading, spacing: MonarchEmbeddedAppFrameTokens.spacing) {
            Label {
                Text(labels.errorTitle ?? MonarchEmbeddedAppFrameStrings.errorTitle)
                    .monarchTextStyle(.bodyStrong)
                    .foregroundStyle(theme.colors.textDefault)
            } icon: {
                MonarchIcon(systemName: MonarchEmbeddedAppFrameTokens.failureSymbol)
                    .foregroundStyle(theme.colors.warningStrong)
            }
            .accessibilityAddTraits(.isHeader)
            Text(labels.errorDescription ?? MonarchEmbeddedAppFrameStrings.errorDescription)
                .monarchTextStyle(.bodySmall)
                .foregroundStyle(theme.colors[role: MonarchEmbeddedAppFrameTokens.supportingColor])
                .fixedSize(horizontal: false, vertical: true)
            MonarchFlowLayout(spacing: MonarchEmbeddedAppFrameTokens.actionSpacing) {
                if let onRetry {
                    MonarchButton(labels.retry ?? MonarchEmbeddedAppFrameStrings.retry, size: .sm) { onRetry() }
                }
                if let openURL {
                    Link(labels.open ?? MonarchEmbeddedAppFrameStrings.open, destination: openURL)
                        .buttonStyle(.monarch(.ghost, size: .sm))
                }
            }
        }
        .padding(MonarchEmbeddedAppFrameTokens.padding)
        .frame(maxWidth: .infinity, alignment: .topLeading)
    }
}

#if DEBUG
    #Preview("MonarchEmbeddedAppFrame") {
        MonarchPreviewMatrix {
            VStack {
                MonarchEmbeddedAppFrame("Example Desk: Email notifications") {
                    Text("Email notifications").monarchTextStyle(.bodyStrong)
                }
                MonarchEmbeddedAppFrame("Example Desk: Billing", state: .loading) { EmptyView() }
                MonarchEmbeddedAppFrame(
                    "Example Desk: Billing",
                    state: .failed,
                    openURL: URL(string: "https://example.com"),
                    onRetry: {}
                ) { EmptyView() }
            }
        }
    }
#endif
