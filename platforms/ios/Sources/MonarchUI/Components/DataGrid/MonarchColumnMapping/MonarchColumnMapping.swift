import MonarchTokens
import SwiftUI

/// Maps the columns of imported data (a CSV header, a spreadsheet) to the
/// product's fields: one picker per column with example values,
/// suggestions from names and aliases, and the required fields still
/// unmapped and the fields chosen twice.
///
/// ```swift
/// @State private var mapping = MonarchColumnMapping.mapping(
///     from: MonarchColumnMapping.suggest(sources, fields: fields)
/// )
///
/// MonarchColumnMapping(sources, mapping: .constant(["E-mail": "email", "Mail": "email"]), fields: fields)
/// ```
///
/// The view is a `List`, the mapping step of an import on its own screen:
/// a first section with the summary ("8 of 10 columns mapped"), the
/// missing required fields and the "Use suggestions" and "Don’t import any"
/// buttons, then one row per column. Each row is a standard menu `Picker`
/// whose label shows the column's header and up to three example values;
/// "Don’t import" leaves the column out. A column whose field is the
/// suggestion shows a "Matched" or "Suggested" badge. The mapping is
/// column name → field id; a column missing from it is not imported. Use
/// ``issues(_:fields:)`` to decide whether the import can go on. Disable it
/// with `disabled(_:)`.
///
/// Accessibility: each picker is named "Field for <column>" and reads the
/// chosen field as its value; the example values, the badge, a duplicate
/// error and the field's help follow as its hint. Errors have an icon and
/// text, never colour alone. The summary changes are announced. Rows grow
/// with Dynamic Type.
public struct MonarchColumnMapping: View {
    private let sources: [MonarchColumnMappingSource]
    @Binding private var mapping: [String: String]
    private let fields: [MonarchColumnMappingField]
    private let suggestions: [String: MonarchColumnMappingSuggestion]?
    private let showsIssues: Bool
    private let showsBulkActions: Bool
    private let labels: MonarchColumnMappingLabels

    @Environment(\.monarchTheme) private var theme

    /// A column mapping.
    ///
    /// - Parameters:
    ///   - sources: The columns of the imported data, in file order.
    ///   - mapping: Column name → field id. A column missing from it is not imported.
    ///   - fields: The fields the columns can be mapped to.
    ///   - suggestions: The suggestions to show. By default, ``suggest(_:fields:threshold:)``.
    ///   - showsIssues: Shows the missing required fields and the duplicates. Pass `false`
    ///     until the person first tries to go on, if errors should not show straight away.
    ///   - showsBulkActions: Shows "Use suggestions" and "Don’t import any".
    ///   - labels: Replacements for the built-in strings.
    public init(
        _ sources: [MonarchColumnMappingSource],
        mapping: Binding<[String: String]>,
        fields: [MonarchColumnMappingField],
        suggestions: [String: MonarchColumnMappingSuggestion]? = nil,
        showsIssues: Bool = true,
        showsBulkActions: Bool = true,
        labels: MonarchColumnMappingLabels = MonarchColumnMappingLabels()
    ) {
        self.sources = sources
        _mapping = mapping
        self.fields = fields
        self.suggestions = suggestions
        self.showsIssues = showsIssues
        self.showsBulkActions = showsBulkActions
        self.labels = labels
    }

    /// Suggests a field for each column. Exact matches (the header equals a
    /// field's id, label or alias, ignoring case and punctuation) come
    /// first, then close matches scoring at least `threshold`. Each field is
    /// suggested for one column at most.
    ///
    /// - Parameters:
    ///   - sources: The columns.
    ///   - fields: The fields.
    ///   - threshold: The lowest similarity of a close match, from 0 to 1.
    /// - Returns: Column name → suggestion, for the columns that have one.
    public static func suggest(
        _ sources: [MonarchColumnMappingSource],
        fields: [MonarchColumnMappingField],
        threshold: Double = 0.75
    ) -> [String: MonarchColumnMappingSuggestion] {
        MonarchColumnMappingTokens.suggest(sources, fields: fields, threshold: threshold)
    }

    /// The suggestions as a mapping, to start from.
    ///
    /// - Parameter suggestions: The suggestions.
    /// - Returns: Column name → field id.
    public static func mapping(from suggestions: [String: MonarchColumnMappingSuggestion]) -> [String: String] {
        MonarchColumnMappingTokens.mapping(from: suggestions)
    }

    /// What still blocks a mapping: required fields that no column maps to,
    /// and fields chosen for more than one column.
    ///
    /// - Parameters:
    ///   - mapping: Column name → field id.
    ///   - fields: The fields.
    /// - Returns: The missing fields, and field id → the columns that chose it, for fields chosen twice or more.
    public static func issues(
        _ mapping: [String: String],
        fields: [MonarchColumnMappingField]
    ) -> (missing: [MonarchColumnMappingField], duplicates: [String: [String]]) {
        MonarchColumnMappingTokens.issues(mapping, fields: fields, sourceOrder: [])
    }

    private typealias Tokens = MonarchColumnMappingTokens

    /// The content of the view.
    public var body: some View {
        let computed = suggestions ?? Tokens.suggest(sources, fields: fields, threshold: Tokens.defaultThreshold)
        let issues = Tokens.issues(mapping, fields: fields, sourceOrder: sources.map(\.name))
        let summary = Tokens.summary(
            mapped: Tokens.mappedCount(mapping, sources: sources, fields: fields),
            total: sources.count,
            labels: labels
        )
        List {
            Group {
                Section {
                    Text(summary)
                        .monarchTextStyle(.label)
                        .foregroundStyle(theme.colors[role: Tokens.nameColor])
                    if showsIssues, let missing = Tokens.missingText(issues.missing, labels: labels) {
                        error(missing)
                    }
                    if showsBulkActions {
                        bulkActions(computed)
                    }
                }
                Section {
                    headerRow
                    ForEach(Array(sources.enumerated()), id: \.offset) { index, source in
                        row(source, index: index, suggestion: computed[source.name], duplicates: issues.duplicates)
                    }
                }

            }
            .monarchRowsOnCanvas()
        }
        .listStyle(.plain)
        .monarchCanvas()
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.label ?? MonarchColumnMappingStrings.label)
        .onChange(of: summary) { _, new in
            AccessibilityNotification.Announcement(new).post()
        }
    }

    @ViewBuilder private func bulkActions(_ computed: [String: MonarchColumnMappingSuggestion]) -> some View {
        let offersSuggestions = Tokens.offersSuggestions(mapping, suggestions: computed, sources: sources)
        let offersSkipAll = Tokens.offersSkipAll(mapping, sources: sources)
        if offersSuggestions || offersSkipAll {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: Tokens.actionSpacing) { bulkButtons(computed, offersSuggestions, offersSkipAll) }
                VStack(alignment: .leading, spacing: Tokens.actionSpacing) {
                    bulkButtons(computed, offersSuggestions, offersSkipAll)
                }
            }
        }
    }

    @ViewBuilder private func bulkButtons(
        _ computed: [String: MonarchColumnMappingSuggestion],
        _ offersSuggestions: Bool,
        _ offersSkipAll: Bool
    ) -> some View {
        if offersSuggestions {
            MonarchButton(
                labels.useSuggestions ?? MonarchColumnMappingStrings.useSuggestions,
                variant: .ghost,
                size: .sm
            ) {
                mapping = Tokens.mapping(from: computed)
            }
        }
        if offersSkipAll {
            MonarchButton(labels.skipAll ?? MonarchColumnMappingStrings.skipAll, variant: .ghost, size: .sm) {
                mapping = [:]
            }
        }
    }

    private func row(
        _ source: MonarchColumnMappingSource,
        index: Int,
        suggestion: MonarchColumnMappingSuggestion?,
        duplicates: [String: [String]]
    ) -> some View {
        let name = Tokens.displayName(source, index: index)
        let current = mapping[source.name]
        let field = fields.first { $0.id == current }
        let samples = Tokens.samplesText(source, labels: labels)
        let badge = suggestion.flatMap { $0.fieldID == current ? $0.confidence : nil }
        let badgeText = badge.map { Tokens.badgeText($0, labels: labels) }
        let duplicate =
            showsIssues
            ? Tokens.duplicateText(source: source.name, field: field, duplicates: duplicates, labels: labels) : nil
        let selection = Binding<String?>(
            get: { current },
            set: { mapping[source.name] = $0 }
        )
        let selected =
            field.map { Tokens.optionTitle($0, labels: labels) } ?? (labels.skip ?? MonarchColumnMappingStrings.skip)
        // Name, example values and the target field share a row, as the three columns of the web table.
        return HStack(alignment: .top, spacing: Tokens.actionSpacing) {
            Text(name)
                .monarchTextStyle(.bodyStrong)
                .foregroundStyle(theme.colors[role: Tokens.nameColor])
                // A long word shrinks a little instead of breaking inside the word.
                .lineLimit(2)
                .minimumScaleFactor(0.7)
                .frame(width: Self.nameWidth, alignment: .leading)
            Text(samples)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                .lineLimit(3)
                .minimumScaleFactor(0.7)
                .frame(width: Self.samplesWidth, alignment: .leading)
            VStack(alignment: .leading, spacing: Tokens.rowSpacing) {
                MonarchSelectField(
                    Tokens.pickerLabel(name, labels: labels),
                    selection: selection,
                    valueText: selected,
                    labelVisibility: .hidden,
                    style: .compact
                ) {
                    Text(labels.skip ?? MonarchColumnMappingStrings.skip).tag(String?.none)
                    ForEach(fields) { field in
                        Text(Tokens.optionTitle(field, labels: labels)).tag(Optional(field.id))
                    }
                }
                if let badge, let badgeText {
                    MonarchBadge(badgeText, tone: Tokens.badgeTone(badge))
                }
                if let help = field?.description {
                    Text(help)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: Tokens.mutedColor])
                }
                if let duplicate {
                    error(duplicate)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .contain)
        .accessibilityHint(
            Tokens.pickerHint(samples: samples, badge: badgeText, error: duplicate, help: field?.description)
        )
    }

    private static let nameWidth: CGFloat = 96
    private static let samplesWidth: CGFloat = 88

    /// The three column headings of the table, over the rows.
    private var headerRow: some View {
        HStack(alignment: .top, spacing: Tokens.actionSpacing) {
            heading(MonarchColumnMappingStrings.columnInFile).frame(width: Self.nameWidth, alignment: .leading)
            heading(MonarchColumnMappingStrings.exampleValues).frame(width: Self.samplesWidth, alignment: .leading)
            heading(MonarchColumnMappingStrings.importAs).frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityHidden(true)
    }

    private func heading(_ text: String) -> some View {
        Text(text)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: Tokens.mutedColor])
    }

    private func error(_ message: String) -> some View {
        Label {
            Text(message)
        } icon: {
            MonarchIcon(systemName: Tokens.errorSymbol, size: .sm)
        }
        .monarchTextStyle(.caption)
        .foregroundStyle(theme.colors[role: Tokens.errorColor])
    }
}

#if DEBUG
    #Preview("MonarchColumnMapping") {
        MonarchPreviewMatrix {
            MonarchColumnMapping(
                [
                    MonarchColumnMappingSource("E-mail", samples: ["ada@example.com"]),
                    MonarchColumnMappingSource("Mail"),
                    MonarchColumnMappingSource("Full name", samples: ["Ada Moss", "Bea Lund"]),
                ],
                mapping: .constant(["E-mail": "email", "Mail": "email"]),
                fields: [
                    MonarchColumnMappingField(id: "email", label: "Email", isRequired: true),
                    MonarchColumnMappingField(id: "name", label: "Name", isRequired: true, aliases: ["Full name"]),
                ]
            )
            .frame(height: MonarchTokens.Space.space24 * 4)
        }
    }
#endif
