import SwiftUI

/// A button with a title, optional SF Symbols and a loading state.
///
/// It is a standard `Button` with ``MonarchButtonStyle`` applied. Use the
/// style directly when the label is not a plain title.
///
/// ```swift
/// MonarchButton("Save", systemImage: "checkmark", variant: .primary) {
///     save()
/// }
///
/// MonarchButton("Dismiss", systemImage: "xmark", variant: .ghost, isIconOnly: true) {
///     dismiss()
/// }
/// ```
///
/// While `isLoading` is true a progress indicator replaces the leading icon,
/// the action does not run, and VoiceOver reads "Loading" as the value. The
/// button stays focusable so the state can be read. Icons are decorative.
///
/// With `isIconOnly` only the symbol is drawn, on a square button, and the
/// title is still the spoken name, so an icon-only button always has a
/// label. The touch target is at least 44 by 44 points either way.
///
/// Inside a container that styles its buttons itself, such as
/// ``MonarchButtonGroup``, the button takes the container's style instead of
/// its own `variant` and `size`.
public struct MonarchButton: View {
    private let title: String
    private let systemImage: String?
    private let trailingSystemImage: String?
    private let variant: MonarchButtonVariant
    private let size: MonarchButtonSize
    private let isIconOnly: Bool
    private let isLoading: Bool
    private let loadingLabel: String?
    private let fullWidth: Bool
    private let action: @MainActor () -> Void

    @Environment(\.monarchButtonDefersStyle) private var defersStyle

    /// A button.
    ///
    /// - Parameters:
    ///   - title: The label, already localised. With `isIconOnly` it is the spoken name only.
    ///   - systemImage: An SF Symbol before the title. Required for `isIconOnly` to show an icon.
    ///   - trailingSystemImage: An SF Symbol after the title. Not shown with `isIconOnly`.
    ///   - variant: The visual weight.
    ///   - size: The height and padding.
    ///   - isIconOnly: Whether only `systemImage` is shown, on a square button. The title stays the spoken name.
    ///   - isLoading: Whether the action is in progress.
    ///   - loadingLabel: Replaces the built-in "Loading" spoken value.
    ///   - fullWidth: Whether the button fills the available width.
    ///   - action: Runs when the button is activated and not loading.
    public init(
        _ title: String,
        systemImage: String? = nil,
        trailingSystemImage: String? = nil,
        variant: MonarchButtonVariant = .secondary,
        size: MonarchButtonSize = .md,
        isIconOnly: Bool = false,
        isLoading: Bool = false,
        loadingLabel: String? = nil,
        fullWidth: Bool = false,
        action: @escaping @MainActor () -> Void
    ) {
        self.title = title
        self.systemImage = systemImage
        self.trailingSystemImage = trailingSystemImage
        self.variant = variant
        self.size = size
        self.isIconOnly = isIconOnly
        self.isLoading = isLoading
        self.loadingLabel = loadingLabel
        self.fullWidth = fullWidth
        self.action = action
    }

    private var showsIconOnly: Bool {
        MonarchButtonTokens.showsIconOnly(isIconOnly: isIconOnly, systemImage: systemImage)
    }

    /// The content of the view.
    public var body: some View {
        let button = Button {
            if !isLoading { action() }
        } label: {
            label
        }
        .accessibilityLabel(title)
        .accessibilityValue(isLoading ? (loadingLabel ?? MonarchButtonStrings.loading) : "")
        if defersStyle {
            button
        } else {
            button.buttonStyle(
                MonarchButtonStyle(variant, size: size, fullWidth: fullWidth, isIconOnly: showsIconOnly)
            )
        }
    }

    @ViewBuilder private var label: some View {
        if showsIconOnly, let systemImage {
            if isLoading {
                ProgressView().controlSize(.small)
            } else {
                MonarchIcon(systemName: systemImage, size: .sm)
            }
        } else {
            HStack(spacing: MonarchButtonTokens.iconSpacing) {
                if isLoading {
                    ProgressView().controlSize(.small)
                } else if let systemImage {
                    MonarchIcon(systemName: systemImage, size: .sm)
                }
                Text(title)
                if let trailingSystemImage {
                    MonarchIcon(systemName: trailingSystemImage, size: .sm)
                }
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchButton") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchButtonVariant.allCases, id: \.self) { variant in
                    HStack {
                        MonarchButton("Small", variant: variant, size: .sm) {}
                        MonarchButton("Medium", systemImage: "plus", variant: variant) {}
                        MonarchButton("Large", variant: variant, size: .lg, isLoading: true) {}
                        MonarchButton("Dismiss", systemImage: "xmark", variant: variant, isIconOnly: true) {}
                    }
                }
                MonarchButton("Disabled", variant: .primary) {}.disabled(true)
            }
        }
    }
#endif
