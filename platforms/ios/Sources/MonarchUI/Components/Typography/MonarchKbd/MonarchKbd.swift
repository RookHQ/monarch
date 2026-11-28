import SwiftUI

/// A keyboard key or shortcut drawn as a key cap, such as ⌘K.
///
/// ```swift
/// HStack {
///     Text("Search")
///     Spacer()
///     MonarchKbd("⌘K")
/// }
/// ```
///
/// A shortcut is of use only with a hardware keyboard, so by default the
/// key cap is drawn only while one is attached, and takes no room
/// otherwise. Pass `isAlwaysVisible: true` where the shortcut is the
/// content, for example in a list of shortcuts.
///
/// Accessibility: the cap is one element. VoiceOver reads the keys by name
/// ("Command K"), not as symbols. The text scales with Dynamic Type and the
/// cap grows with it. The cap is not interactive.
public struct MonarchKbd: View {
    private let keys: String
    private let labels: MonarchKbdLabels
    private let isAlwaysVisible: Bool

    @State private var isKeyboardAttached = MonarchKbdKeyboard.isAttached

    /// A key cap.
    ///
    /// - Parameters:
    ///   - keys: The key or shortcut, such as "⌘K", "Esc" or "⇧⌘P".
    ///   - labels: Replacements for the spoken names of the key glyphs.
    ///   - isAlwaysVisible: Whether the cap is drawn without a hardware
    ///     keyboard too.
    public init(
        _ keys: String,
        labels: MonarchKbdLabels = MonarchKbdLabels(),
        isAlwaysVisible: Bool = false
    ) {
        self.keys = keys
        self.labels = labels
        self.isAlwaysVisible = isAlwaysVisible
    }

    /// The content of the view.
    public var body: some View {
        // A stack, not a group, so that the keyboard is watched while
        // nothing is drawn.
        ZStack {
            if MonarchKbdTokens.isVisible(isAlwaysVisible: isAlwaysVisible, isKeyboardAttached: isKeyboardAttached) {
                MonarchKbdCap(keys: keys)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(MonarchKbdTokens.spokenText(for: keys, labels: labels))
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: MonarchKbdKeyboard.didConnect)) { _ in
            isKeyboardAttached = MonarchKbdKeyboard.isAttached
        }
        .onReceive(NotificationCenter.default.publisher(for: MonarchKbdKeyboard.didDisconnect)) { _ in
            isKeyboardAttached = MonarchKbdKeyboard.isAttached
        }
    }
}

/// The key cap: the keys on a filled shape with a heavier bottom edge.
private struct MonarchKbdCap: View {
    let keys: String

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var minimumSize = MonarchKbdTokens.minimumSize

    var body: some View {
        let appearance = MonarchKbdTokens.appearance
        Text(keys)
            .monarchTextStyle(MonarchKbdTokens.textStyle)
            .multilineTextAlignment(.center)
            .foregroundStyle(theme.colors[role: appearance.foreground])
            .padding(.horizontal, MonarchKbdTokens.horizontalPadding)
            .padding(.top, MonarchKbdTokens.verticalPadding)
            .padding(.bottom, MonarchKbdTokens.verticalPadding + MonarchKbdTokens.bottomEdgeWidth)
            .frame(minWidth: minimumSize, minHeight: minimumSize)
            .background {
                ZStack {
                    if let border = appearance.border {
                        MonarchShapes.mark.fill(theme.colors[role: border])
                    }
                    if let background = appearance.background {
                        MonarchShapes.mark
                            .fill(theme.colors[role: background])
                            .padding(.horizontal, MonarchShapes.borderWidth)
                            .padding(.top, MonarchShapes.borderWidth)
                            .padding(.bottom, MonarchKbdTokens.bottomEdgeWidth)
                    }
                }
            }
    }
}

#if DEBUG
    #Preview("MonarchKbd") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack {
                    MonarchKbd("⌘K", isAlwaysVisible: true)
                    MonarchKbd("⇧⌘P", isAlwaysVisible: true)
                    MonarchKbd("Esc", isAlwaysVisible: true)
                    MonarchKbd("↩", isAlwaysVisible: true)
                }
                HStack {
                    Text("Search")
                    MonarchKbd("⌘F")
                }
            }
        }
    }
#endif
