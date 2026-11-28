import SwiftUI

/// The emoji reactions under a message, comment or post: one chip per emoji
/// with its count, selected when you reacted, and a button that opens the
/// emoji picker.
///
/// ```swift
/// MonarchReactionBar(reactions) { emoji, isReacted in
///     store.setReaction(emoji, isReacted, on: message)
/// }
/// ```
///
/// Chips wrap onto new rows. Tapping a chip calls `onToggle` with the emoji
/// and the new state; picking an emoji in the picker calls it with `true`,
/// unless you already reacted with that emoji. The product updates
/// `reactions`. Without `onToggle` the bar is read-only, has no add button,
/// and draws nothing when there are no reactions.
///
/// Accessibility: the bar is a container named "Reactions". Each chip is a
/// toggle button named by the emoji's name and count, "thumbs up, 3
/// reactions", with the selected trait when you reacted; a reacted chip
/// also has a thicker border and a bold count, so the state does not depend
/// on colour. Who reacted is offered as more content. Chips and the add
/// button have touch targets at least 44 points high.
public struct MonarchReactionBar: View {
    private let reactions: [MonarchReactionBarItem]
    private let size: MonarchReactionBarSize
    private let maxReactions: Int?
    private let isAddable: Bool
    private let quickReactions: [String]
    private let labels: MonarchReactionBarLabels
    private let onToggle: (@MainActor (String, Bool) -> Void)?

    @State private var showsPicker = false
    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    /// A reaction bar.
    ///
    /// - Parameters:
    ///   - reactions: The reactions, in display order, usually first reacted first.
    ///   - size: The size of the chips: `sm` under messages, `md` under posts.
    ///   - maxReactions: At most this many distinct reactions; the add button hides when reached.
    ///   - isAddable: Whether the add button is shown when the bar is interactive.
    ///   - quickReactions: Emoji offered first in the picker, such as the product's quick reactions.
    ///   - labels: Replacements for the built-in strings.
    ///   - onToggle: Receives the emoji and the new state. Without it the bar is read-only.
    public init(
        _ reactions: [MonarchReactionBarItem],
        size: MonarchReactionBarSize = .sm,
        maxReactions: Int? = nil,
        isAddable: Bool = true,
        quickReactions: [String] = [],
        labels: MonarchReactionBarLabels = MonarchReactionBarLabels(),
        onToggle: (@MainActor (String, Bool) -> Void)? = nil
    ) {
        self.reactions = reactions
        self.size = size
        self.maxReactions = maxReactions
        self.isAddable = isAddable
        self.quickReactions = quickReactions
        self.labels = labels
        self.onToggle = onToggle
    }

    private typealias Tokens = MonarchReactionBarTokens

    /// The content of the view.
    public var body: some View {
        let shown = Tokens.shown(reactions)
        let canAdd = Tokens.canAdd(
            isInteractive: onToggle != nil,
            isAddable: isAddable,
            shownCount: shown.count,
            maxReactions: maxReactions
        )
        if !shown.isEmpty || canAdd {
            MonarchFlowLayout(spacing: Tokens.spacing, rowSpacing: onToggle == nil ? Tokens.rowSpacing : 0) {
                ForEach(shown) { item in
                    chip(item)
                }
                if canAdd {
                    addButton
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(labels.label ?? MonarchReactionBarStrings.label)
            .sheet(isPresented: $showsPicker) {
                MonarchEmojiPicker(recents: quickReactions, labels: labels.picker) { emoji, _ in
                    showsPicker = false
                    if let emoji = Tokens.reactionToAdd(emoji, in: reactions) {
                        onToggle?(emoji, true)
                    }
                }
                .presentationDetents([.medium, .large])
                .presentationDragIndicator(.visible)
            }
        }
    }

    private var hitInset: CGFloat { MonarchReactionBarTokens.hitInset(size, isInteractive: onToggle != nil) }

    @ViewBuilder private func chip(_ item: MonarchReactionBarItem) -> some View {
        let label = MonarchReactionBarTokens.chipLabel(item, labels: labels)
        if let onToggle {
            Button {
                onToggle(item.emoji, !item.isReacted)
            } label: {
                chipFace(item)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(label)
            .accessibilityAddTraits(item.isReacted ? [.isToggle, .isSelected] : [.isToggle])
            .modifier(ReactedBy(item: item, heading: labels.reactedBy))
        } else {
            chipFace(item)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(label)
                .accessibilityAddTraits(.isImage)
                .modifier(ReactedBy(item: item, heading: labels.reactedBy))
        }
    }

    private func chipFace(_ item: MonarchReactionBarItem) -> some View {
        let appearance = MonarchReactionBarTokens.appearance(isReacted: item.isReacted, isEnabled: isEnabled)
        return HStack(spacing: MonarchReactionBarTokens.contentSpacing) {
            Text(item.emoji)
                .monarchTextStyle(MonarchReactionBarTokens.textStyle(size))
                .accessibilityHidden(true)
            Text(item.count.formatted())
                .monarchTextStyle(
                    MonarchReactionBarTokens.textStyle(size),
                    weight: appearance.isCountBold ? .bold : .regular
                )
                .monospacedDigit()
        }
        .foregroundStyle(theme.colors[role: appearance.surface.foreground])
        .padding(.horizontal, MonarchReactionBarTokens.horizontalPadding(size))
        .padding(.vertical, MonarchReactionBarTokens.verticalPadding)
        .frame(minHeight: MonarchReactionBarTokens.minHeight(size))
        .background {
            if let background = appearance.surface.background {
                Capsule().fill(theme.colors[role: background])
            }
        }
        .overlay {
            if let border = appearance.surface.border {
                Capsule().strokeBorder(theme.colors[role: border], lineWidth: appearance.borderWidth)
            }
        }
        .padding(.vertical, hitInset)
        .frame(minWidth: onToggle == nil ? 0 : MonarchAccessibility.minimumHitTarget)
        .contentShape(Rectangle())
        .monarchAnimation(.fast, value: item.isReacted)
    }

    private var addButton: some View {
        Button {
            showsPicker = true
        } label: {
            MonarchIcon(systemName: MonarchReactionBarTokens.addSymbol, size: .sm)
                .overlay(alignment: .topTrailing) {
                    // The web's smile-plus: the face with a small plus at its top right.
                    Image(systemName: "plus")
                        .font(.system(size: 7, weight: .bold))
                        .padding(1)
                        .background(theme.colors.bgCanvas, in: Circle())
                        .offset(x: 3, y: -3)
                }
                .foregroundStyle(theme.colors[role: isEnabled ? \.textMuted : \.textDisabled])
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(labels.add ?? MonarchReactionBarStrings.add)
    }
}

/// Offers who reacted to VoiceOver as more content and on pointer hover.
private struct ReactedBy: ViewModifier {
    let item: MonarchReactionBarItem
    let heading: String?

    func body(content: Content) -> some View {
        if let reactedBy = item.reactedBy, !reactedBy.isEmpty {
            content
                .accessibilityCustomContent(Text(heading ?? MonarchReactionBarStrings.reactedBy), Text(reactedBy))
                .help(reactedBy)
        } else {
            content
        }
    }
}

#if DEBUG
    #Preview("MonarchReactionBar") {
        let reactions = [
            MonarchReactionBarItem("👍", count: 3, isReacted: true, reactedBy: "You, Ana and Ben"),
            MonarchReactionBarItem("🎉", count: 1),
            MonarchReactionBarItem("❤️", count: 12),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchReactionBar(reactions) { _, _ in }
                MonarchReactionBar(reactions, size: .md) { _, _ in }
                MonarchReactionBar(reactions)
                MonarchReactionBar([]) { _, _ in }
            }
        }
    }
#endif
