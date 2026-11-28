import SwiftUI

/// The small "i" button next to a label that explains it: a toggletip that
/// opens a popover on tap, not a hover tooltip.
///
/// ```swift
/// HStack {
///     Text("Visibility score")
///     MonarchInfoTooltip(
///         "How often your listing appears in search, from 0 to 100.",
///         label: "About visibility score"
///     )
/// }
/// ```
///
/// The explanation stays a popover on a phone, anchored to the button, and
/// closes on a tap outside or a second tap on the button. Keep it to a
/// sentence or two. Nothing essential should live only in the explanation.
///
/// Accessibility: the button is named by `label` ("More information" when
/// none is given; name what it explains). The glyph is hidden. When the
/// popover opens the system moves VoiceOver focus into it, and the escape
/// gesture closes it. The button has a touch target of at least 44 points
/// and the glyph and text scale with Dynamic Type; a long explanation
/// scrolls at the largest sizes.
public struct MonarchInfoTooltip: View {
    private let message: String
    private let label: String?
    private let icon: MonarchInfoTooltipIcon
    private let arrowEdge: Edge
    private let isPresented: Binding<Bool>?

    @State private var isOpenState = false
    @Environment(\.monarchTheme) private var theme

    /// An info button with an explanation.
    ///
    /// - Parameters:
    ///   - message: The explanation, already localised. A sentence or two.
    ///   - label: The spoken name of the button. Name what it explains, for example
    ///     "About visibility score". Replaces the built-in "More information".
    ///   - icon: An "i" or a question mark.
    ///   - arrowEdge: The edge of the popover the arrow is on. The system moves it when there is no room.
    ///   - isPresented: Whether the explanation is shown, when the app controls it. Without it
    ///     the button keeps its own state.
    public init(
        _ message: String,
        label: String? = nil,
        icon: MonarchInfoTooltipIcon = .info,
        arrowEdge: Edge = .top,
        isPresented: Binding<Bool>? = nil
    ) {
        self.message = message
        self.label = label
        self.icon = icon
        self.arrowEdge = arrowEdge
        self.isPresented = isPresented
    }

    /// The content of the view.
    public var body: some View {
        let isOpen = isPresented ?? $isOpenState
        Button {
            isOpen.wrappedValue.toggle()
        } label: {
            MonarchIcon(systemName: MonarchInfoTooltipTokens.symbol(icon))
        }
        .buttonStyle(MonarchInfoTooltipButtonStyle(isOpen: isOpen.wrappedValue))
        .accessibilityLabel(MonarchInfoTooltipTokens.label(label))
        .popover(isPresented: isOpen, arrowEdge: arrowEdge) {
            ViewThatFits(in: .vertical) {
                bubble
                ScrollView { bubble }
            }
            .presentationCompactAdaptation(.popover)
            .presentationBackground(theme.colors[role: MonarchInfoTooltipTokens.bubbleColor])
        }
    }

    private var bubble: some View {
        Text(message)
            .monarchTextStyle(MonarchInfoTooltipTokens.textStyle)
            .foregroundStyle(theme.colors[role: MonarchInfoTooltipTokens.textColor])
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
            .frame(idealWidth: MonarchInfoTooltipTokens.idealWidth, alignment: .leading)
            .padding(MonarchInfoTooltipTokens.padding)
    }
}

/// Colours the glyph for the pressed, open and disabled states and gives it the touch target.
private struct MonarchInfoTooltipButtonStyle: ButtonStyle {
    let isOpen: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchInfoTooltipButtonBody(configuration: configuration, isOpen: isOpen)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchInfoTooltipButtonBody: View {
    let configuration: ButtonStyleConfiguration
    let isOpen: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let role = MonarchInfoTooltipTokens.iconColor(
            isPressed: configuration.isPressed,
            isOpen: isOpen,
            isEnabled: isEnabled
        )
        configuration.label
            .foregroundStyle(theme.colors[role: role])
            .monarchMinimumHitTarget()
            .contentShape(Rectangle())
            .monarchAnimation(.fast, value: configuration.isPressed)
    }
}

#if DEBUG
    #Preview("MonarchInfoTooltip") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchInfoTooltipIcon.allCases, id: \.self) { icon in
                    HStack {
                        Text("Visibility score").monarchTextStyle(.label)
                        MonarchInfoTooltip(
                            "How often your listing appears in search, from 0 to 100.",
                            label: "About visibility score",
                            icon: icon
                        )
                    }
                }
                MonarchInfoTooltip("Unavailable").disabled(true)
            }
        }
    }
#endif
