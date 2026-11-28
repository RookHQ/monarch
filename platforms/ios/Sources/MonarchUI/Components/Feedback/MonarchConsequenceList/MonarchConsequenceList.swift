import SwiftUI

/// What an action deletes, changes and keeps, as a list to read before
/// confirming.
///
/// ```swift
/// MonarchConsequenceList(
///     [
///         MonarchConsequence(.removed, "tasks will be deleted", count: 12),
///         MonarchConsequence(.changed, "Members lose access immediately"),
///         MonarchConsequence(.kept, "Invoices stay in Billing"),
///     ],
///     heading: "What happens"
/// )
/// ```
///
/// Accessibility: each row is one element. An ungrouped row is read as the
/// name of its kind, then the text ("Removed: 12 tasks will be deleted"); a
/// grouped row is read without it, under a heading that says it. Each kind
/// also has its own icon shape, so the kind does not depend on colour. While
/// loading the list is one element that reads "Checking what will change".
public struct MonarchConsequenceList: View {
    private let items: [MonarchConsequence]
    private let heading: String?
    private let isGrouped: Bool
    private let isLoading: Bool
    private let loadingRowCount: Int
    private let loadingLabel: String?
    private let kindLabel: (@Sendable (MonarchConsequenceKind) -> String)?
    private let groupTitle: (@Sendable (MonarchConsequenceKind) -> String)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale

    /// A consequence list.
    ///
    /// - Parameters:
    ///   - items: The consequences, in the order they should be read.
    ///   - heading: A heading above the list, such as "What happens".
    ///   - isGrouped: Whether the items are split by kind under headings:
    ///     deleted, changed, warnings, kept, in that order.
    ///   - isLoading: Whether placeholder rows replace the items while the
    ///     consequences are being worked out.
    ///   - loadingRowCount: The number of placeholder rows while loading.
    ///   - loadingLabel: Replaces the built-in spoken text while loading.
    ///   - kindLabel: Replaces the built-in spoken name of each kind.
    ///   - groupTitle: Replaces the built-in heading of each group.
    public init(
        _ items: [MonarchConsequence],
        heading: String? = nil,
        isGrouped: Bool = false,
        isLoading: Bool = false,
        loadingRowCount: Int = 3,
        loadingLabel: String? = nil,
        kindLabel: (@Sendable (MonarchConsequenceKind) -> String)? = nil,
        groupTitle: (@Sendable (MonarchConsequenceKind) -> String)? = nil
    ) {
        self.items = items
        self.heading = heading
        self.isGrouped = isGrouped
        self.isLoading = isLoading
        self.loadingRowCount = MonarchConsequenceListTokens.loadingRows(loadingRowCount)
        self.loadingLabel = loadingLabel
        self.kindLabel = kindLabel
        self.groupTitle = groupTitle
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchConsequenceListTokens.sectionSpacing) {
            if let heading {
                Text(heading)
                    .monarchTextStyle(.subheading)
                    .foregroundStyle(theme.colors.textDefault)
                    .accessibilityAddTraits(.isHeader)
            }
            if isLoading {
                loadingRows
            } else if isGrouped {
                ForEach(MonarchConsequenceListTokens.groups(items)) { group in
                    VStack(alignment: .leading, spacing: MonarchConsequenceListTokens.rowSpacing) {
                        Text(groupTitle?(group.kind) ?? MonarchConsequenceListStrings.group(group.kind))
                            .monarchTextStyle(.label)
                            .foregroundStyle(theme.colors.textMuted)
                            .accessibilityAddTraits(.isHeader)
                        rows(group.items, spokenKind: false)
                    }
                }
            } else {
                VStack(alignment: .leading, spacing: MonarchConsequenceListTokens.rowSpacing) {
                    rows(items, spokenKind: true)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .contain)
    }

    private func rows(_ items: [MonarchConsequence], spokenKind: Bool) -> some View {
        ForEach(items) { item in
            let kindName = spokenKind ? (kindLabel?(item.kind) ?? MonarchConsequenceListStrings.kind(item.kind)) : nil
            row(item)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(
                    MonarchConsequenceListTokens.accessibilityLabel(item, kindName: kindName, locale: locale)
                )
        }
    }

    private func row(_ item: MonarchConsequence) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: MonarchConsequenceListTokens.iconSpacing) {
            MonarchIcon(systemName: MonarchConsequenceListTokens.symbol(item.kind), size: .sm)
                .foregroundStyle(theme.colors[role: MonarchConsequenceListTokens.iconRole(item.kind)])
            VStack(alignment: .leading, spacing: MonarchConsequenceListTokens.textSpacing) {
                titleText(item)
                    .monarchTextStyle(.body)
                    .monospacedDigit()
                    .foregroundStyle(theme.colors.textDefault)
                if let detail = item.detail {
                    Text(detail)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                }
            }
        }
    }

    /// The first line, with the count in bold and, for a removal, in the danger colour, as the web.
    private func titleText(_ item: MonarchConsequence) -> Text {
        guard let count = item.count else { return Text(item.label) }
        var number = AttributedString(count.formatted(.number.locale(locale)))
        number.inlinePresentationIntent = .stronglyEmphasized
        if item.kind == .removed { number.foregroundColor = theme.colors.dangerStrong }
        return Text((number + AttributedString(" " + item.label)).monarchStrongFaces(theme: theme, style: .body))
    }

    /// Placeholder rows drawn by the system's redaction, read as one element.
    private var loadingRows: some View {
        VStack(alignment: .leading, spacing: MonarchConsequenceListTokens.rowSpacing) {
            ForEach(0..<loadingRowCount, id: \.self) { _ in
                row(MonarchConsequence(.kept, MonarchConsequenceListTokens.placeholder))
            }
        }
        .redacted(reason: .placeholder)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(loadingLabel ?? MonarchConsequenceListStrings.loading)
    }
}

#if DEBUG
    #Preview("MonarchConsequenceList") {
        let items = [
            MonarchConsequence(.removed, "tasks will be deleted", detail: "Including 3 that are overdue", count: 1200),
            MonarchConsequence(.changed, "Members lose access immediately"),
            MonarchConsequence(.warning, "This cannot be undone"),
            MonarchConsequence(.kept, "Invoices stay in Billing"),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchConsequenceList(items, heading: "What happens")
                MonarchConsequenceList(items, isGrouped: true)
                MonarchConsequenceList([], isLoading: true)
            }
        }
    }
#endif
