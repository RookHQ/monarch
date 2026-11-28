import SwiftUI

/// A chip people act on: a label that can be removed, selected, or both.
///
/// ```swift
/// MonarchTag("Design")
/// MonarchTag("Priority: high", tone: .warning) { remove() }
/// MonarchTag("Unread", isSelected: $showsUnread)
/// ```
///
/// A tag with neither a selection binding nor a remove action is a plain
/// label. For a status that is never acted on, use ``MonarchBadge``.
///
/// Accessibility: a selectable tag is a button with the selected trait and
/// a check mark, so the state does not depend on colour. The remove button is
/// a separate element named "Remove" and the text of the tag. Both have a
/// touch target at least 44 points high. The icon is decorative.
public struct MonarchTag: View {
    private let text: String
    private let isSelected: Binding<Bool>?
    private let systemImage: String?
    private let tone: MonarchTone
    private let size: MonarchTagSize
    private let removeLabel: String?
    private let onRemove: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A tag, removable when `onRemove` is given.
    ///
    /// - Parameters:
    ///   - text: The label, already localised.
    ///   - systemImage: An SF Symbol before the text.
    ///   - tone: The meaning, which selects the colours.
    ///   - size: The height and padding.
    ///   - removeLabel: Replaces the built-in spoken name of the remove button.
    ///   - onRemove: Runs when the remove button is activated. No button without it.
    public init(
        _ text: String,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        size: MonarchTagSize = .md,
        removeLabel: String? = nil,
        onRemove: (@MainActor () -> Void)? = nil
    ) {
        self.text = text
        self.isSelected = nil
        self.systemImage = systemImage
        self.tone = tone
        self.size = size
        self.removeLabel = removeLabel
        self.onRemove = onRemove
    }

    /// A tag that toggles a selection, for example a filter.
    ///
    /// - Parameters:
    ///   - text: The label, already localised.
    ///   - isSelected: Whether the tag is on. Activating the tag toggles it.
    ///   - systemImage: An SF Symbol before the text.
    ///   - tone: The meaning, which selects the colours while the tag is off.
    ///   - size: The height and padding.
    ///   - removeLabel: Replaces the built-in spoken name of the remove button.
    ///   - onRemove: Runs when the remove button is activated. No button without it.
    public init(
        _ text: String,
        isSelected: Binding<Bool>,
        systemImage: String? = nil,
        tone: MonarchTone = .neutral,
        size: MonarchTagSize = .md,
        removeLabel: String? = nil,
        onRemove: (@MainActor () -> Void)? = nil
    ) {
        self.text = text
        self.isSelected = isSelected
        self.systemImage = systemImage
        self.tone = tone
        self.size = size
        self.removeLabel = removeLabel
        self.onRemove = onRemove
    }

    private var isInteractive: Bool { isSelected != nil || onRemove != nil }
    private var hitInset: CGFloat { MonarchTagTokens.hitInset(size, isInteractive: isInteractive) }

    /// The content of the view.
    public var body: some View {
        let appearance = MonarchTagTokens.appearance(
            tone: tone,
            isSelected: isSelected?.wrappedValue ?? false,
            isEnabled: isEnabled
        )
        HStack(spacing: 0) {
            if let isSelected {
                Button {
                    isSelected.wrappedValue.toggle()
                } label: {
                    label.contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected.wrappedValue ? .isSelected : [])
                .sensoryFeedback(.selection, trigger: isSelected.wrappedValue)
            } else {
                label.accessibilityElement(children: .combine)
            }
            if let onRemove {
                removeButton(onRemove)
            }
        }
        .foregroundStyle(theme.colors[role: appearance.foreground])
        .background {
            if let background = appearance.background {
                MonarchShapes.control.fill(theme.colors[role: background]).padding(.vertical, hitInset)
            }
        }
        .overlay {
            if let border = appearance.border {
                MonarchShapes.control
                    .strokeBorder(theme.colors[role: border], lineWidth: MonarchTagTokens.borderWidth)
                    .padding(.vertical, hitInset)
                    .allowsHitTesting(false)
            }
        }
        .monarchAnimation(.fast, value: isSelected?.wrappedValue)
    }

    private var label: some View {
        HStack(spacing: MonarchTagTokens.iconSpacing) {
            if isSelected?.wrappedValue == true {
                MonarchIcon(systemName: MonarchTagTokens.selectedSymbol, size: .sm)
                    .imageScale(.small)
            }
            if let systemImage {
                MonarchIcon(systemName: systemImage, size: .sm)
            }
            Text(text)
                .monarchTextStyle(MonarchTagTokens.textStyle(size), weight: MonarchTagTokens.fontWeight)
                .lineLimit(1)
                .fixedSize(horizontal: true, vertical: false)
        }
        .padding(.leading, MonarchTagTokens.horizontalPadding(size))
        .padding(.trailing, onRemove == nil ? MonarchTagTokens.horizontalPadding(size) : 0)
        .padding(.vertical, MonarchTagTokens.verticalPadding + hitInset)
        .frame(minHeight: MonarchTagTokens.minHeight(size) + hitInset * 2)
    }

    private func removeButton(_ onRemove: @escaping @MainActor () -> Void) -> some View {
        Button(action: onRemove) {
            MonarchIcon(systemName: MonarchTagTokens.removeSymbol, size: .sm)
                .imageScale(.small)
                .padding(.trailing, MonarchTagTokens.horizontalPadding(size))
                .frame(
                    minWidth: MonarchAccessibility.minimumHitTarget,
                    minHeight: MonarchAccessibility.minimumHitTarget,
                    alignment: .trailing
                )
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .padding(.leading, -MonarchTagTokens.removeOverlap)
        .accessibilityLabel(MonarchTagTokens.removeLabel(text: text, override: removeLabel))
    }
}

#if DEBUG
    #Preview("MonarchTag") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchTagSize.allCases, id: \.self) { size in
                    HStack {
                        ForEach(MonarchTone.allCases, id: \.self) { tone in
                            MonarchTag("Label", tone: tone, size: size)
                        }
                    }
                }
                MonarchTag("Removable", systemImage: "tag") {}
                MonarchTag("Selected", isSelected: .constant(true))
                MonarchTag("Not selected", isSelected: .constant(false)) {}
                MonarchTag("Disabled", isSelected: .constant(true)) {}.disabled(true)
            }
        }
    }
#endif
