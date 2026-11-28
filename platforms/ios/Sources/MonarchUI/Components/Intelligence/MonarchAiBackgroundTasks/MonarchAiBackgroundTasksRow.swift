import MonarchTokens
import SwiftUI

/// One task of ``MonarchAiBackgroundTasks``: icon, title, status line,
/// detail, bar and actions.
struct MonarchAiBackgroundTasksRow: View {
    let task: MonarchAiBackgroundTask
    let now: Date
    let labels: MonarchAiBackgroundTasksLabels
    let actions: [MonarchAiBackgroundTaskAction]
    let perform: @MainActor (MonarchAiBackgroundTaskAction) -> Void

    @Environment(\.monarchTheme) private var theme
    @Environment(\.locale) private var locale
    @Environment(\.monarchAiPopoverCard) private var isCard

    private var buttons: [MonarchAiBackgroundTaskAction] { actions.filter { $0 != .dismiss } }

    var body: some View {
        VStack(alignment: .leading, spacing: MonarchAiBackgroundTasksTokens.rowSpacing) {
            HStack(alignment: .top, spacing: MonarchAiBackgroundTasksTokens.iconSpacing) {
                icon
                VStack(alignment: .leading, spacing: MonarchAiBackgroundTasksTokens.textSpacing) {
                    Text(task.title)
                        .monarchTextStyle(.bodyStrong)
                        .foregroundStyle(theme.colors[role: MonarchAiBackgroundTasksTokens.title])
                    // The status in strong text, then the agent and the time in muted text, as on the web.
                    let meta = MonarchAiBackgroundTasksTokens.meta(task, now: now, locale: locale, labels: labels)
                    MonarchFlowLayout(spacing: MonarchTokens.Space.space2) {
                        if let status = meta.first {
                            Text(status)
                                .monarchTextStyle(.caption, weight: .semibold)
                                .foregroundStyle(theme.colors.textDefault)
                        }
                        ForEach(Array(meta.dropFirst().enumerated()), id: \.offset) { _, part in
                            Text(part)
                                .monarchTextStyle(.caption)
                                .foregroundStyle(theme.colors[role: MonarchAiBackgroundTasksTokens.muted])
                        }
                    }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(meta.joined(separator: ", "))
                    if let detail = MonarchAiBackgroundTasksTokens.detail(task) {
                        Text(detail)
                            .monarchTextStyle(.caption)
                            .foregroundStyle(
                                theme.colors[role: MonarchAiBackgroundTasksTokens.detailColor(task.status)]
                            )
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .combine)
                if actions.contains(.dismiss) {
                    Button {
                        perform(.dismiss)
                    } label: {
                        MonarchIcon(
                            systemName: MonarchAiBackgroundTasksTokens.dismissSymbol,
                            label: MonarchAiBackgroundTasksTokens.actionLabel(
                                .dismiss,
                                title: task.title,
                                labels: labels
                            ),
                            size: .sm
                        )
                        .foregroundStyle(theme.colors[role: MonarchAiBackgroundTasksTokens.muted])
                        .monarchMinimumHitTarget()
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
            }
            if MonarchAiBackgroundTasksTokens.showsBar(task) {
                ProgressView(value: MonarchAiBackgroundTasksTokens.fraction(task)) {
                    EmptyView()
                }
                .progressViewStyle(.monarchLinear)
                .accessibilityLabel(
                    labels.progressLabel?(task.title) ?? MonarchAiBackgroundTasksStrings.progressLabel(task.title)
                )
            }
            if !buttons.isEmpty {
                // A flow layout, so the buttons wrap at the largest text sizes.
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: MonarchAiBackgroundTasksTokens.actionSpacing) { actionButtons }
                    VStack(alignment: .leading, spacing: MonarchAiBackgroundTasksTokens.actionSpacing) { actionButtons }
                }
            }
        }
        .padding(.vertical, isCard ? MonarchTokens.Space.space3 : MonarchAiBackgroundTasksTokens.rowPadding)
        .padding(.horizontal, isCard ? MonarchTokens.Space.space3 : 0)
        .modifier(CardChrome(isCard: isCard, status: task.status))
        .swipeActions(edge: .trailing) {
            if actions.contains(.dismiss) {
                Button(MonarchAiBackgroundTasksTokens.actionTitle(.dismiss, labels: labels), role: .destructive) {
                    perform(.dismiss)
                }
            }
            if actions.contains(.cancel) {
                Button(MonarchAiBackgroundTasksTokens.actionTitle(.cancel, labels: labels), role: .destructive) {
                    perform(.cancel)
                }
            }
        }
    }

    @ViewBuilder private var icon: some View {
        if task.status == .running {
            Circle()
                .trim(from: 0, to: 0.75)
                .stroke(theme.colors.aiStrong, style: StrokeStyle(lineWidth: 2, lineCap: .round))
                .frame(width: 16, height: 16)
                .frame(width: 20, height: 20)
                .accessibilityHidden(true)
        } else {
            MonarchIcon(systemName: MonarchAiBackgroundTasksTokens.symbol(task.status), size: .md)
                .foregroundStyle(theme.colors[role: MonarchAiBackgroundTasksTokens.symbolColor(task.status)])
        }
    }

    @ViewBuilder private var actionButtons: some View {
        ForEach(buttons, id: \.self) { action in
            MonarchButton(
                MonarchAiBackgroundTasksTokens.actionTitle(action, labels: labels),
                variant: variant(action),
                size: .sm
            ) {
                perform(action)
            }
            .accessibilityLabel(MonarchAiBackgroundTasksTokens.actionLabel(action, title: task.title, labels: labels))
        }
    }

    private func variant(_ action: MonarchAiBackgroundTaskAction) -> MonarchButtonVariant {
        switch action {
        case .respond: .primary
        case .retry, .open: .secondary
        case .cancel, .dismiss: .ghost
        }
    }
}

/// The bordered card of a task in the inline tray, with the accent strip on its leading edge.
private struct CardChrome: ViewModifier {
    let isCard: Bool
    let status: MonarchAiBackgroundTaskStatus
    @Environment(\.monarchTheme) private var theme

    func body(content: Content) -> some View {
        if isCard {
            let strip: Color =
                switch status {
                case .needsInput: theme.colors.warningStrong.opacity(0.3)
                case .running: theme.colors.aiStrong.opacity(0.3)
                default: theme.colors.borderDefault
                }
            let shape = RoundedRectangle(cornerRadius: MonarchTokens.Radius.md)
            content
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(alignment: .leading) { Rectangle().fill(strip).frame(width: 3) }
                .clipShape(shape)
                .overlay(shape.stroke(theme.colors.borderDefault, lineWidth: 1))
        } else {
            content
        }
    }
}
