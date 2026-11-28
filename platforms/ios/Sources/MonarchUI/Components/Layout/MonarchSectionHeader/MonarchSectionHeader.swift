import SwiftUI

/// The title of a section that has no box around it, with an optional
/// description, a meta line and trailing actions.
///
/// ```swift
/// MonarchSectionHeader("Invoices", description: "Sent in the last 30 days", meta: "12 invoices") {
///     MonarchButton("New invoice", size: .sm) { create() }
/// }
/// ```
///
/// Put the section's content after it in a stack, or use it as the `header`
/// of a `Section` in a `List`.
///
/// Accessibility: the title has the header trait, so VoiceOver's Headings
/// rotor stops on it. While `isBusy` is true the title's value is "Loading"
/// and the progress indicator is hidden from VoiceOver. The description and
/// the meta line are read after the title, and each action stays its own
/// element. At the accessibility text sizes the actions move below the text.
public struct MonarchSectionHeader<Actions: View>: View {
    private let title: String
    private let description: String?
    private let meta: String?
    private let size: MonarchSectionHeaderSize
    private let isBusy: Bool
    private let busyLabel: String?
    private let hasActions: Bool
    private let actions: Actions

    @Environment(\.monarchTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A section header with trailing actions.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - description: A sentence under the title that explains the section.
    ///   - meta: A short secondary line: a count, a date range, "Updated 5 min ago".
    ///   - size: The size of the title.
    ///   - isBusy: Whether the section's content is loading or refreshing.
    ///   - busyLabel: Replaces the built-in "Loading" spoken value.
    ///   - actions: Buttons, a menu or a link at the trailing edge.
    public init(
        _ title: String,
        description: String? = nil,
        meta: String? = nil,
        size: MonarchSectionHeaderSize = .sm,
        isBusy: Bool = false,
        busyLabel: String? = nil,
        @ViewBuilder actions: () -> Actions
    ) {
        self.title = title
        self.description = description
        self.meta = meta
        self.size = size
        self.isBusy = isBusy
        self.busyLabel = busyLabel
        self.hasActions = true
        self.actions = actions()
    }

    /// The content of the view.
    public var body: some View {
        Group {
            if !hasActions {
                text
            } else if MonarchSectionHeaderTokens.stacksActions(at: dynamicTypeSize) {
                VStack(alignment: .leading, spacing: MonarchSectionHeaderTokens.stackedSpacing) {
                    text
                    actionRow
                }
            } else {
                HStack(alignment: .top, spacing: MonarchSectionHeaderTokens.barSpacing) {
                    text.frame(maxWidth: .infinity, alignment: .leading)
                    actionRow
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var text: some View {
        VStack(alignment: .leading, spacing: MonarchSectionHeaderTokens.textSpacing) {
            HStack(spacing: MonarchSectionHeaderTokens.titleSpacing) {
                Text(title)
                    .monarchTextStyle(
                        MonarchSectionHeaderTokens.titleStyle(size),
                        weight: MonarchSectionHeaderTokens.titleWeight
                    )
                    .foregroundStyle(theme.colors[role: MonarchSectionHeaderTokens.titleColor])
                    .accessibilityAddTraits(.isHeader)
                    .accessibilityValue(
                        MonarchSectionHeaderTokens.accessibilityValue(isBusy: isBusy, busyLabel: busyLabel)
                    )
                if isBusy {
                    ProgressView()
                        .controlSize(.small)
                        .accessibilityHidden(true)
                }
            }
            if let description {
                Text(description)
                    .monarchTextStyle(
                        MonarchSectionHeaderTokens.descriptionStyle,
                        weight: MonarchSectionHeaderTokens.descriptionWeight
                    )
                    .foregroundStyle(theme.colors[role: MonarchSectionHeaderTokens.supportingColor])
            }
            if let meta {
                Text(meta)
                    .monarchTextStyle(MonarchSectionHeaderTokens.metaStyle)
                    .foregroundStyle(theme.colors[role: MonarchSectionHeaderTokens.supportingColor])
            }
        }
        .multilineTextAlignment(.leading)
    }

    private var actionRow: some View {
        HStack(spacing: MonarchSectionHeaderTokens.actionSpacing) {
            actions
        }
    }
}

extension MonarchSectionHeader where Actions == EmptyView {
    /// A section header without actions.
    ///
    /// - Parameters:
    ///   - title: The section title, already localised.
    ///   - description: A sentence under the title that explains the section.
    ///   - meta: A short secondary line: a count, a date range, "Updated 5 min ago".
    ///   - size: The size of the title.
    ///   - isBusy: Whether the section's content is loading or refreshing.
    ///   - busyLabel: Replaces the built-in "Loading" spoken value.
    public init(
        _ title: String,
        description: String? = nil,
        meta: String? = nil,
        size: MonarchSectionHeaderSize = .sm,
        isBusy: Bool = false,
        busyLabel: String? = nil
    ) {
        self.title = title
        self.description = description
        self.meta = meta
        self.size = size
        self.isBusy = isBusy
        self.busyLabel = busyLabel
        self.hasActions = false
        self.actions = EmptyView()
    }
}

#if DEBUG
    #Preview("MonarchSectionHeader") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                MonarchSectionHeader("Recent activity")
                MonarchSectionHeader("Invoices", description: "Sent in the last 30 days", meta: "12 invoices") {
                    MonarchButton("New", size: .sm) {}
                }
                MonarchSectionHeader("Team", size: .md, isBusy: true) {
                    MonarchButton("Invite", variant: .ghost, size: .sm) {}
                }
            }
        }
    }
#endif
