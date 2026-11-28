import SwiftUI

/// An emoji picker: search, categories, recently used emoji and skin tones,
/// with a compact built-in emoji index and no dependency.
///
/// ```swift
/// MonarchEmojiPicker(recents: recents, skinTone: $skinTone) { emoji, _ in
///     message.append(emoji)
///     recents = MonarchEmojiPicker.pushingRecent(emoji, to: recents)
/// }
/// ```
///
/// iOS has no emoji picker outside the keyboard, so Monarch draws one. Use
/// it inline, in a `popover`, or in a sheet with medium detents. The built-in
/// index has about 500 common emoji with English names; pass `categories`
/// to offer a full set or names in another language. The app stores the
/// recents and the skin tone.
///
/// Accessibility: each emoji is a button named by its name ("thumbs up"),
/// not by its character. The category buttons are named by category and the
/// current one has the selected trait. The skin tone menu is a standard
/// picker. The number of results is announced after each search. Every cell
/// is at least 44 points and grows with Dynamic Type.
public struct MonarchEmojiPicker: View {
    private let categories: [MonarchEmojiPickerCategory]
    private let recents: [String]
    private let maxRecents: Int
    private let skinToneBinding: Binding<MonarchEmojiSkinTone>?
    private let showsSkinTones: Bool
    private let labels: MonarchEmojiPickerLabels
    private let onSelect: @MainActor (String, MonarchEmojiPickerItem) -> Void

    @State private var query = ""
    @State private var ownSkinTone = MonarchEmojiSkinTone.none
    @State private var currentSection: String?

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric private var cellSize: CGFloat = MonarchEmojiPickerTokens.cellSize
    @ScaledMetric private var gridHeight: CGFloat = MonarchEmojiPickerTokens.gridHeight

    private typealias Tokens = MonarchEmojiPickerTokens

    /// An emoji picker.
    ///
    /// - Parameters:
    ///   - categories: The emoji offered, by category. Default: the built-in index.
    ///   - recents: Recently used emoji, newest first, shown as the first
    ///     category. The app stores them.
    ///   - maxRecents: The most recents shown.
    ///   - skinTone: The skin tone for emoji that take one. Without a binding
    ///     the picker keeps the choice itself.
    ///   - showsSkinTones: Whether the skin tone menu is shown.
    ///   - labels: Replacements for the built-in strings.
    ///   - onSelect: Called with the chosen emoji, skin tone applied, and its item.
    public init(
        categories: [MonarchEmojiPickerCategory] = MonarchEmojiPickerCategory.defaults,
        recents: [String] = [],
        maxRecents: Int = 24,
        skinTone: Binding<MonarchEmojiSkinTone>? = nil,
        showsSkinTones: Bool = true,
        labels: MonarchEmojiPickerLabels = MonarchEmojiPickerLabels(),
        onSelect: @escaping @MainActor (String, MonarchEmojiPickerItem) -> Void
    ) {
        self.categories = categories
        self.recents = recents
        self.maxRecents = maxRecents
        self.skinToneBinding = skinTone
        self.showsSkinTones = showsSkinTones
        self.labels = labels
        self.onSelect = onSelect
    }

    private var skinTone: Binding<MonarchEmojiSkinTone> {
        skinToneBinding ?? $ownSkinTone
    }

    private var sections: [Tokens.Section] {
        Tokens.sections(
            query: query,
            categories: categories,
            recents: recents,
            maxRecents: maxRecents,
            tone: skinTone.wrappedValue,
            labels: labels
        )
    }

    private var isSearching: Bool {
        !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    /// The content of the view.
    public var body: some View {
        let sections = sections
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            HStack(spacing: Tokens.spacing) {
                MonarchEmojiPickerSearchField(
                    prompt: labels.search ?? MonarchEmojiPickerStrings.search,
                    clearLabel: labels.clearSearch ?? MonarchEmojiPickerStrings.clearSearch,
                    text: $query
                )
                if showsSkinTones {
                    skinToneMenu
                }
            }
            ScrollViewReader { proxy in
                VStack(alignment: .leading, spacing: Tokens.spacing) {
                    if !isSearching {
                        categoryBar(sections, proxy: proxy)
                    }
                    grid(sections)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchEmojiPickerStrings.label)
        .onChange(of: query) { _, newValue in
            let trimmed = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else { return }
            let count = Tokens.search(trimmed, in: categories).count
            AccessibilityNotification.Announcement(Tokens.resultsAnnouncement(count, labels: labels)).post()
        }
    }

    private var skinToneMenu: some View {
        Menu {
            Picker(labels.skinTone ?? MonarchEmojiPickerStrings.skinTone, selection: skinTone) {
                ForEach(MonarchEmojiSkinTone.allCases, id: \.self) { tone in
                    Text(
                        Tokens.applying(tone, to: Tokens.skinToneSample) + " "
                            + Tokens.skinToneName(tone, labels: labels)
                    )
                    .tag(tone)
                }
            }
            .pickerStyle(.inline)
        } label: {
            Text(Tokens.applying(skinTone.wrappedValue, to: Tokens.skinToneSample))
                .monarchTextStyle(Tokens.emojiStyle)
                .monarchMinimumHitTarget()
        }
        .buttonStyle(.plain)
        .accessibilityLabel(labels.skinTone ?? MonarchEmojiPickerStrings.skinTone)
        .accessibilityValue(Tokens.skinToneName(skinTone.wrappedValue, labels: labels))
    }

    private func categoryBar(_ sections: [Tokens.Section], proxy: ScrollViewProxy) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Tokens.cellSpacing) {
                ForEach(sections) { section in
                    let isCurrent = (currentSection ?? sections.first?.id) == section.id
                    Button {
                        currentSection = section.id
                        proxy.scrollTo(section.id, anchor: .top)
                    } label: {
                        Text(section.symbol)
                            .monarchTextStyle(Tokens.categoryStyle)
                            .frame(minWidth: cellSize, minHeight: cellSize)
                            .background {
                                if isCurrent {
                                    MonarchShapes.control.fill(theme.colors[role: Tokens.selectedCategoryFill])
                                }
                            }
                            .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(section.label)
                    .accessibilityAddTraits(isCurrent ? .isSelected : [])
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.categoryBar ?? MonarchEmojiPickerStrings.categoryBar)
    }

    private func grid(_ sections: [Tokens.Section]) -> some View {
        ScrollView {
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: cellSize), spacing: Tokens.cellSpacing)],
                alignment: .leading,
                spacing: Tokens.cellSpacing
            ) {
                ForEach(sections) { section in
                    Section {
                        ForEach(section.entries) { entry in
                            cell(entry)
                        }
                    } header: {
                        Text(section.label)
                            .monarchTextStyle(Tokens.headingStyle)
                            .foregroundStyle(theme.colors[role: Tokens.headingColor])
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .accessibilityAddTraits(.isHeader)
                            .id(section.id)
                    }
                }
            }
            if isSearching, sections.first?.entries.isEmpty ?? true {
                Text(labels.noResults ?? MonarchEmojiPickerStrings.noResults)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors[role: Tokens.headingColor])
                    .frame(maxWidth: .infinity)
            }
        }
        .frame(height: gridHeight)
    }

    private func cell(_ entry: Tokens.Entry) -> some View {
        Button {
            onSelect(entry.display, entry.item)
        } label: {
            Text(entry.display)
                .monarchTextStyle(Tokens.emojiStyle)
                .frame(minWidth: cellSize, minHeight: cellSize)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(entry.item.name)
    }
}

extension MonarchEmojiPicker {
    /// The emoji whose name or keywords match every word of the query, each
    /// word as the start of a word of the name or of a keyword. Names that
    /// start with the query come first. Useful for `:shortcode` completion
    /// in a composer.
    ///
    /// - Parameters:
    ///   - query: The words to find.
    ///   - categories: Where to look. Default: the built-in index.
    ///   - limit: The most results.
    /// - Returns: The matching emoji, without duplicates.
    nonisolated public static func search(
        _ query: String,
        in categories: [MonarchEmojiPickerCategory] = MonarchEmojiPickerCategory.defaults,
        limit: Int = .max
    ) -> [MonarchEmojiPickerItem] {
        MonarchEmojiPickerTokens.search(query, in: categories, limit: limit)
    }

    /// The emoji with a skin tone applied after its first character.
    ///
    /// - Parameters:
    ///   - tone: The skin tone. `none` returns the emoji unchanged.
    ///   - emoji: An emoji that takes a skin tone.
    /// - Returns: The emoji with the tone.
    nonisolated public static func applying(_ tone: MonarchEmojiSkinTone, to emoji: String) -> String {
        MonarchEmojiPickerTokens.applying(tone, to: emoji)
    }

    /// A new recents list with the emoji first, without duplicates. Store
    /// the result wherever the app keeps preferences.
    ///
    /// - Parameters:
    ///   - emoji: The emoji just chosen.
    ///   - recents: The current recents, newest first.
    ///   - max: The longest list kept.
    /// - Returns: The new recents.
    nonisolated public static func pushingRecent(_ emoji: String, to recents: [String], max: Int = 24) -> [String] {
        MonarchEmojiPickerTokens.pushingRecent(emoji, to: recents, max: max)
    }
}

#if DEBUG
    #Preview("MonarchEmojiPicker") {
        MonarchPreviewMatrix {
            MonarchEmojiPicker(recents: ["👍", "🎉"]) { _, _ in }
        }
    }
#endif
