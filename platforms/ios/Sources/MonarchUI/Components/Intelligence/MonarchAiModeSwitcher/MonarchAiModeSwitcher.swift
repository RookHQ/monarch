import SwiftUI

/// Switches how the assistant works (Ask, Edit, Agent) with the selected
/// mode's description underneath, so the consequence of the choice is
/// always visible.
///
/// ```swift
/// MonarchAiModeSwitcher(selection: $mode)
/// MonarchAiModeSwitcher(selection: $mode, modes: myModes, size: .md, showsDescription: false)
/// ```
///
/// It is the standard segmented `Picker`, for 2 to 5 short modes. Disable
/// every mode with the standard `disabled(_:)` modifier, for example while
/// a response runs. A disabled mode is dimmed where the system supports it
/// and can never become the selection.
///
/// Accessibility: the system segmented control, named by `label` ("Mode"),
/// with the selected mode's description as its hint. When the selection
/// changes, the new description is announced. An icon-only segment is read
/// as the mode's label.
public struct MonarchAiModeSwitcher: View {
    @Binding private var selection: String
    private let modes: [MonarchAiMode]
    private let size: MonarchAiModeSwitcherSize
    private let showsDescription: Bool
    private let label: String?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A mode switcher.
    ///
    /// - Parameters:
    ///   - selection: The id of the selected mode.
    ///   - modes: The modes, 2 to 5. Ask, Edit and Agent by default.
    ///   - size: The control size. `sm` fits a prompt toolbar.
    ///   - showsDescription: Whether the selected mode's description is shown
    ///     under the control. Turn it off when it is shown elsewhere.
    ///   - label: Replaces the built-in name of the choice, "Mode".
    public init(
        selection: Binding<String>,
        modes: [MonarchAiMode] = MonarchAiMode.defaults,
        size: MonarchAiModeSwitcherSize = .sm,
        showsDescription: Bool = true,
        label: String? = nil
    ) {
        _selection = selection
        self.modes = modes
        self.size = size
        self.showsDescription = showsDescription
        self.label = label
    }

    private var title: String { label ?? MonarchAiModeSwitcherStrings.label }
    private var description: String? {
        MonarchAiModeSwitcherTokens.description(selection: selection, modes: modes)
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiModeSwitcherTokens.spacing) {
            MonarchSegmentedControl(
                title,
                selection: guarded,
                items: modes.map {
                    MonarchSegmentedItem(
                        $0.label,
                        value: $0.id,
                        systemImage: $0.systemImage,
                        isIconOnly: $0.isIconOnly,
                        isDisabled: $0.isDisabled
                    )
                },
                size: MonarchAiModeSwitcherTokens.segmentedSize(size)
            )
            .accessibilityHint(description ?? "")
            if MonarchAiModeSwitcherTokens.describes(modes: modes, showsDescription: showsDescription) {
                Text(description ?? "")
                    .monarchTextStyle(.caption)
                    .foregroundStyle(
                        theme.colors[
                            role: isEnabled
                                ? MonarchAiModeSwitcherTokens.description
                                : MonarchAiModeSwitcherTokens.disabledDescription
                        ]
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .accessibilityHidden(true)
            }
        }
        .onChange(of: selection) {
            if showsDescription, let description {
                AccessibilityNotification.Announcement(description).post()
            }
        }
    }

    private var guarded: Binding<String> {
        Binding(
            get: { selection },
            set: { id in
                if MonarchAiModeSwitcherTokens.accepts(id, modes: modes) {
                    selection = id
                }
            }
        )
    }
}

#if DEBUG
    #Preview("MonarchAiModeSwitcher") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchAiModeSwitcher(selection: .constant("ask"))
                MonarchAiModeSwitcher(selection: .constant("edit"), size: .md, showsDescription: false)
                MonarchAiModeSwitcher(selection: .constant("agent")).disabled(true)
            }
        }
    }
#endif
