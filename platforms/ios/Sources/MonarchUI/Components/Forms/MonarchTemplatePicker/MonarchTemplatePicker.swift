import SwiftUI

/// A gallery of templates to start from: category filters, a grid of
/// selectable cards and an optional "start from scratch" card first.
///
/// ```swift
/// NavigationStack {
///     ScrollView {
///         MonarchTemplatePicker(
///             "Templates",
///             selection: $templateID,
///             templates: templates,
///             categories: categories,
///             query: query,
///             blank: .blank()
///         )
///         .padding()
///     }
///     .searchable(text: $query, prompt: "Search templates")
/// }
/// ```
///
/// Search uses the system search field: put `searchable` on the screen that
/// shows the picker and pass its text as `query`. The app owns what applying
/// a template does; the picker only writes the chosen id.
///
/// Accessibility: the grid is a container named by `label`. Each card is one
/// button whose label is the template name, whose value is the badge and the
/// description, and which has the selected trait when chosen; it is at least
/// 44 points high. Selection is also shown by a check mark and a heavier
/// border, not colour alone. The category filters are toggle buttons in a
/// group named "Categories". When the filters change, the number of
/// templates shown is announced. The preview is decorative.
public struct MonarchTemplatePicker: View {
    private let label: String
    @Binding private var selection: String?
    private let templates: [MonarchTemplatePickerItem]
    private let categories: [MonarchTemplatePickerCategory]
    private let query: String
    private let blank: MonarchTemplatePickerItem?
    private let labels: MonarchTemplatePickerLabels

    @State private var category: String?
    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var minCardWidth = MonarchTemplatePickerTokens.minCardWidth

    /// A template picker.
    ///
    /// - Parameters:
    ///   - label: The name of the gallery, already localised. VoiceOver reads it; it is not shown.
    ///   - selection: The id of the chosen template, or `nil` for none yet.
    ///   - templates: The templates, in order.
    ///   - categories: Category filters, in order. Shown with an "All" filter first when given.
    ///   - query: The search text from the screen's `searchable` field.
    ///   - blank: A "start from scratch" card shown first, usually `.blank()`.
    ///   - initialCategory: The category selected at first. All by default.
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ label: String,
        selection: Binding<String?>,
        templates: [MonarchTemplatePickerItem],
        categories: [MonarchTemplatePickerCategory] = [],
        query: String = "",
        blank: MonarchTemplatePickerItem? = nil,
        initialCategory: String? = nil,
        labels: MonarchTemplatePickerLabels = MonarchTemplatePickerLabels()
    ) {
        self.label = label
        _selection = selection
        self.templates = templates
        self.categories = categories
        self.query = query
        self.blank = blank
        self.labels = labels
        _category = State(initialValue: initialCategory)
    }

    private var shown: [MonarchTemplatePickerItem] {
        MonarchTemplatePickerTokens.filtered(templates, category: category, query: query)
    }

    private var isFiltering: Bool {
        MonarchTemplatePickerTokens.isFiltering(category: category, query: query)
    }

    private func resultsText(_ count: Int) -> String {
        labels.results?(count) ?? MonarchTemplatePickerStrings.results(count)
    }

    /// The content of the view.
    public var body: some View {
        let shown = shown
        VStack(alignment: .leading, spacing: MonarchTemplatePickerTokens.sectionSpacing) {
            if !categories.isEmpty {
                filters
            }
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: minCardWidth), spacing: MonarchTemplatePickerTokens.gridSpacing)],
                alignment: .leading,
                spacing: MonarchTemplatePickerTokens.gridSpacing
            ) {
                ForEach((blank.map { [$0] } ?? []) + shown) { item in
                    card(item)
                }
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(label)
            if shown.isEmpty && isFiltering {
                emptyState
            }
        }
        .onChange(of: shown.map(\.id)) { _, ids in
            if isFiltering { AccessibilityNotification.Announcement(resultsText(ids.count)).post() }
        }
    }

    private var filters: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: MonarchTemplatePickerTokens.filterSpacing) {
                filter(labels.allCategories ?? MonarchTemplatePickerStrings.allCategories, id: nil)
                ForEach(categories) { item in
                    filter(item.label, id: item.id)
                }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.categories ?? MonarchTemplatePickerStrings.categories)
    }

    private func filter(_ title: String, id: String?) -> some View {
        MonarchTag(
            title,
            isSelected: Binding(
                get: { category == id },
                set: { isOn in
                    if isOn { category = id }
                }
            )
        )
    }

    @ViewBuilder private var emptyState: some View {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        MonarchEmptyState(
            trimmed.isEmpty
                ? resultsText(0) : (labels.noResults?(trimmed) ?? MonarchTemplatePickerStrings.noResults(trimmed)),
            kind: .filtered,
            size: .sm
        )
    }

    private func card(_ item: MonarchTemplatePickerItem) -> some View {
        let isSelected = selection == item.id
        return Button {
            selection = item.id
        } label: {
            MonarchTemplatePickerCardLabel(item: item, isSelected: isSelected)
        }
        .buttonStyle(MonarchTemplatePickerCardStyle(isSelected: isSelected))
        .disabled(item.isDisabled)
        .accessibilityLabel(item.name)
        .accessibilityValue(
            MonarchTemplatePickerTokens.accessibilityValue(badge: item.badge, description: item.description)
        )
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// The preview, name, badge and description of a template card.
private struct MonarchTemplatePickerCardLabel: View {
    let item: MonarchTemplatePickerItem
    let isSelected: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchTemplatePickerTokens.appearance(
            isSelected: isSelected,
            isPressed: false,
            isEnabled: isEnabled
        )
        let previewShape = RoundedRectangle(cornerRadius: MonarchTemplatePickerTokens.previewRadius, style: .continuous)
        VStack(alignment: .leading, spacing: MonarchTemplatePickerTokens.cardSpacing) {
            ZStack {
                previewShape.fill(theme.colors.bgSubtle)
                if let image = item.image {
                    image.resizable().scaledToFill()
                } else {
                    MonarchIcon(
                        systemName: item.systemImage ?? MonarchTemplatePickerTokens.placeholderSymbol,
                        size: .lg
                    )
                    .foregroundStyle(theme.colors[role: appearance.icon])
                }
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(MonarchTemplatePickerTokens.previewAspectRatio, contentMode: .fit)
            .clipShape(previewShape)
            .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: MonarchTemplatePickerTokens.textSpacing) {
                Text(item.name)
                    .monarchTextStyle(.label)
                    .foregroundStyle(theme.colors[role: appearance.title])
                if let badge = item.badge, !badge.isEmpty {
                    MonarchBadge(badge, tone: .info)
                }
                if let description = item.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: appearance.secondary])
                }
            }
            .multilineTextAlignment(.leading)
        }
        .frame(maxWidth: .infinity, alignment: .topLeading)
        .overlay(alignment: .topTrailing) {
            if isSelected {
                MonarchIcon(systemName: MonarchTemplatePickerTokens.checkSymbol, size: .md)
                    .foregroundStyle(theme.colors[role: appearance.mark])
                    .padding(MonarchTemplatePickerTokens.checkInset)
            }
        }
    }
}

/// Draws the card surface for the pressed, selected and disabled states.
private struct MonarchTemplatePickerCardStyle: ButtonStyle {
    let isSelected: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchTemplatePickerCardSurface(configuration: configuration, isSelected: isSelected)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchTemplatePickerCardSurface: View {
    let configuration: ButtonStyleConfiguration
    let isSelected: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let appearance = MonarchTemplatePickerTokens.appearance(
            isSelected: isSelected,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let shape = RoundedRectangle(cornerRadius: MonarchTemplatePickerTokens.cornerRadius, style: .continuous)
        configuration.label
            .padding(MonarchTemplatePickerTokens.cardPadding)
            .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .topLeading)
            .background(shape.fill(theme.colors[role: appearance.background]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: appearance.border],
                    lineWidth: MonarchTemplatePickerTokens.borderWidth(isSelected: isSelected && isEnabled)
                )
            )
            .contentShape(shape)
            .monarchAnimation(.fast, value: configuration.isPressed)
            .monarchAnimation(.fast, value: isSelected)
    }
}

#if DEBUG
    #Preview("MonarchTemplatePicker") {
        MonarchPreviewMatrix {
            MonarchTemplatePicker(
                "Templates",
                selection: .constant("notes"),
                templates: [
                    MonarchTemplatePickerItem(
                        id: "notes",
                        name: "Meeting notes",
                        description: "Agenda, decisions and actions.",
                        category: "meetings",
                        systemImage: "person.3"
                    ),
                    MonarchTemplatePickerItem(
                        id: "brief",
                        name: "Project brief",
                        description: "Goals, scope and owners.",
                        category: "planning",
                        badge: "New",
                        systemImage: "doc.richtext"
                    ),
                ],
                categories: [
                    MonarchTemplatePickerCategory(id: "meetings", label: "Meetings"),
                    MonarchTemplatePickerCategory(id: "planning", label: "Planning"),
                ],
                blank: .blank()
            )
        }
    }
#endif
