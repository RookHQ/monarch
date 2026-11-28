import SwiftUI

/// A period of time entry, usually a week: rows of projects or tasks
/// against days, with row, day and period totals, locked days and fast
/// typed entry ("1:30", "1.5h", "90m").
///
/// ```swift
/// MonarchTimesheetGrid(
///     days: week,
///     rows: rows,
///     onCellChange: { rowID, day, minutes in
///         try await store.log(minutes, on: day, for: rowID)
///     }
/// )
/// ```
///
/// At regular width it is a SwiftUI `Grid` with a column per day, scrolling
/// sideways when it does not fit; Return saves a cell and moves down, the up
/// and down arrows move between cells on a hardware keyboard, and Escape
/// puts back the saved value. At compact width and at the accessibility
/// text sizes it shows one day per page, with previous and next buttons and a
/// ``MonarchDurationInput`` (with its stepper) per row. `layout` forces
/// either form.
///
/// The grid shows what `rows` says. A value typed in a cell is read when
/// the person presses Return or leaves the cell and passed to
/// `onCellChange`; the app saves it and passes the new rows. While the save
/// runs the cell is disabled; a thrown error shows its description (or a
/// built-in message) and keeps the typed text. Text that cannot be read, or
/// a value above `maximumMinutesPerDay`, is flagged and never saved. Without
/// `onCellChange`, or with `isReadOnly`, every cell is read-only. Cells of a
/// locked day and of a read-only row never take typing.
///
/// Accessibility: the grid is a container named "Time per row and day".
/// Day headings and row names are headers; a day heading is read as the
/// day, "Today", the lock reason and the note. Each cell is named by its row
/// and day ("Atlas, Mon, Jan 12"); a read-only cell's value is the time in
/// words and the lock reason, and an editable cell's hint is its error. A
/// day total is read with its expected time and "Over the daily limit".
/// Errors are announced. Locked days show a lock symbol, today a bar under
/// its heading and an over-limit total a warning symbol, not colour alone.
/// On a day page the day's title is adjustable: swipe up or down to change
/// day.
@_spi(Experimental) public struct MonarchTimesheetGrid: View {
    private let days: [MonarchTimesheetGridDay]
    private let rows: [MonarchTimesheetGridRow]
    private let summaryRows: [MonarchTimesheetGridSummaryRow]
    private let maximumMinutesPerDay: Int
    private let layout: MonarchTimesheetGridLayout
    private let isReadOnly: Bool
    private let emptyMessage: String?
    private let labels: MonarchTimesheetGridLabels
    private let rowActions: ((MonarchTimesheetGridRow) -> [MonarchDataTableRowAction])?
    private let onDayActivate: (@MainActor (Date) -> Void)?
    private let onCellActivate: (@MainActor (String, Date) -> Void)?
    private let onCellChange: (@MainActor (String, Date, Int) async throws -> Void)?

    @State private var errors: [MonarchTimesheetGridCellKey: String] = [:]
    @State private var saving: Set<MonarchTimesheetGridCellKey> = []
    @State private var message: String?
    @State private var selectedDay: Date?
    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    #if os(iOS)
        @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    #endif

    private typealias Tokens = MonarchTimesheetGridTokens

    /// A timesheet grid.
    ///
    /// - Parameters:
    ///   - days: The days, usually the seven days of a week, in order.
    ///   - rows: The rows, with the minutes logged per day.
    ///   - summaryRows: Display-only footer rows, such as attendance.
    ///   - maximumMinutesPerDay: The largest value a cell accepts, and the
    ///     day total above which a day is flagged. 24 hours by default.
    ///   - layout: A grid, day pages, or the one that fits the width.
    ///   - isReadOnly: Whether every cell is read-only, for example after
    ///     submission.
    ///   - emptyMessage: Shown when there are no rows. Built in: "No time
    ///     logged for this period."
    ///   - labels: Replacements for the built-in strings.
    ///   - rowActions: The actions of a row, in a menu beside its name.
    ///   - onDayActivate: Makes day headings buttons, for example to open a
    ///     day view.
    ///   - onCellActivate: Makes read-only cells with time buttons, for
    ///     example to open the entries behind them.
    ///   - onCellChange: Saves a value typed in a cell: the row's
    ///     identifier, the day's date and the minutes (0 clears the cell).
    ///     A thrown error is shown on the cell. Without it, cells are read-only.
    public init(
        days: [MonarchTimesheetGridDay],
        rows: [MonarchTimesheetGridRow],
        summaryRows: [MonarchTimesheetGridSummaryRow] = [],
        maximumMinutesPerDay: Int = 1440,
        layout: MonarchTimesheetGridLayout = .automatic,
        isReadOnly: Bool = false,
        emptyMessage: String? = nil,
        labels: MonarchTimesheetGridLabels = MonarchTimesheetGridLabels(),
        rowActions: ((MonarchTimesheetGridRow) -> [MonarchDataTableRowAction])? = nil,
        onDayActivate: (@MainActor (Date) -> Void)? = nil,
        onCellActivate: (@MainActor (String, Date) -> Void)? = nil,
        onCellChange: (@MainActor (String, Date, Int) async throws -> Void)? = nil
    ) {
        self.days = days
        self.rows = rows
        self.summaryRows = summaryRows
        self.maximumMinutesPerDay = max(0, maximumMinutesPerDay)
        self.layout = layout
        self.isReadOnly = isReadOnly
        self.emptyMessage = emptyMessage
        self.labels = labels
        self.rowActions = rowActions
        self.onDayActivate = onDayActivate
        self.onCellActivate = onCellActivate
        self.onCellChange = onCellChange
    }

    private var isRegularWidth: Bool {
        #if os(iOS)
            horizontalSizeClass == .regular
        #else
            true
        #endif
    }

    private var model: MonarchTimesheetGridModel {
        MonarchTimesheetGridModel(
            days: days,
            rows: rows,
            summaryRows: summaryRows,
            maximum: maximumMinutesPerDay,
            isEditable: !isReadOnly && onCellChange != nil,
            emptyMessage: emptyMessage,
            labels: labels,
            errors: errors,
            saving: saving,
            rowActions: rowActions,
            onDayActivate: onDayActivate,
            onCellActivate: onCellActivate,
            commit: { commit($0, $1) },
            report: { report($0, $1) }
        )
    }

    /// The content of the view.
    public var body: some View {
        VStack(alignment: .leading, spacing: Tokens.spacing) {
            if Tokens.usesGrid(
                layout,
                isRegularWidth: isRegularWidth,
                isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
            ) {
                MonarchTimesheetGridTable(model: model)
                if let message {
                    HStack(alignment: .firstTextBaseline, spacing: Tokens.iconSpacing) {
                        MonarchIcon(systemName: Tokens.warningSymbol, size: .sm)
                        Text(message).monarchTextStyle(.caption)
                    }
                    .foregroundStyle(theme.colors.dangerStrong)
                    .accessibilityHidden(true)
                }
            } else {
                MonarchTimesheetGridDayPage(model: model, selectedDay: $selectedDay)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func commit(_ key: MonarchTimesheetGridCellKey, _ minutes: Int) {
        guard let onCellChange, !isReadOnly, let row = rows.first(where: { $0.id == key.row }) else { return }
        report(key, nil)
        guard minutes != row.minutes(on: key.day) else { return }
        saving.insert(key)
        Task {
            do {
                try await onCellChange(key.row, key.day, minutes)
            } catch {
                report(key, Tokens.failureMessage(for: error, labels: labels))
            }
            saving.remove(key)
        }
    }

    private func report(_ key: MonarchTimesheetGridCellKey, _ problem: String?) {
        if let problem {
            errors[key] = problem
            message = problem
            AccessibilityNotification.Announcement(MonarchTimesheetGridStrings.error(problem)).post()
        } else if errors[key] != nil {
            errors[key] = nil
            if errors.isEmpty { message = nil }
        }
    }
}

#if DEBUG
    /// The week shown in the preview.
    private let monarchTimesheetGridPreviewWeek: [MonarchTimesheetGridDay] = {
        let monday = Date(timeIntervalSinceReferenceDate: 790_000_000)
        var days: [MonarchTimesheetGridDay] = []
        for offset in 0..<7 {
            let date: Date = monday.addingTimeInterval(Double(offset) * 86_400)
            let reason: String? = offset == 0 ? "Approved" : nil
            let expected: Int? = offset < 5 ? 480 : nil
            days.append(
                MonarchTimesheetGridDay(
                    date,
                    isToday: offset == 2,
                    isNonWorking: offset >= 5,
                    lockReason: reason,
                    expectedMinutes: expected
                )
            )
        }
        return days
    }()

    /// The rows shown in the preview.
    private let monarchTimesheetGridPreviewRows: [MonarchTimesheetGridRow] = {
        let week = monarchTimesheetGridPreviewWeek
        let atlas: [Date: Int] = [week[0].date: 240, week[1].date: 90]
        let support: [Date: Int] = [week[2].date: 45]
        return [
            MonarchTimesheetGridRow(id: "atlas", label: "Atlas", description: "Billable", cells: atlas),
            MonarchTimesheetGridRow(id: "timer", label: "Support", cells: support, isReadOnly: true),
        ]
    }()

    #Preview("MonarchTimesheetGrid") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchTimesheetGrid(
                    days: monarchTimesheetGridPreviewWeek,
                    rows: monarchTimesheetGridPreviewRows,
                    layout: .grid,
                    onCellChange: { _, _, _ in }
                )
                MonarchTimesheetGrid(
                    days: monarchTimesheetGridPreviewWeek,
                    rows: monarchTimesheetGridPreviewRows,
                    layout: .day,
                    onCellChange: { _, _, _ in }
                )
                MonarchTimesheetGrid(days: monarchTimesheetGridPreviewWeek, rows: [], layout: .grid)
            }
        }
    }
#endif
