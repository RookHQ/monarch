import MonarchTokens
import SwiftUI

/// Picks a saved prompt and fills in its blanks: a searchable list of
/// templates, then a page with the template, one field per `{{blank}}` and
/// a live preview of the finished prompt.
///
/// ```swift
/// .sheet(isPresented: $showsTemplates) {
///     MonarchAiPromptTemplatePicker(
///         templates,
///         onCancel: { showsTemplates = false }
///     ) { text, _, _ in
///         draft = text
///         showsTemplates = false
///     }
/// }
/// ```
///
/// It brings its own `NavigationStack`, so present it in a `sheet`. The list
/// is a `List` with the system search field and, when `categories` are
/// given, a category picker. Choosing a template opens its page; "Use
/// template" hands the filled text, the template and the values to
/// `onUse`, usually to put the text in the prompt field for the person to
/// review and send. A required blank left empty shows an error under its
/// field instead.
///
/// Accessibility: each template is a button read as its title and
/// description. Every field has its label, "Optional" when it may be left
/// empty, and its error or help as the hint; a missing value is announced.
/// Headings on the template page are headers. Buttons have 44-point touch
/// targets and the text wraps at every size.
public struct MonarchAiPromptTemplatePicker: View {
    private let templates: [MonarchAiPromptTemplate]
    private let categories: [MonarchAiPromptTemplateCategory]
    private let initialTemplateID: String?
    private let labels: MonarchAiPromptTemplatePickerLabels
    private let onCancel: (@MainActor () -> Void)?
    private let onUse:
        @MainActor (_ text: String, _ template: MonarchAiPromptTemplate, _ values: [String: String]) -> Void

    @Environment(\.monarchTheme) private var theme
    @State private var query = ""
    @State private var category: String?
    @State private var selected: MonarchAiPromptTemplate?
    /// The row that is marked while the person decides; Continue opens it.
    @State private var chosen: MonarchAiPromptTemplate?

    /// A prompt template picker.
    ///
    /// - Parameters:
    ///   - templates: The templates.
    ///   - categories: Category filters. No filter when empty.
    ///   - initialTemplateID: A template whose page is open at first.
    ///   - labels: Replaces the built-in strings.
    ///   - onCancel: Adds a Cancel button. Close the sheet here.
    ///   - onUse: Receives the filled prompt, the template and the trimmed values.
    public init(
        _ templates: [MonarchAiPromptTemplate],
        categories: [MonarchAiPromptTemplateCategory] = [],
        initialTemplateID: String? = nil,
        labels: MonarchAiPromptTemplatePickerLabels = MonarchAiPromptTemplatePickerLabels(),
        onCancel: (@MainActor () -> Void)? = nil,
        onUse:
            @escaping @MainActor (_ text: String, _ template: MonarchAiPromptTemplate, _ values: [String: String]) ->
            Void
    ) {
        self.templates = templates
        self.categories = categories
        self.initialTemplateID = initialTemplateID
        self.labels = labels
        self.onCancel = onCancel
        self.onUse = onUse
        _selected = State(initialValue: templates.first { $0.id == initialTemplateID })
    }

    private typealias Tokens = MonarchAiPromptTemplatePickerTokens

    /// The content of the view.
    public var body: some View {
        NavigationStack {
            list
                .safeAreaInset(edge: .bottom, spacing: 0) { footer }
                .navigationTitle(labels.title)
                .monarchTemplatePickerInlineTitle()
                .monarchSearchable(text: $query, prompt: Text(labels.search))
                .navigationDestination(item: $selected) { template in
                    MonarchAiPromptTemplateForm(template: template, labels: labels, onUse: onUse)
                }
                .toolbar {
                    if let onCancel {
                        ToolbarItem(placement: .cancellationAction) {
                            Button(labels.cancel, action: onCancel)
                        }
                    }
                }
        }
    }

    private var list: some View {
        let shown = Tokens.filtered(templates, query: query, category: category)
        return List {
            Group {
                if !categories.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            categoryChip(labels.allCategories, isOn: category == nil) { category = nil }
                            ForEach(categories) { item in
                                categoryChip(item.label, isOn: category == item.id) { category = item.id }
                            }
                        }
                    }
                    .accessibilityLabel(labels.category)
                    .listRowSeparator(.hidden)
                }
                if shown.isEmpty {
                    emptyState.listRowBackground(Color.clear)
                }
                ForEach(shown) { template in
                    Button {
                        chosen = template
                    } label: {
                        row(template)
                    }
                    .buttonStyle(.plain)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(Tokens.rowLabel(template))
                    .accessibilityAddTraits(chosen?.id == template.id ? [.isButton, .isSelected] : .isButton)
                    .listRowBackground(Color.clear)
                    .listRowSeparator(.hidden)
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
    }

    /// The hint and the Continue button, as the web shows them under the list.
    private var footer: some View {
        HStack(spacing: Tokens.spacing) {
            Text(chosen == nil ? labels.pickFirst : "")
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textMuted)
                .frame(maxWidth: .infinity, alignment: .leading)
            MonarchButton(labels.next, variant: .primary, size: .sm) {
                selected = chosen
            }
            .disabled(chosen == nil)
        }
        .padding(.horizontal, MonarchTokens.Space.space4)
        .padding(.vertical, MonarchTokens.Space.space3)
        .background(theme.colors.bgCanvas)
    }

    /// A category filter, drawn as a chip as on the web; the chosen one is tinted.
    private func categoryChip(_ title: String, isOn: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .monarchTextStyle(.label)
                .foregroundStyle(isOn ? theme.colors.accentStrong : theme.colors.textDefault)
                .padding(.horizontal, 12)
                .frame(minHeight: 32)
                .background(isOn ? theme.colors.accentSubtle : Color.clear, in: Capsule())
                .overlay(Capsule().strokeBorder(isOn ? theme.colors.accentBorder : theme.colors.borderDefault))
                .monarchMinimumHitTarget()
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }

    @ViewBuilder private var emptyState: some View {
        if templates.isEmpty {
            MonarchEmptyState(labels.empty, kind: .firstRun, size: .sm)
        } else {
            MonarchEmptyState(
                labels.noResults(query.trimmingCharacters(in: .whitespacesAndNewlines)),
                kind: .filtered,
                size: .sm
            ) {
                query = ""
                category = nil
            }
        }
    }

    /// One template as a bordered card, as the web draws it: the name, then what it does.
    private func row(_ template: MonarchAiPromptTemplate) -> some View {
        let isChosen = chosen?.id == template.id
        let shape = RoundedRectangle(cornerRadius: MonarchTokens.Radius.lg, style: .continuous)
        return VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
            Text(template.title)
                .monarchTextStyle(.body, weight: .medium)
                .foregroundStyle(theme.colors.textDefault)
            if let description = template.description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Tokens.spacing + 4)
        .background(shape.fill(isChosen ? theme.colors.aiSubtle : theme.colors.bgSurface))
        .overlay(
            shape.strokeBorder(
                isChosen ? theme.colors.aiBorder : theme.colors.borderSubtle,
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .fixedSize(horizontal: false, vertical: true)
        .monarchMinimumHitTarget()
        .contentShape(Rectangle())
    }
}

extension View {
    /// An inline navigation title on iOS; other platforms have one style.
    @ViewBuilder
    fileprivate func monarchTemplatePickerInlineTitle() -> some View {
        #if os(iOS)
            navigationBarTitleDisplayMode(.inline)
        #else
            self
        #endif
    }
}

#if DEBUG
    #Preview("MonarchAiPromptTemplatePicker") {
        MonarchPreviewMatrix {
            MonarchAiPromptTemplatePicker(
                [
                    MonarchAiPromptTemplate(
                        id: "reply",
                        title: "Reply to a customer",
                        body: "Write a friendly reply to {{customer_name}} about {{topic}}.",
                        description: "A short, warm reply.",
                        category: "support"
                    ),
                    MonarchAiPromptTemplate(id: "summary", title: "Summarise this page", body: "Summarise this page."),
                ],
                categories: [MonarchAiPromptTemplateCategory(id: "support", label: "Support")],
                onCancel: {}
            ) { _, _, _ in }
            .frame(height: MonarchAiPromptTemplatePickerTokens.previewHeight)
        }
    }
#endif
