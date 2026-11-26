import MonarchTokens
import SwiftUI

/// The DNS records a user must add at their DNS provider to connect a custom
/// domain or verify a sender: type, name and value with copy buttons, what
/// each record is for, and whether public DNS already shows it.
///
/// ```swift
/// MonarchDnsRecordTable(
///     [
///         MonarchDnsRecord(type: "CNAME", name: "book.example.com", value: "domains.example.net", purpose: .routing)
///     ],
///     zone: "example.com"
/// )
/// ```
///
/// With a zone, names are shown the way most providers want them typed
/// (`book`, `@` for the apex) with the full name under the card. Each name
/// and value is a ``MonarchCopyField``. A record that was checked shows a
/// ``MonarchStatusBadge``: "Found" or "Waiting", a word as well as a tone.
/// By default the records are cards in a compact width or at accessibility
/// text sizes and a table in a regular width.
///
/// Accessibility: the records are a container named "DNS records for
/// example.com". The heading of each card is a header. Each name and value
/// is read as "name of the CNAME record, book" and its copy button as "Copy
/// name of the CNAME record". When the number of found records changes the
/// summary ("1 of 2 records found.") is announced. Copy buttons keep a
/// 44-point target.
public struct MonarchDnsRecordTable: View {
    private let records: [MonarchDnsRecord]
    private let zone: String?
    private let layout: MonarchDnsRecordTableLayout
    private let labels: MonarchDnsRecordTableLabels
    private let onCopy: (@MainActor (String) -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    /// A DNS record table.
    ///
    /// - Parameters:
    ///   - records: The records, in the order the user should create them.
    ///   - zone: The DNS zone the user manages, such as `example.com`. Names
    ///     are then shown relative to it, with the full name below.
    ///   - layout: Cards, a table, or automatic (cards in a compact width).
    ///   - labels: Replacements for the built-in strings.
    ///   - onCopy: Runs with the copied text after a name or value is copied.
    public init(
        _ records: [MonarchDnsRecord],
        zone: String? = nil,
        layout: MonarchDnsRecordTableLayout = .automatic,
        labels: MonarchDnsRecordTableLabels = MonarchDnsRecordTableLabels(),
        onCopy: (@MainActor (String) -> Void)? = nil
    ) {
        self.records = records
        self.zone = zone
        self.layout = layout
        self.labels = labels
        self.onCopy = onCopy
    }

    private var form: MonarchDnsRecordTableForm {
        MonarchDnsRecordTableTokens.form(
            layout,
            isRegularWidth: horizontalSizeClass != .compact,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
    }

    private var foundCount: Int {
        MonarchDnsRecordTableTokens.counts(records).found
    }

    /// The content of the view.
    public var body: some View {
        Group {
            switch form {
            case .cards: cards
            case .table: table
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(labels.labelText(zone: zone))
        .onChange(of: foundCount) { _, found in
            let checked = MonarchDnsRecordTableTokens.counts(records).checked
            AccessibilityNotification.Announcement(labels.summaryText(found: found, total: checked)).post()
        }
    }

    // MARK: Cards

    private var cards: some View {
        VStack(alignment: .leading, spacing: MonarchDnsRecordTableTokens.cardSpacing) {
            ForEach(records) { record in
                card(record)
            }
        }
    }

    private func card(_ record: MonarchDnsRecord) -> some View {
        let shown = MonarchDnsRecordTableTokens.relativeName(record.name, zone: zone)
        let shape = MonarchShapes.container
        return VStack(alignment: .leading, spacing: MonarchDnsRecordTableTokens.cardContentSpacing) {
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: MonarchDnsRecordTableTokens.headSpacing) {
                    heading(record)
                    Spacer(minLength: 0)
                    statusBadge(record)
                }
                VStack(alignment: .leading, spacing: MonarchDnsRecordTableTokens.headSpacing) {
                    heading(record)
                    statusBadge(record)
                }
            }
            // Type, Name and Value side by side, as on the web; stacked when the text size leaves no room.
            if dynamicTypeSize.isAccessibilitySize {
                VStack(alignment: .leading, spacing: MonarchDnsRecordTableTokens.cardContentSpacing) {
                    term(labels.resolvedType) { plainValue(record.type) }
                    term(labels.resolvedName) { nameField(record, shown: shown) }
                    term(labels.resolvedValue) { valueField(record, shown: shown) }
                }
            } else {
                // A phone has no room for three fields and two copy buttons in a row: Type and Name share the
                // first row, Value takes the one below.
                VStack(alignment: .leading, spacing: MonarchDnsRecordTableTokens.cardContentSpacing) {
                    HStack(alignment: .top, spacing: MonarchTokens.Space.space2) {
                        term(labels.resolvedType) { plainValue(record.type) }
                            .fixedSize()
                        term(labels.resolvedName) { nameField(record, shown: shown) }
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    term(labels.resolvedValue) { valueField(record, shown: shown) }
                }
            }
            if record.priority != nil || record.ttl != nil {
                HStack(alignment: .top, spacing: MonarchDnsRecordTableTokens.columnSpacing) {
                    if let priority = record.priority {
                        term(labels.resolvedPriority) { plainValue(String(priority)) }
                    }
                    if let ttl = record.ttl {
                        term(labels.resolvedTTL) { plainValue(ttl) }
                    }
                }
            }
            if shown != record.name {
                Text(labels.fullNameText(record.name))
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors[role: MonarchDnsRecordTableTokens.mutedRole])
                    .multilineTextAlignment(.leading)
            }
        }
        .padding(MonarchDnsRecordTableTokens.cardPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchDnsRecordTableTokens.surfaceRole]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchDnsRecordTableTokens.borderRole],
                lineWidth: MonarchShapes.borderWidth
            )
        )
        .accessibilityElement(children: .contain)
    }

    private func nameField(_ record: MonarchDnsRecord, shown: String) -> some View {
        MonarchCopyField(
            labels.copyNameText(record, shown: shown),
            value: shown,
            isIconOnly: true,
            isLabelHidden: true,
            onCopy: onCopy
        )
    }

    private func valueField(_ record: MonarchDnsRecord, shown: String) -> some View {
        MonarchCopyField(
            labels.copyValueText(record, shown: shown),
            value: record.value,
            isMultiline: false,
            isIconOnly: true,
            isLabelHidden: true,
            onCopy: onCopy
        )
    }

    private func heading(_ record: MonarchDnsRecord) -> some View {
        Text(labels.purposeText(record.purpose).uppercased())
            .monarchTextStyle(.caption, weight: .semibold)
            .tracking(0.4)
            .foregroundStyle(theme.colors[role: MonarchDnsRecordTableTokens.textRole])
            .multilineTextAlignment(.leading)
            .fixedSize(horizontal: false, vertical: true)
            .accessibilityAddTraits(.isHeader)
    }

    @ViewBuilder private func statusBadge(_ record: MonarchDnsRecord, size: MonarchStatusBadgeSize = .sm) -> some View {
        if let status = MonarchDnsRecordTableTokens.status(record, labels: labels) {
            MonarchStatusBadge(status, size: size)
        }
    }

    /// A term above its value. The term is read with plain values and
    /// hidden for copy fields, which carry their own spoken name.
    private func term(_ title: String, @ViewBuilder content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: MonarchDnsRecordTableTokens.termSpacing) {
            Text(title)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchDnsRecordTableTokens.mutedRole])
            content()
        }
    }

    private func plainValue(_ text: String) -> some View {
        Text(text)
            .frame(minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
            .monarchTextStyle(.code)
            .foregroundStyle(theme.colors[role: MonarchDnsRecordTableTokens.textRole])
            .textSelection(.enabled)
    }

    // MARK: Table

    private var table: some View {
        let hasPriority = MonarchDnsRecordTableTokens.hasPriority(records)
        let hasTTL = MonarchDnsRecordTableTokens.hasTTL(records)
        let hasStatus = MonarchDnsRecordTableTokens.counts(records).checked > 0
        let shape = MonarchShapes.container
        return Grid(
            alignment: .leadingFirstTextBaseline,
            horizontalSpacing: MonarchDnsRecordTableTokens.columnSpacing,
            verticalSpacing: MonarchDnsRecordTableTokens.rowPadding
        ) {
            GridRow {
                header(labels.resolvedType)
                header(labels.resolvedName)
                header(labels.resolvedValue)
                if hasPriority { header(labels.resolvedPriority) }
                if hasTTL { header(labels.resolvedTTL) }
                if hasStatus { header(labels.resolvedStatus) }
            }
            ForEach(records) { record in
                let shown = MonarchDnsRecordTableTokens.relativeName(record.name, zone: zone)
                rule
                GridRow(alignment: .center) {
                    plainValue(record.type)
                    MonarchCopyField(
                        labels.copyNameText(record, shown: shown),
                        value: shown,
                        size: .sm,
                        isIconOnly: true,
                        isLabelHidden: true,
                        onCopy: onCopy
                    )
                    MonarchCopyField(
                        labels.copyValueText(record, shown: shown),
                        value: record.value,
                        size: .sm,
                        isIconOnly: true,
                        isLabelHidden: true,
                        onCopy: onCopy
                    )
                    if hasPriority { plainValue(record.priority.map(String.init) ?? "") }
                    if hasTTL { plainValue(record.ttl ?? "") }
                    if hasStatus { statusBadge(record) }
                }
                .accessibilityElement(children: .contain)
                .accessibilityLabel(MonarchDnsRecordTableTokens.rowLabel(record, shown: shown))
            }
        }
        .padding(MonarchDnsRecordTableTokens.tablePadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(shape.fill(theme.colors[role: MonarchDnsRecordTableTokens.surfaceRole]))
        .overlay(
            shape.strokeBorder(
                theme.colors[role: MonarchDnsRecordTableTokens.borderRole],
                lineWidth: MonarchShapes.borderWidth
            )
        )
    }

    private func header(_ title: String) -> some View {
        Text(title)
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchDnsRecordTableTokens.mutedRole])
            .accessibilityAddTraits(.isHeader)
    }

    private var rule: some View {
        Rectangle()
            .fill(theme.colors[role: MonarchDnsRecordTableTokens.borderRole])
            .frame(height: MonarchShapes.borderWidth)
            .gridCellUnsizedAxes(.horizontal)
            .accessibilityHidden(true)
    }
}

#if DEBUG
    #Preview("MonarchDnsRecordTable") {
        let records = [
            MonarchDnsRecord(
                type: "CNAME",
                name: "book.example.com",
                value: "domains.example.net",
                purpose: .routing,
                isFound: true
            ),
            MonarchDnsRecord(
                type: "TXT",
                name: "_verify.book.example.com",
                value: "verify=4f7k2m9q8d3r6t1w",
                purpose: .verification,
                ttl: "3600",
                isFound: false
            ),
            MonarchDnsRecord(type: "MX", name: "example.com", value: "mx.example.net", priority: 10),
        ]
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchDnsRecordTable(records, zone: "example.com", layout: .cards)
                MonarchDnsRecordTable(records, zone: "example.com", layout: .table)
            }
        }
    }
#endif
