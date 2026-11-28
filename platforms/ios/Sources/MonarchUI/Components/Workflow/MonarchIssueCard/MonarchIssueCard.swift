import SwiftUI

/// An issue on a board or in a list: key, title, status, priority, tags,
/// due date and assignee. The title opens the issue; status and priority
/// can be changed in place with menus.
///
/// ```swift
/// MonarchIssueCard(
///     "Sign-in fails on slow networks",
///     key: "STR-142",
///     status: inProgress,
///     statuses: workflow,
///     priority: .high,
///     assignee: .person("Ana Ruiz"),
///     tags: ["Bug", "Auth"],
///     dueDate: dueDate,
///     onStatusChange: { id in move(issue, to: id) },
///     onOpen: { open(issue) }
/// )
/// ```
///
/// The title is a link when `url` is an http or https address, a button
/// when `onOpen` is given, and plain text otherwise. A due date before
/// today, in the environment's calendar and time zone, shows "Overdue"
/// unless the status is completed or cancelled. Pass `today` for a fixed
/// reference day.
///
/// Accessibility: the card is a container. The title is read with the key
/// first ("STR-142, Sign-in fails…"), and has the selected trait when the
/// card is selected. Priority, status, due date and assignee are read as
/// "Priority: High", "Status: In progress", "Due Mar 12, Overdue" and
/// "Assigned to Ana Ruiz"; each has a symbol of its own shape, so nothing
/// relies on colour. Menus have 44-point targets.
@_spi(Experimental) public struct MonarchIssueCard: View {
    private let title: String
    private let key: String
    private let url: URL?
    private let status: MonarchIssueCardStatus?
    private let statuses: [MonarchIssueCardStatus]
    private let priority: MonarchIssueCardPriority?
    private let assignee: MonarchIssueCardAssignee?
    private let tags: [String]
    private let dueDate: Date?
    private let today: Date?
    private let isSelected: Bool
    private let isDragging: Bool
    private let labels: MonarchIssueCardLabels
    private let onStatusChange: (@MainActor (_ statusID: String) -> Void)?
    private let onPriorityChange: (@MainActor (_ priority: MonarchIssueCardPriority) -> Void)?
    private let onOpen: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme
    @Environment(\.calendar) private var calendar
    @Environment(\.timeZone) private var timeZone
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var assigneeSide = MonarchIssueCardTokens.assigneeSide

    /// An issue card.
    ///
    /// - Parameters:
    ///   - title: The issue's title, already localised.
    ///   - key: The issue key, such as "STR-142".
    ///   - url: Where the issue opens. Only http and https addresses make the title a link.
    ///   - status: The current status.
    ///   - statuses: Every status. With `onStatusChange`, the status becomes a menu.
    ///   - priority: The priority. No priority symbol when `nil`.
    ///   - assignee: A person or the empty slot. No assignee slot when `nil`.
    ///   - tags: Labels on the issue, already localised.
    ///   - dueDate: The due day.
    ///   - today: The reference day for overdue. The current date when `nil`.
    ///   - isSelected: Whether the card is selected, for example in a multi-select board.
    ///   - isDragging: Whether the card is being dragged; lifts it.
    ///   - labels: Replacements for the built-in strings.
    ///   - onStatusChange: Changes the status from the card. Receives the status id.
    ///   - onPriorityChange: Changes the priority from the card.
    ///   - onOpen: Opens the issue. Makes the title a button when there is no `url`.
    public init(
        _ title: String,
        key: String,
        url: URL? = nil,
        status: MonarchIssueCardStatus? = nil,
        statuses: [MonarchIssueCardStatus] = [],
        priority: MonarchIssueCardPriority? = nil,
        assignee: MonarchIssueCardAssignee? = nil,
        tags: [String] = [],
        dueDate: Date? = nil,
        today: Date? = nil,
        isSelected: Bool = false,
        isDragging: Bool = false,
        labels: MonarchIssueCardLabels = MonarchIssueCardLabels(),
        onStatusChange: (@MainActor (_ statusID: String) -> Void)? = nil,
        onPriorityChange: (@MainActor (_ priority: MonarchIssueCardPriority) -> Void)? = nil,
        onOpen: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.key = key
        self.url = url
        self.status = status
        self.statuses = statuses
        self.priority = priority
        self.assignee = assignee
        self.tags = tags
        self.dueDate = dueDate
        self.today = today
        self.isSelected = isSelected
        self.isDragging = isDragging
        self.labels = labels
        self.onStatusChange = onStatusChange
        self.onPriorityChange = onPriorityChange
        self.onOpen = onOpen
    }

    private typealias Tokens = MonarchIssueCardTokens

    private var zonedCalendar: Calendar {
        var zoned = calendar
        zoned.timeZone = timeZone
        return zoned
    }

    /// The content of the view.
    public var body: some View {
        let surface = Tokens.surface(isSelected: isSelected, isDragging: isDragging)
        let shape = RoundedRectangle(cornerRadius: MonarchCardTokens.cornerRadius, style: .continuous)
        VStack(alignment: .leading, spacing: Tokens.stackSpacing) {
            header
            titleView
            meta
        }
        .padding(MonarchCardTokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
        .monarchElevation(surface.elevation, fill: surface.fill, in: shape)
        .overlay(shape.strokeBorder(theme.colors[role: surface.border], lineWidth: surface.borderWidth))
        .monarchAnimation(.fast, value: isDragging)
        .accessibilityElement(children: .contain)
    }

    private var header: some View {
        HStack(spacing: Tokens.itemSpacing) {
            if isSelected {
                MonarchIcon(systemName: Tokens.selectedSymbol, size: .sm)
                    .foregroundStyle(theme.colors.accentStrong)
            }
            Text(key)
                .monarchTextStyle(.codeSmall)
                .foregroundStyle(theme.colors.textMuted)
                .accessibilityHidden(true)
            Spacer(minLength: 0)
            if let priority {
                priorityView(priority)
            }
        }
    }

    @ViewBuilder private var titleView: some View {
        let label = Tokens.titleAccessibilityLabel(key: key, title: title)
        let text = Text(title)
            .monarchTextStyle(.bodyStrong)
            .foregroundStyle(theme.colors.textDefault)
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
        Group {
            if let safe = Tokens.safeURL(url) {
                Link(destination: safe) { text.contentShape(Rectangle()) }
                    .buttonStyle(.plain)
            } else if let onOpen {
                Button(action: onOpen) { text.contentShape(Rectangle()) }
                    .buttonStyle(.plain)
            } else {
                text
            }
        }
        .accessibilityLabel(label)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }

    private var meta: some View {
        HStack(alignment: .center, spacing: Tokens.itemSpacing) {
            MonarchFlowLayout(spacing: Tokens.itemSpacing) {
                if let status {
                    statusView(status)
                }
                if !tags.isEmpty {
                    ForEach(Array(tags.enumerated()), id: \.offset) { _, tag in
                        MonarchTag(tag, size: .sm)
                    }
                }
                if let dueDate {
                    dueView(dueDate)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            if let assignee {
                assigneeView(assignee)
            }
        }
    }

    @ViewBuilder private func priorityView(_ priority: MonarchIssueCardPriority) -> some View {
        let name = labels.priorityLevel(priority)
        let icon = MonarchPriorityGlyph(priority: priority, color: theme.colors[role: Tokens.priorityRole(priority)])
        if let onPriorityChange {
            Menu {
                Picker(
                    labels.priority(name),
                    selection: Binding(get: { priority }, set: { onPriorityChange($0) })
                ) {
                    ForEach(MonarchIssueCardPriority.allCases, id: \.self) { level in
                        Label {
                            Text(labels.priorityLevel(level))
                        } icon: {
                            if let lit = MonarchPriorityGlyph.litBars(level) {
                                Image(systemName: "cellularbars", variableValue: Double(lit) / 3)
                            } else {
                                Image(systemName: Tokens.prioritySymbol(level))
                            }
                        }
                        .tag(level)
                    }
                }
                .pickerStyle(.inline)
            } label: {
                icon.monarchMinimumHitTarget()
            }
            .buttonStyle(.plain)
            .accessibilityLabel(labels.priority(name))
        } else {
            icon
                .accessibilityHidden(false)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(labels.priority(name))
        }
    }

    @ViewBuilder private func statusView(_ status: MonarchIssueCardStatus) -> some View {
        let label = HStack(spacing: Tokens.iconSpacing) {
            MonarchIcon(systemName: Tokens.statusSymbol(status.category), size: .sm)
                .foregroundStyle(theme.colors[role: Tokens.statusRole(status.category)])
            Text(status.name)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors.textDefault)
        }
        if let onStatusChange, !statuses.isEmpty {
            Menu {
                Picker(
                    labels.status(status.name),
                    selection: Binding(get: { status.id }, set: { onStatusChange($0) })
                ) {
                    ForEach(statuses) { option in
                        Label(option.name, systemImage: Tokens.statusSymbol(option.category)).tag(option.id)
                    }
                }
                .pickerStyle(.inline)
            } label: {
                HStack(spacing: Tokens.iconSpacing) {
                    label
                    MonarchIcon(systemName: "chevron.down", size: .sm)
                        .foregroundStyle(theme.colors.textMuted)
                }
                // A bordered pill, as the web status picker; the touch target stays larger than the pill.
                .padding(.horizontal, Tokens.itemSpacing)
                .padding(.vertical, Tokens.iconSpacing)
                .overlay(Capsule().strokeBorder(theme.colors.borderDefault, lineWidth: MonarchShapes.borderWidth))
                .frame(minHeight: MonarchAccessibility.minimumHitTarget)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(labels.status(status.name))
        } else {
            label
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(labels.status(status.name))
        }
    }

    private func dueView(_ due: Date) -> some View {
        let calendar = zonedCalendar
        let isOverdue = Tokens.isOverdue(due: due, today: today ?? Date(), status: status, calendar: calendar)
        let dateText = Tokens.dueDateText(due, calendar: calendar, locale: locale)
        return HStack(spacing: Tokens.iconSpacing) {
            MonarchIcon(systemName: isOverdue ? Tokens.overdueSymbol : Tokens.dueSymbol, size: .sm)
            Text(labels.due(dateText)).monarchTextStyle(.caption)
            if isOverdue {
                Text(labels.overdue).monarchTextStyle(.caption, weight: .semibold)
            }
        }
        .foregroundStyle(isOverdue ? theme.colors.dangerStrong : theme.colors.textMuted)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Tokens.dueAccessibilityLabel(dateText: dateText, isOverdue: isOverdue, labels: labels))
    }

    @ViewBuilder private func assigneeView(_ assignee: MonarchIssueCardAssignee) -> some View {
        let label = Tokens.assigneeAccessibilityLabel(assignee, labels: labels)
        switch assignee {
        case .person(let name, let avatarURL):
            MonarchAvatar(url: avatarURL, name: name, size: .sm)
                .accessibilityLabel(label)
        case .unassigned:
            Circle()
                .strokeBorder(
                    theme.colors.borderStrong,
                    style: StrokeStyle(lineWidth: MonarchShapes.borderWidth, dash: Tokens.unassignedDash)
                )
                .frame(width: assigneeSide, height: assigneeSide)
                .accessibilityElement()
                .accessibilityLabel(label)
                .accessibilityAddTraits(.isImage)
        }
    }
}

#if DEBUG
    #Preview("MonarchIssueCard") {
        let today = Date(timeIntervalSinceReferenceDate: 800_000_000)
        let statuses = [
            MonarchIssueCardStatus(id: "todo", name: "Todo", category: .unstarted),
            MonarchIssueCardStatus(id: "doing", name: "In progress", category: .started),
            MonarchIssueCardStatus(id: "done", name: "Done", category: .completed),
        ]
        MonarchPreviewMatrix {
            VStack {
                MonarchIssueCard(
                    "Sign-in fails on slow networks",
                    key: "STR-142",
                    status: statuses[1],
                    statuses: statuses,
                    priority: .high,
                    assignee: .person("Ana Ruiz"),
                    tags: ["Bug", "Auth"],
                    dueDate: today.addingTimeInterval(-86_400 * 2),
                    today: today,
                    onStatusChange: { _ in },
                    onOpen: {}
                )
                MonarchIssueCard("Write the release notes", key: "STR-150", assignee: .unassigned, isSelected: true)
            }
        }
    }
#endif
