import SwiftUI

/// Light, dark or system appearance, bound to the scheme input of the
/// Monarch theme.
///
/// ```swift
/// @AppStorage("theme") private var stored = "system"
///
/// MonarchThemeToggle(selection: $scheme)
/// ContentView().monarchTheme(scheme: scheme)
/// ```
///
/// The toggle only changes the binding. The app applies it with
/// `.monarchTheme(scheme:)` and keeps it, for example in `UserDefaults`
/// through `@AppStorage`. ``storageValue(for:)`` and
/// ``preference(storageValue:)`` convert the choice to and from the text the
/// web toggle stores (`light`, `dark`, `system`).
///
/// The `segmented` variant is a standard segmented `Picker`. The `menu`
/// variant is a standard `Menu` with an inline `Picker` inside, drawn with the
/// Monarch ghost button style; its button shows the current choice's symbol
/// and, unless `showsLabel` is false, its name.
///
/// Accessibility: both variants keep the system picker, so VoiceOver reads
/// the choices as selectable items and the current one as selected. The
/// menu button is named "Theme" and its value is the current choice. The
/// choices are named in words, not only by symbol.
public struct MonarchThemeToggle: View {
    @Binding private var selection: MonarchSchemePreference
    private let variant: MonarchThemeToggleVariant
    private let size: MonarchThemeToggleSize
    private let showsLabel: Bool
    private let labels: MonarchThemeToggleLabels

    /// A theme toggle.
    ///
    /// - Parameters:
    ///   - selection: The chosen scheme. Pass it to `.monarchTheme(scheme:)`.
    ///   - variant: Three segments, or a button with a menu.
    ///   - size: The size of the segments or the button.
    ///   - showsLabel: `menu` only: whether the button shows the name of the
    ///     current choice beside its symbol.
    ///   - labels: Replacements for the built-in strings.
    public init(
        selection: Binding<MonarchSchemePreference>,
        variant: MonarchThemeToggleVariant = .segmented,
        size: MonarchThemeToggleSize = .md,
        showsLabel: Bool = true,
        labels: MonarchThemeToggleLabels = MonarchThemeToggleLabels()
    ) {
        _selection = selection
        self.variant = variant
        self.size = size
        self.showsLabel = showsLabel
        self.labels = labels
    }

    /// The text to store for a choice: `light`, `dark` or `system`, the same
    /// values the web toggle writes to the shared `rook_theme` cookie.
    nonisolated public static func storageValue(for preference: MonarchSchemePreference) -> String {
        MonarchThemeToggleTokens.storageValue(preference)
    }

    /// The choice for stored text, or `nil` when the text is missing or not
    /// one of `light`, `dark` and `system`.
    nonisolated public static func preference(storageValue: String?) -> MonarchSchemePreference? {
        MonarchThemeToggleTokens.preference(storageValue: storageValue)
    }

    /// The content of the view.
    public var body: some View {
        let title = MonarchThemeToggleTokens.title(labels: labels)
        switch variant {
        case .segmented:
            MonarchSegmentedControl(
                title,
                selection: $selection,
                items: MonarchThemeToggleTokens.order.map { preference in
                    MonarchSegmentedItem(
                        MonarchThemeToggleTokens.name(preference, labels: labels),
                        value: preference,
                        systemImage: MonarchThemeToggleTokens.systemImage(preference)
                    )
                },
                size: MonarchThemeToggleTokens.buttonSize(size),
                fillsWidth: false
            )
        case .menu:
            let current = MonarchThemeToggleTokens.name(selection, labels: labels)
            Menu {
                Picker(title, selection: $selection) {
                    ForEach(MonarchThemeToggleTokens.order, id: \.self) { preference in
                        Label(
                            MonarchThemeToggleTokens.name(preference, labels: labels),
                            systemImage: MonarchThemeToggleTokens.systemImage(preference)
                        )
                        .tag(preference)
                    }
                }
                .pickerStyle(.inline)
            } label: {
                if showsLabel {
                    Label(current, systemImage: MonarchThemeToggleTokens.systemImage(selection))
                } else {
                    Label(current, systemImage: MonarchThemeToggleTokens.systemImage(selection))
                        .labelStyle(.iconOnly)
                }
            }
            .menuStyle(.button)
            .buttonStyle(.monarch(.ghost, size: MonarchThemeToggleTokens.buttonSize(size)))
            .fixedSize()
            .accessibilityLabel(title)
            .accessibilityValue(current)
        }
    }
}

#if DEBUG
    #Preview("MonarchThemeToggle") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchThemeToggle(selection: .constant(.system))
                MonarchThemeToggle(selection: .constant(.dark), size: .sm)
                HStack {
                    MonarchThemeToggle(selection: .constant(.light), variant: .menu)
                    MonarchThemeToggle(selection: .constant(.dark), variant: .menu, size: .sm, showsLabel: false)
                }
                MonarchThemeToggle(selection: .constant(.light)).disabled(true)
            }
        }
    }
#endif
