import SwiftUI

/// A "Skip to main content" control at the top of a screen with long repeated content before the main part (a
/// toolbar of filters, a carousel). Activating it moves VoiceOver focus to the target through
/// `AccessibilityFocusState`.
///
/// ```swift
/// @AccessibilityFocusState private var isMainFocused: Bool
///
/// VStack(alignment: .leading) {
///     MonarchSkipLink(target: $isMainFocused)
///     FilterBar()
///     Text("Results").accessibilityFocused($isMainFocused)
/// }
/// ```
///
/// Most iOS screens do not need one: VoiceOver moves by heading and by container, and the navigation and tab
/// bars are separate regions. Use it where many elements stand before the content people came for.
///
/// It is drawn only while VoiceOver or Switch Control runs, or while it has keyboard focus, unless
/// `isAlwaysVisible` is on; it is always in the accessibility tree.
///
/// Accessibility: a button named "Skip to main content" (or `title`). Activating it sets the target's
/// accessibility focus, so VoiceOver reads the target next. At least 44 points tall.
public struct MonarchSkipLink: View {
    private let title: String?
    private let target: AccessibilityFocusState<Bool>.Binding
    private let isAlwaysVisible: Bool

    @FocusState private var isFocused: Bool
    @Environment(\.accessibilityVoiceOverEnabled) private var isVoiceOverRunning
    @Environment(\.accessibilitySwitchControlEnabled) private var isSwitchControlRunning

    /// A skip link.
    ///
    /// - Parameters:
    ///   - title: Replaces the built-in title "Skip to main content", for example "Skip to search".
    ///   - target: The accessibility focus of the element to move to.
    ///   - isAlwaysVisible: Whether the control is drawn for everyone, not only for assistive technologies.
    public init(
        _ title: String? = nil,
        target: AccessibilityFocusState<Bool>.Binding,
        isAlwaysVisible: Bool = false
    ) {
        self.title = title
        self.target = target
        self.isAlwaysVisible = isAlwaysVisible
    }

    /// The content of the view.
    public var body: some View {
        let isVisible = MonarchSkipLinkTokens.isVisible(
            isAlwaysVisible: isAlwaysVisible,
            isVoiceOverRunning: isVoiceOverRunning,
            isSwitchControlRunning: isSwitchControlRunning,
            isFocused: isFocused
        )
        Button {
            target.wrappedValue = true
        } label: {
            Label(title ?? MonarchSkipLinkStrings.label, systemImage: MonarchSkipLinkTokens.symbol)
        }
        .buttonStyle(.monarch(MonarchSkipLinkTokens.variant, size: MonarchSkipLinkTokens.size))
        .monarchMinimumHitTarget()
        .focused($isFocused)
        .opacity(isVisible ? 1 : 0)
        .frame(maxHeight: isVisible ? nil : 0, alignment: .top)
        .clipped()
    }
}

#if DEBUG
    private struct MonarchSkipLinkPreview: View {
        @AccessibilityFocusState private var isMainFocused: Bool

        var body: some View {
            VStack(alignment: .leading) {
                MonarchSkipLink(target: $isMainFocused, isAlwaysVisible: true)
                Text("Main content starts here.").accessibilityFocused($isMainFocused)
            }
        }
    }

    #Preview("MonarchSkipLink") {
        MonarchPreviewMatrix {
            MonarchSkipLinkPreview()
        }
    }
#endif
