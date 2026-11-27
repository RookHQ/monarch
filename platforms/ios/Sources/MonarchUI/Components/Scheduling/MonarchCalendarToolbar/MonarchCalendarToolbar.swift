import SwiftUI

/// The header of a calendar: Today, previous and next, the range title, a
/// busy indicator, the view switcher and the app's actions.
///
/// ```swift
/// MonarchCalendarToolbar(
///     "November 2026",
///     view: $view,
///     isBusy: isLoading,
///     onToday: { model.showToday() },
///     onPrevious: { model.move(by: -1) },
///     onNext: { model.move(by: 1) }
/// )
/// ```
///
/// Navigation is the app's: the toolbar only calls back. Put it above the
/// calendar, for example in `safeAreaInset(edge: .top)`. When the controls do
/// not fit on one line it puts the title on its own line, and at the largest
/// text sizes stacks the controls under it.
///
/// Accessibility: the title has the header trait and is announced when it
/// changes. The arrows are named for the view ("Previous week", "Next
/// week") and follow the reading direction. The view switcher is a menu
/// named "Calendar view" whose value is the current view. The busy indicator
/// is named "Loading". Every button has a touch target of at least 44 points.
public struct MonarchCalendarToolbar<Actions: View>: View {
    private let title: String
    @Binding private var view: MonarchCalendarToolbarView
    private let views: [MonarchCalendarToolbarView]
    private let isBusy: Bool
    private let isTodayDisabled: Bool
    private let isPreviousDisabled: Bool
    private let isNextDisabled: Bool
    private let labels: MonarchCalendarToolbarLabels
    private let onToday: @MainActor () -> Void
    private let onPrevious: @MainActor () -> Void
    private let onNext: @MainActor () -> Void
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme

    /// A calendar toolbar with actions at the end.
    ///
    /// - Parameters:
    ///   - title: The range shown, already formatted: "November 2026", "2 – 8 Nov 2026".
    ///   - view: The current view. It sets the unit the arrows are named for.
    ///   - views: The views offered in the switcher, in order. With one view or none there is
    ///     no switcher.
    ///   - isBusy: Whether the range is loading. Shows a small indicator after the title.
    ///   - isTodayDisabled: Whether Today is disabled, for example when the range already contains today.
    ///   - isPreviousDisabled: Whether previous is disabled, for example at the start of a booking window.
    ///   - isNextDisabled: Whether next is disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - onToday: Moves to the range that contains today.
    ///   - onPrevious: Moves back one unit of the view.
    ///   - onNext: Moves forward one unit of the view.
    ///   - actions: Controls at the end: a filter, a New event button, a date picker.
    public init(
        _ title: String,
        view: Binding<MonarchCalendarToolbarView>,
        views: [MonarchCalendarToolbarView] = MonarchCalendarToolbarView.allCases,
        isBusy: Bool = false,
        isTodayDisabled: Bool = false,
        isPreviousDisabled: Bool = false,
        isNextDisabled: Bool = false,
        labels: MonarchCalendarToolbarLabels = MonarchCalendarToolbarLabels(),
        onToday: @escaping @MainActor () -> Void,
        onPrevious: @escaping @MainActor () -> Void,
        onNext: @escaping @MainActor () -> Void,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        _view = view
        self.views = views
        self.isBusy = isBusy
        self.isTodayDisabled = isTodayDisabled
        self.isPreviousDisabled = isPreviousDisabled
        self.isNextDisabled = isNextDisabled
        self.labels = labels
        self.onToday = onToday
        self.onPrevious = onPrevious
        self.onNext = onNext
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: MonarchCalendarToolbarTokens.spacing) {
                navigation
                heading
                Spacer(minLength: 0)
                trailing
            }
            // As the web lays it out when it is narrower: Today, the arrows and the title, then the views and actions.
            VStack(alignment: .leading, spacing: MonarchCalendarToolbarTokens.spacing) {
                HStack(spacing: MonarchCalendarToolbarTokens.spacing) {
                    navigation
                    heading
                }
                HStack(spacing: MonarchCalendarToolbarTokens.spacing) { trailing }
            }
            VStack(alignment: .leading, spacing: MonarchCalendarToolbarTokens.spacing) {
                heading
                HStack(spacing: MonarchCalendarToolbarTokens.spacing) { navigation }
                HStack(spacing: MonarchCalendarToolbarTokens.spacing) { trailing }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .onChange(of: title) { _, new in
            AccessibilityNotification.Announcement(new).post()
        }
    }

    private var navigation: some View {
        HStack(spacing: MonarchCalendarToolbarTokens.arrowSpacing) {
            MonarchButton(
                MonarchCalendarToolbarTokens.today(labels),
                size: MonarchCalendarToolbarTokens.buttonSize,
                action: onToday
            )
            .disabled(isTodayDisabled)
            .padding(.trailing, MonarchCalendarToolbarTokens.spacing)
            arrow(
                MonarchCalendarToolbarTokens.previousSymbol,
                label: MonarchCalendarToolbarTokens.previousLabel(view, labels: labels),
                isDisabled: isPreviousDisabled,
                action: onPrevious
            )
            arrow(
                MonarchCalendarToolbarTokens.nextSymbol,
                label: MonarchCalendarToolbarTokens.nextLabel(view, labels: labels),
                isDisabled: isNextDisabled,
                action: onNext
            )
        }
        .fixedSize()
    }

    private var heading: some View {
        HStack(spacing: MonarchCalendarToolbarTokens.spacing) {
            Text(title)
                .monarchTextStyle(MonarchCalendarToolbarTokens.titleStyle)
                .foregroundStyle(theme.colors[role: MonarchCalendarToolbarTokens.titleColor])
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            if isBusy {
                ProgressView()
                    .progressViewStyle(.monarchCircular(size: .sm))
                    .accessibilityLabel(MonarchCalendarToolbarTokens.loading(labels))
            }
        }
    }

    @ViewBuilder private var trailing: some View {
        if MonarchCalendarToolbarTokens.showsSwitcher(views) {
            // The views are always a segmented control, as on the web, never a menu.
            MonarchSegmentedControl(
                MonarchCalendarToolbarTokens.switcherLabel(labels),
                selection: $view,
                items: views.map {
                    MonarchSegmentedItem(MonarchCalendarToolbarTokens.viewName($0, labels: labels), value: $0)
                },
                size: MonarchCalendarToolbarTokens.buttonSize,
                fillsWidth: false
            )
            .fixedSize()
        }
        actions
    }

    private func arrow(
        _ symbol: String,
        label: String,
        isDisabled: Bool,
        action: @escaping @MainActor () -> Void
    ) -> some View {
        Button(action: action) {
            MonarchIcon(systemName: symbol, label: label, size: .sm)
        }
        .buttonStyle(.monarch(.ghost, size: MonarchCalendarToolbarTokens.buttonSize))
        .disabled(isDisabled)
    }
}

extension MonarchCalendarToolbar where Actions == EmptyView {
    /// A calendar toolbar without actions.
    ///
    /// - Parameters:
    ///   - title: The range shown, already formatted.
    ///   - view: The current view.
    ///   - views: The views offered in the switcher, in order.
    ///   - isBusy: Whether the range is loading.
    ///   - isTodayDisabled: Whether Today is disabled.
    ///   - isPreviousDisabled: Whether previous is disabled.
    ///   - isNextDisabled: Whether next is disabled.
    ///   - labels: Replacements for the built-in strings.
    ///   - onToday: Moves to the range that contains today.
    ///   - onPrevious: Moves back one unit of the view.
    ///   - onNext: Moves forward one unit of the view.
    public init(
        _ title: String,
        view: Binding<MonarchCalendarToolbarView>,
        views: [MonarchCalendarToolbarView] = MonarchCalendarToolbarView.allCases,
        isBusy: Bool = false,
        isTodayDisabled: Bool = false,
        isPreviousDisabled: Bool = false,
        isNextDisabled: Bool = false,
        labels: MonarchCalendarToolbarLabels = MonarchCalendarToolbarLabels(),
        onToday: @escaping @MainActor () -> Void,
        onPrevious: @escaping @MainActor () -> Void,
        onNext: @escaping @MainActor () -> Void
    ) {
        self.init(
            title,
            view: view,
            views: views,
            isBusy: isBusy,
            isTodayDisabled: isTodayDisabled,
            isPreviousDisabled: isPreviousDisabled,
            isNextDisabled: isNextDisabled,
            labels: labels,
            onToday: onToday,
            onPrevious: onPrevious,
            onNext: onNext,
            actions: { EmptyView() }
        )
    }
}

#if DEBUG
    #Preview("MonarchCalendarToolbar") {
        MonarchPreviewMatrix {
            VStack {
                MonarchCalendarToolbar(
                    "November 2026",
                    view: .constant(.month),
                    onToday: {},
                    onPrevious: {},
                    onNext: {}
                )
                MonarchCalendarToolbar(
                    "2 – 8 Nov 2026",
                    view: .constant(.week),
                    isBusy: true,
                    isTodayDisabled: true,
                    onToday: {},
                    onPrevious: {},
                    onNext: {},
                    actions: { MonarchButton("New", systemImage: "plus", variant: .primary, size: .sm) {} }
                )
            }
        }
    }
#endif
