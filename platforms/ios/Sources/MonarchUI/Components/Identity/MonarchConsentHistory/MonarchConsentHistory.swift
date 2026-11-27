import SwiftUI

/// A person's recorded consent decisions, newest first: when, how, which
/// policy version and rules applied, and every category allowed or not.
///
/// For a privacy screen or an admin view. Monarch does not store consent:
/// the app passes the records it keeps.
///
/// ```swift
/// MonarchConsentHistory(
///     records: records,
///     categories: [MonarchConsentCategory(id: "analytics", label: "Analytics")],
///     showsRelativeTime: true,
///     exportTitle: "Download as CSV"
/// ) {
///     try await exportHistory()
/// }
/// ```
///
/// With `onExport`, a download button appears in the header while there are
/// records; it is busy while the export runs, and a failure shows a message.
///
/// Accessibility: the title is a header and each decision's date is a
/// header, so the headings rotor moves between decisions. Each category is
/// read as its name and "Allowed" or "Not allowed"; the icon is not the only
/// signal. The export failure is announced.
public struct MonarchConsentHistory: View {
    private let title: String?
    private let records: [MonarchConsentRecord]
    private let categories: [MonarchConsentCategory]
    private let description: String?
    private let dateFormat: Date.FormatStyle
    private let showsRelativeTime: Bool
    private let exportTitle: String?
    private let labels: MonarchConsentHistoryLabels
    private let onExport: (@MainActor () async throws -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @State private var isExporting = false
    @State private var exportFailed = false

    /// A consent history.
    ///
    /// - Parameters:
    ///   - title: The heading. Built in: "Consent history".
    ///   - records: The recorded decisions, in any order. Shown newest first.
    ///   - categories: The product's categories, for their names and order. Other ids are shown as they are, after these.
    ///   - description: A sentence under the heading.
    ///   - dateFormat: The format of each decision's date. A medium date and a short time by default.
    ///   - showsRelativeTime: Also shows how long ago each decision was.
    ///   - exportTitle: The title of the download button; name the format. Built in: "Download history".
    ///   - labels: Replacements for the built-in strings.
    ///   - onExport: Builds and shares the file. No button without it. Throw to show the failure.
    public init(
        _ title: String? = nil,
        records: [MonarchConsentRecord],
        categories: [MonarchConsentCategory] = [],
        description: String? = nil,
        dateFormat: Date.FormatStyle = Date.FormatStyle(date: .abbreviated, time: .shortened),
        showsRelativeTime: Bool = false,
        exportTitle: String? = nil,
        labels: MonarchConsentHistoryLabels = MonarchConsentHistoryLabels(),
        onExport: (@MainActor () async throws -> Void)? = nil
    ) {
        self.title = title
        self.records = records
        self.categories = categories
        self.description = description
        self.dateFormat = dateFormat
        self.showsRelativeTime = showsRelativeTime
        self.exportTitle = exportTitle
        self.labels = labels
        self.onExport = onExport
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: MonarchConsentHistoryTokens.sectionSpacing) {
            header
            if exportFailed {
                HStack(alignment: .firstTextBaseline, spacing: MonarchConsentHistoryTokens.iconSpacing) {
                    MonarchIcon(systemName: MonarchConsentHistoryTokens.errorSymbol, size: .sm)
                    Text(labels.exportFailed ?? MonarchConsentHistoryStrings.exportFailed)
                        .monarchTextStyle(.body)
                }
                .foregroundStyle(theme.colors.dangerStrong)
                .accessibilityElement(children: .combine)
            }
            if records.isEmpty {
                MonarchEmptyState(
                    labels.emptyTitle ?? MonarchConsentHistoryStrings.emptyTitle,
                    message: labels.emptyMessage ?? MonarchConsentHistoryStrings.emptyMessage,
                    systemImage: MonarchConsentHistoryTokens.emptySymbol,
                    size: .sm
                )
            } else {
                VStack(alignment: .leading, spacing: MonarchConsentHistoryTokens.recordSpacing) {
                    ForEach(MonarchConsentHistoryTokens.sorted(records)) { record in
                        recordView(record)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: exportFailed) { _, failed in
            if failed {
                AccessibilityNotification.Announcement(labels.exportFailed ?? MonarchConsentHistoryStrings.exportFailed)
                    .post()
            }
        }
    }

    private var header: some View {
        ViewThatFits(in: .horizontal) {
            HStack(alignment: .firstTextBaseline, spacing: MonarchConsentHistoryTokens.lineSpacing) {
                heading
                Spacer(minLength: 0)
                exportButton
            }
            VStack(alignment: .leading, spacing: MonarchConsentHistoryTokens.lineSpacing) {
                heading
                exportButton
            }
        }
    }

    private var heading: some View {
        VStack(alignment: .leading, spacing: MonarchConsentHistoryTokens.valueSpacing) {
            Text(title ?? MonarchConsentHistoryStrings.title)
                .monarchTextStyle(.heading)
                .foregroundStyle(theme.colors.textDefault)
                .accessibilityAddTraits(.isHeader)
            if let description, !description.isEmpty {
                Text(description)
                    .monarchTextStyle(.body)
                    .foregroundStyle(theme.colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    @ViewBuilder private var exportButton: some View {
        if let onExport, !records.isEmpty {
            MonarchButton(
                exportTitle ?? MonarchConsentHistoryStrings.export,
                systemImage: MonarchConsentHistoryTokens.exportSymbol,
                variant: .secondary,
                size: .sm,
                isLoading: isExporting
            ) {
                guard !isExporting else { return }
                exportFailed = false
                isExporting = true
                Task { @MainActor in
                    do {
                        try await onExport()
                    } catch {
                        exportFailed = true
                    }
                    isExporting = false
                }
            }
        }
    }

    /// The default format is a medium date, a comma and a two-digit 24-hour time ("18 Apr 2026, 08:42"), as the web;
    /// a format the app passes is used as given.
    private func dateText(_ date: Date) -> String {
        guard dateFormat == Date.FormatStyle(date: .abbreviated, time: .shortened) else {
            return date.formatted(dateFormat)
        }
        let day = date.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted).locale(locale))
        let time = date.formatted(
            Date.FormatStyle().hour(.twoDigits(amPM: .omitted)).minute(.twoDigits).locale(locale)
        )
        return day + ", " + time
    }

    /// One record as a list of label and value rows divided by a rule, with no card, as the web draws it.
    private func recordView(_ record: MonarchConsentRecord) -> some View {
        VStack(alignment: .leading, spacing: MonarchConsentHistoryTokens.lineSpacing) {
            line(
                MonarchConsentHistoryStrings.dateTitle,
                value: dateText(record.date)
            )
            if showsRelativeTime {
                MonarchRelativeTimeClock(dates: [record.date]) { now in
                    Text(MonarchRelativeTime.string(record.date, now: now, locale: locale))
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors.textMuted)
                        .padding(.leading, MonarchConsentHistoryTokens.labelWidth)
                }
            }
            line(
                labels.methodTitle ?? MonarchConsentHistoryStrings.methodTitle,
                value: MonarchConsentHistoryTokens.methodText(record.method, labels: labels),
                note: MonarchConsentHistoryTokens.signalNote(record, labels: labels),
                isStrong: true
            )
            line(
                labels.policyVersionTitle ?? MonarchConsentHistoryStrings.policyVersionTitle,
                value: record.policyVersion,
                isMono: true
            )
            line(
                labels.regimeTitle ?? MonarchConsentHistoryStrings.regimeTitle,
                value: MonarchConsentHistoryTokens.regimeText(record.regime, labels: labels)
            )
            categoryList(record)
        }
        .padding(.vertical, MonarchConsentHistoryTokens.recordPadding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .overlay(alignment: .bottom) { MonarchDivider() }
        .accessibilityElement(children: .contain)
    }

    private func line(
        _ name: String,
        value: String,
        note: String? = nil,
        isStrong: Bool = false,
        isMono: Bool = false
    ) -> some View {
        VStack(alignment: .leading, spacing: MonarchConsentHistoryTokens.valueSpacing) {
            HStack(alignment: .firstTextBaseline, spacing: 0) {
                Text(name)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .frame(width: MonarchConsentHistoryTokens.labelWidth, alignment: .leading)
                Text(value)
                    .monarchTextStyle(isStrong ? .bodyStrong : .body)
                    .monospaced(isMono)
                    .foregroundStyle(theme.colors.textDefault)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            if let note {
                Text(note)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .padding(.leading, MonarchConsentHistoryTokens.labelWidth)
            }
        }
        .fixedSize(horizontal: false, vertical: true)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder private func categoryList(_ record: MonarchConsentRecord) -> some View {
        let rows = MonarchConsentHistoryTokens.categoryRows(record, categories: categories)
        if !rows.isEmpty {
            HStack(alignment: .firstTextBaseline, spacing: 0) {
                Text(labels.categoriesTitle ?? MonarchConsentHistoryStrings.categoriesTitle)
                    .monarchTextStyle(.caption)
                    .foregroundStyle(theme.colors.textMuted)
                    .frame(width: MonarchConsentHistoryTokens.labelWidth, alignment: .leading)
                VStack(alignment: .leading, spacing: MonarchConsentHistoryTokens.valueSpacing) {
                    ForEach(rows) { row in
                        HStack(alignment: .firstTextBaseline, spacing: MonarchConsentHistoryTokens.iconSpacing) {
                            MonarchIcon(
                                systemName: row.isAllowed
                                    ? MonarchConsentHistoryTokens.allowedSymbol
                                    : MonarchConsentHistoryTokens.deniedSymbol,
                                size: .sm
                            )
                            .foregroundStyle(
                                theme.colors[role: MonarchConsentHistoryTokens.decisionColor(isAllowed: row.isAllowed)]
                            )
                            (Text(row.label).foregroundStyle(theme.colors.textDefault)
                                + Text(
                                    " · "
                                        + MonarchConsentHistoryTokens.decisionText(
                                            isAllowed: row.isAllowed,
                                            labels: labels
                                        )
                                ).foregroundStyle(theme.colors.textMuted))
                                .monarchTextStyle(.body)
                        }
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel(MonarchConsentHistoryTokens.categoryLabel(row, labels: labels))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

#if DEBUG
    #Preview("MonarchConsentHistory") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchConsentHistory(
                    records: [
                        MonarchConsentRecord(
                            id: "1",
                            date: .now.addingTimeInterval(-3_600),
                            method: .custom,
                            policyVersion: "2026-03",
                            regime: .optIn,
                            categories: ["necessary": true, "analytics": true, "marketing": false],
                            isGlobalPrivacyControl: true
                        )
                    ],
                    categories: [
                        MonarchConsentCategory(id: "necessary", label: "Necessary"),
                        MonarchConsentCategory(id: "analytics", label: "Analytics"),
                    ],
                    showsRelativeTime: true
                ) {}
                MonarchConsentHistory(records: [])
            }
        }
    }
#endif
