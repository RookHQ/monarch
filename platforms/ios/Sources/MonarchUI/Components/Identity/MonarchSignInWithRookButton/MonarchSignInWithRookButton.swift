import SwiftUI

/// The "Sign in with Rook ID" button, for any app that offers Rook ID sign-in.
///
/// ```swift
/// MonarchSignInWithRookButton(mark: Image("RookIDMark"), isLoading: isStarting) {
///     startSignIn()
/// }
/// ```
///
/// The button only starts the flow. The sign-in itself runs in the app, in an
/// `ASWebAuthenticationSession`. The light, dark and neutral variants keep the
/// same look on every screen whatever its theme. Monarch ships no artwork:
/// pass the Rook ID mark as an image or a URL.
///
/// Accessibility: a standard button named by its title. The mark is
/// decorative. While `isLoading` is true a progress indicator replaces the
/// mark, the action does not run and VoiceOver reads "Loading" as the value.
/// The touch target is at least 44 points high at every size, and the title
/// scales with Dynamic Type and wraps.
public struct MonarchSignInWithRookButton: View {
    private enum Mark {
        case image(Image?)
        case url(URL?)
    }

    private let title: String?
    private let mark: Mark
    private let variant: MonarchSignInWithRookButtonVariant
    private let size: MonarchSignInWithRookButtonSize
    private let isLoading: Bool
    private let loadingLabel: String?
    private let fullWidth: Bool
    private let action: @MainActor () -> Void

    @ScaledMetric private var markSize: CGFloat

    /// A sign-in button with a mark the app already has.
    ///
    /// - Parameters:
    ///   - title: The label, already localised. By default "Sign in with Rook ID".
    ///   - mark: The Rook ID mark, drawn before the title. Decorative.
    ///   - variant: The look.
    ///   - size: The height and padding.
    ///   - isLoading: Whether the sign-in is starting.
    ///   - loadingLabel: Replaces the built-in "Loading" spoken value.
    ///   - fullWidth: Whether the button fills the available width.
    ///   - action: Runs when the button is activated and not loading.
    public init(
        _ title: String? = nil,
        mark: Image? = nil,
        variant: MonarchSignInWithRookButtonVariant = .light,
        size: MonarchSignInWithRookButtonSize = .md,
        isLoading: Bool = false,
        loadingLabel: String? = nil,
        fullWidth: Bool = false,
        action: @escaping @MainActor () -> Void
    ) {
        self.init(
            title: title,
            mark: .image(mark),
            variant: variant,
            size: size,
            isLoading: isLoading,
            loadingLabel: loadingLabel,
            fullWidth: fullWidth,
            action: action
        )
    }

    /// A sign-in button with a mark loaded from a URL.
    ///
    /// - Parameters:
    ///   - title: The label, already localised. By default "Sign in with Rook ID".
    ///   - markURL: The address of the Rook ID mark, drawn before the title. Decorative.
    ///   - variant: The look.
    ///   - size: The height and padding.
    ///   - isLoading: Whether the sign-in is starting.
    ///   - loadingLabel: Replaces the built-in "Loading" spoken value.
    ///   - fullWidth: Whether the button fills the available width.
    ///   - action: Runs when the button is activated and not loading.
    public init(
        _ title: String? = nil,
        markURL: URL?,
        variant: MonarchSignInWithRookButtonVariant = .light,
        size: MonarchSignInWithRookButtonSize = .md,
        isLoading: Bool = false,
        loadingLabel: String? = nil,
        fullWidth: Bool = false,
        action: @escaping @MainActor () -> Void
    ) {
        self.init(
            title: title,
            mark: .url(markURL),
            variant: variant,
            size: size,
            isLoading: isLoading,
            loadingLabel: loadingLabel,
            fullWidth: fullWidth,
            action: action
        )
    }

    private init(
        title: String?,
        mark: Mark,
        variant: MonarchSignInWithRookButtonVariant,
        size: MonarchSignInWithRookButtonSize,
        isLoading: Bool,
        loadingLabel: String?,
        fullWidth: Bool,
        action: @escaping @MainActor () -> Void
    ) {
        self.title = title
        self.mark = mark
        self.variant = variant
        self.size = size
        self.isLoading = isLoading
        self.loadingLabel = loadingLabel
        self.fullWidth = fullWidth
        self.action = action
        _markSize = ScaledMetric(wrappedValue: MonarchSignInWithRookButtonTokens.markSize(size), relativeTo: .body)
    }

    /// The content of the view.
    public var body: some View {
        Button {
            if !isLoading { action() }
        } label: {
            HStack(spacing: MonarchSignInWithRookButtonTokens.spacing(size)) {
                if isLoading {
                    ProgressView().controlSize(.small).frame(width: markSize, height: markSize)
                } else {
                    markView
                }
                Text(title ?? MonarchSignInWithRookButtonStrings.title)
            }
        }
        .buttonStyle(MonarchSignInWithRookButtonStyle(variant: variant, size: size, fullWidth: fullWidth))
        .accessibilityValue(isLoading ? (loadingLabel ?? MonarchSignInWithRookButtonStrings.loading) : "")
    }

    @ViewBuilder private var markView: some View {
        switch mark {
        case .image(let image):
            if let image {
                MonarchRemoteImage(image: image) { EmptyView() }
                    .frame(width: markSize, height: markSize)
                    .accessibilityHidden(true)
            }
        case .url(let url):
            if let url {
                MonarchRemoteImage(url: url) { EmptyView() }
                    .frame(width: markSize, height: markSize)
                    .accessibilityHidden(true)
            }
        }
    }
}

/// The fixed Rook ID look: a pill in the colours of the variant's theme.
private struct MonarchSignInWithRookButtonStyle: ButtonStyle {
    let variant: MonarchSignInWithRookButtonVariant
    let size: MonarchSignInWithRookButtonSize
    let fullWidth: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchSignInWithRookButtonBody(
            configuration: configuration,
            variant: variant,
            size: size,
            fullWidth: fullWidth
        )
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchSignInWithRookButtonBody: View {
    let configuration: ButtonStyleConfiguration
    let variant: MonarchSignInWithRookButtonVariant
    let size: MonarchSignInWithRookButtonSize
    let fullWidth: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let pinned = MonarchSignInWithRookButtonTokens.theme(for: variant, in: theme)
        let appearance = MonarchSignInWithRookButtonTokens.appearance(
            variant: variant,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let foreground = pinned.colors[role: appearance.foreground]
        configuration.label
            .monarchTextStyle(
                MonarchSignInWithRookButtonTokens.textStyle(size),
                weight: MonarchSignInWithRookButtonTokens.fontWeight
            )
            .multilineTextAlignment(.center)
            .foregroundStyle(foreground)
            .tint(foreground)
            // The system progress indicator follows the colour scheme, not the tint alone.
            .environment(\.colorScheme, pinned.isDark ? .dark : .light)
            .padding(.leading, MonarchSignInWithRookButtonTokens.leadingPadding(size))
            .padding(.trailing, MonarchSignInWithRookButtonTokens.trailingPadding(size))
            .padding(.vertical, MonarchSignInWithRookButtonTokens.verticalPadding)
            .frame(
                maxWidth: fullWidth ? .infinity : nil,
                minHeight: MonarchSignInWithRookButtonTokens.minHeight(size)
            )
            .background {
                if let background = appearance.background {
                    Capsule().fill(pinned.colors[role: background])
                }
            }
            .overlay {
                if let border = appearance.border {
                    Capsule().strokeBorder(pinned.colors[role: border], lineWidth: MonarchShapes.borderWidth)
                }
            }
            .monarchMinimumHitTarget()
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchSignInWithRookButton") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchSignInWithRookButtonVariant.allCases, id: \.self) { variant in
                    HStack {
                        MonarchSignInWithRookButton(variant: variant, size: .sm) {}
                        MonarchSignInWithRookButton(variant: variant, isLoading: true) {}
                    }
                }
                MonarchSignInWithRookButton(size: .lg, fullWidth: true) {}
                MonarchSignInWithRookButton {}.disabled(true)
            }
        }
    }
#endif
