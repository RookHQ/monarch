import SwiftUI

/// What will appear in an empty view, and the action that creates it.
///
/// ```swift
/// MonarchEmptyState(
///     "No invoices yet",
///     message: "Invoices you send appear here.",
///     systemImage: "doc.text",
///     actionTitle: "New invoice"
/// ) {
///     createInvoice()
/// }
/// ```
///
/// It is the system `ContentUnavailableView` with Monarch's type, colours
/// and buttons. For a region that failed to load, use ``MonarchErrorState``.
///
/// Accessibility: the title has the header trait and the icon is hidden
/// from VoiceOver. The buttons are standard buttons with 44-point touch
/// targets. Everything scales with Dynamic Type.
public struct MonarchEmptyState: View {
    private let title: String
    private let message: String?
    private let systemImage: String?
    private let kind: MonarchEmptyStateKind
    private let size: MonarchEmptyStateSize
    private let tip: String?
    private let tipKey: String?
    private let actionTitle: String?
    private let secondaryActionTitle: String?
    private let clearFiltersLabel: String?
    private let action: (@MainActor () -> Void)?
    private let secondaryAction: (@MainActor () -> Void)?
    private let onClearFilters: (@MainActor () -> Void)?

    @Environment(\.monarchTheme) private var theme

    /// An empty state.
    ///
    /// - Parameters:
    ///   - title: What is empty, already localised.
    ///   - message: What will appear here, or what to do next.
    ///   - systemImage: Replaces the SF Symbol of the kind.
    ///   - kind: Why the view is empty, which selects the default icon.
    ///   - size: How much room the state takes.
    ///   - tip: A short hint under the buttons.
    ///   - tipKey: A key named in the tip, such as "N". Drawn as a key cap where it first appears in `tip`.
    ///   - actionTitle: The title of the main button. No button without it.
    ///   - secondaryActionTitle: The title of the second button. No button without it.
    ///   - clearFiltersLabel: Replaces the built-in "Clear filters" title.
    ///   - action: Runs when the main button is activated.
    ///   - secondaryAction: Runs when the second button is activated.
    ///   - onClearFilters: Clears the app's filters. With the `filtered` kind
    ///     it adds a button before the others.
    public init(
        _ title: String,
        message: String? = nil,
        systemImage: String? = nil,
        kind: MonarchEmptyStateKind = .firstRun,
        size: MonarchEmptyStateSize = .md,
        tip: String? = nil,
        tipKey: String? = nil,
        actionTitle: String? = nil,
        secondaryActionTitle: String? = nil,
        clearFiltersLabel: String? = nil,
        action: (@MainActor () -> Void)? = nil,
        secondaryAction: (@MainActor () -> Void)? = nil,
        onClearFilters: (@MainActor () -> Void)? = nil
    ) {
        self.title = title
        self.message = message
        self.systemImage = systemImage
        self.kind = kind
        self.size = size
        self.tip = tip
        self.tipKey = tipKey
        self.actionTitle = actionTitle
        self.secondaryActionTitle = secondaryActionTitle
        self.clearFiltersLabel = clearFiltersLabel
        self.action = action
        self.secondaryAction = secondaryAction
        self.onClearFilters = onClearFilters
    }

    private var hasActions: Bool {
        MonarchEmptyStateTokens.showsClearFilters(kind: kind, hasHandler: onClearFilters != nil)
            || MonarchEmptyStateTokens.showsAction(title: actionTitle, hasHandler: action != nil)
            || MonarchEmptyStateTokens.showsAction(title: secondaryActionTitle, hasHandler: secondaryAction != nil)
            || tip != nil
    }

    /// The content of the view.
    public var body: some View {
        let buttonSize = MonarchEmptyStateTokens.buttonSize(size)
        MonarchEmptyStateLayout(
            title: title,
            message: message,
            symbol: MonarchEmptyStateTokens.symbol(kind: kind, override: systemImage),
            iconRole: MonarchEmptyStateTokens.iconRole(kind: kind),
            tileRole: MonarchEmptyStateTokens.tileRole(kind: kind),
            size: size,
            hasActions: hasActions
        ) {
            if let onClearFilters,
                MonarchEmptyStateTokens.showsClearFilters(kind: kind, hasHandler: true)
            {
                Button(clearFiltersLabel ?? MonarchEmptyStateStrings.clearFilters, action: onClearFilters)
                    .buttonStyle(.monarch(.secondary, size: buttonSize))
            }
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.monarch(.primary, size: buttonSize))
            }
            if let secondaryActionTitle, let secondaryAction {
                Button(secondaryActionTitle, action: secondaryAction)
                    .buttonStyle(.monarch(.secondary, size: buttonSize))
            }
            if let tip {
                tipView(tip)
            }
        }
    }
}

extension MonarchEmptyState {
    /// The tip, with its key drawn as a key cap when `tipKey` appears in it as a whole word.
    @ViewBuilder fileprivate func tipView(_ tip: String) -> some View {
        let parts = tipKey.flatMap { MonarchEmptyStateTokens.splitTip(tip, key: $0) }
        if let parts {
            HStack(spacing: MonarchEmptyStateTokens.tipSpacing) {
                if !parts.before.isEmpty { Text(parts.before) }
                MonarchKbd(parts.key, isAlwaysVisible: true)
                if !parts.after.isEmpty { Text(parts.after) }
            }
            .monarchTextStyle(.caption)
            .foregroundStyle(theme.colors[role: MonarchEmptyStateTokens.message])
            .multilineTextAlignment(.center)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(tip)
        } else {
            Text(tip)
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchEmptyStateTokens.message])
        }
    }
}

#if DEBUG
    #Preview("MonarchEmptyState") {
        MonarchPreviewMatrix {
            VStack {
                MonarchEmptyState(
                    "No invoices yet",
                    message: "Invoices you send appear here.",
                    systemImage: "doc.text",
                    tip: "You can also import a CSV file.",
                    actionTitle: "New invoice",
                    secondaryActionTitle: "Import",
                    action: {},
                    secondaryAction: {}
                )
                MonarchEmptyState("No results", kind: .filtered, size: .sm, onClearFilters: {})
                MonarchEmptyState("All caught up", kind: .success, size: .sm)
            }
        }
    }
#endif
