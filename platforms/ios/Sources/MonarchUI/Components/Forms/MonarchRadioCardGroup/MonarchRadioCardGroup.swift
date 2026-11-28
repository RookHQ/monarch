import SwiftUI

/// One choice shown as selectable cards, each with a title and an optional
/// description, icon, meta text such as a price and detail lines such as the
/// capabilities of a role.
///
/// ```swift
/// MonarchRadioCardGroup(
///     "Plan",
///     selection: $plan,
///     options: [
///         MonarchRadioCardOption("team", title: "Team", description: "Up to 10 people.", meta: "$20"),
///         MonarchRadioCardOption("business", title: "Business", description: "Unlimited people.", meta: "$60"),
///     ]
/// )
/// ```
///
/// For a short list of plain options, use the standard `Picker`.
///
/// Accessibility: the group is a container named by `label`. Each card is one
/// button whose label is the title, whose value is the meta text and the
/// description, and which has the selected trait when chosen. The selection is
/// also drawn as a filled mark and a heavier border, not colour alone. The
/// whole card is the touch target and is at least 44 points high. On a compact
/// width and at the accessibility text sizes the cards stack in one column.
public struct MonarchRadioCardGroup<SelectionValue: Hashable>: View {
    private let label: String
    @Binding private var selection: SelectionValue?
    private let options: [MonarchRadioCardOption<SelectionValue>]
    private let columns: Int
    private let compactColumns: Int
    private var isRequired = false

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// A group of selectable cards.
    ///
    /// - Parameters:
    ///   - label: The visible label of the group, already localised.
    ///   - selection: The value of the chosen card, or `nil` for no choice yet.
    ///   - options: The cards, in order.
    ///   - columns: Cards per row on a regular width, from 1 to 3.
    public init(
        _ label: String,
        selection: Binding<SelectionValue?>,
        options: [MonarchRadioCardOption<SelectionValue>],
        columns: Int = 1
    ) {
        self.label = label
        _selection = selection
        self.options = options
        self.columns = columns
        self.compactColumns = 1
    }

    /// A group of selectable cards that also keeps columns on a compact width, as the web shows two cards side by side.
    ///
    /// - Parameters:
    ///   - label: The visible label of the group, already localised.
    ///   - selection: The value of the chosen card, or `nil` for no choice yet.
    ///   - options: The cards, in order.
    ///   - columns: Cards per row on a regular width, from 1 to 3.
    ///   - compactColumns: Cards per row on a compact width, from 1 to 3. The accessibility text sizes still stack.
    ///   - isRequired: Shows a required mark after the label.
    public init(
        _ label: String,
        selection: Binding<SelectionValue?>,
        options: [MonarchRadioCardOption<SelectionValue>],
        columns: Int,
        compactColumns: Int,
        isRequired: Bool = false
    ) {
        self.label = label
        _selection = selection
        self.options = options
        self.columns = columns
        self.compactColumns = compactColumns
        self.isRequired = isRequired
    }

    /// The content of the view.
    public var body: some View {
        let count = MonarchRadioCardGroupTokens.columnCount(
            requested: horizontalSizeClass == .compact ? min(max(compactColumns, 1), columns) : columns,
            isCompactWidth: false,
            isAccessibilitySize: dynamicTypeSize.isAccessibilitySize
        )
        VStack(alignment: .leading, spacing: MonarchRadioCardGroupTokens.stackSpacing) {
            MonarchFieldLabelRow(label: label, isRequired: isRequired)
            if count == 1 {
                VStack(spacing: MonarchRadioCardGroupTokens.cardSpacing) { cards(isTile: count > 1) }
            } else {
                let column = GridItem(
                    .flexible(),
                    spacing: MonarchRadioCardGroupTokens.cardSpacing,
                    alignment: .top
                )
                LazyVGrid(
                    columns: Array(repeating: column, count: count),
                    spacing: MonarchRadioCardGroupTokens.cardSpacing
                ) { cards(isTile: count > 1) }
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(label)
    }

    private func cards(isTile: Bool) -> some View {
        ForEach(options) { option in
            let isSelected = selection == option.value
            Button {
                selection = option.value
            } label: {
                MonarchRadioCardLabel(option: option)
            }
            .buttonStyle(MonarchRadioCardButtonStyle(isSelected: isSelected))
            .environment(\.monarchRadioCardIsTile, isTile)
            .disabled(option.isDisabled)
            .accessibilityLabel(option.title)
            .accessibilityValue(
                MonarchRadioCardGroupTokens.accessibilityValue(
                    meta: option.meta,
                    description: option.description,
                    details: option.details
                )
            )
            .accessibilityAddTraits(isSelected ? .isSelected : [])
        }
    }
}

/// The colour roles of the card being drawn, from the button style to its label.
private struct MonarchRadioCardAppearanceKey: EnvironmentKey {
    static var defaultValue: MonarchRadioCardGroupTokens.Appearance {
        MonarchRadioCardGroupTokens.appearance(isSelected: false, isPressed: false, isEnabled: true)
    }
}

extension EnvironmentValues {
    fileprivate var monarchRadioCardAppearance: MonarchRadioCardGroupTokens.Appearance {
        get { self[MonarchRadioCardAppearanceKey.self] }
        set { self[MonarchRadioCardAppearanceKey.self] = newValue }
    }

    fileprivate var monarchRadioCardIsTile: Bool {
        get { self[MonarchRadioCardIsTileKey.self] }
        set { self[MonarchRadioCardIsTileKey.self] = newValue }
    }

    fileprivate var monarchRadioCardIsSelected: Bool {
        get { self[MonarchRadioCardIsSelectedKey.self] }
        set { self[MonarchRadioCardIsSelectedKey.self] = newValue }
    }
}

private struct MonarchRadioCardIsTileKey: EnvironmentKey {
    static let defaultValue = false
}

private struct MonarchRadioCardIsSelectedKey: EnvironmentKey {
    static let defaultValue = false
}

/// The icon, text and selection mark of a card.
private struct MonarchRadioCardLabel<Value: Hashable>: View {
    let option: MonarchRadioCardOption<Value>

    @Environment(\.monarchTheme) private var theme
    @Environment(\.monarchRadioCardAppearance) private var appearance
    @Environment(\.monarchRadioCardIsSelected) private var isSelected
    @Environment(\.monarchRadioCardIsTile) private var isTile
    @ScaledMetric(relativeTo: .body) private var markSize = MonarchRadioCardGroupTokens.markSize

    var body: some View {
        if isTile, let systemImage = option.systemImage {
            // Side by side, an option with an icon is a tile: the icon over the centred text, no radio mark, as on the web.
            VStack(spacing: MonarchRadioCardGroupTokens.textSpacing) {
                MonarchIcon(systemName: systemImage)
                    .foregroundStyle(theme.colors[role: appearance.icon])
                title
                if let description = option.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: appearance.secondary])
                }
            }
            .multilineTextAlignment(.center)
            .frame(maxWidth: .infinity)
        } else {
            row
        }
    }

    private var row: some View {
        HStack(alignment: .top, spacing: MonarchRadioCardGroupTokens.contentSpacing) {
            if let systemImage = option.systemImage {
                MonarchIcon(systemName: systemImage)
                    .foregroundStyle(theme.colors[role: appearance.icon])
            }
            VStack(alignment: .leading, spacing: MonarchRadioCardGroupTokens.textSpacing) {
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: MonarchRadioCardGroupTokens.metaSpacing) {
                        title
                        Spacer(minLength: 0)
                        meta
                    }
                    VStack(alignment: .leading, spacing: MonarchRadioCardGroupTokens.textSpacing) {
                        title
                        meta
                    }
                }
                if let description = option.description, !description.isEmpty {
                    Text(description)
                        .monarchTextStyle(.caption)
                        .foregroundStyle(theme.colors[role: appearance.secondary])
                }
                ForEach(Array(option.details.filter { !$0.isEmpty }.enumerated()), id: \.offset) { _, detail in
                    HStack(alignment: .firstTextBaseline, spacing: MonarchRadioCardGroupTokens.metaSpacing) {
                        MonarchIcon(systemName: MonarchRadioCardGroupTokens.detailSymbol, size: .sm)
                            .accessibilityHidden(true)
                        Text(detail).monarchTextStyle(.caption)
                    }
                    .foregroundStyle(theme.colors[role: appearance.secondary])
                }
            }
            .multilineTextAlignment(.leading)
            .frame(maxWidth: .infinity, alignment: .leading)
            mark
        }
    }

    private var title: some View {
        Text(option.title)
            .monarchTextStyle(.label)
            .foregroundStyle(theme.colors[role: appearance.title])
    }

    @ViewBuilder private var meta: some View {
        if let meta = option.meta, !meta.isEmpty {
            Text(meta)
                .monarchTextStyle(.bodySmall)
                .foregroundStyle(theme.colors[role: appearance.secondary])
        }
    }

    private var mark: some View {
        Circle()
            .strokeBorder(theme.colors[role: appearance.mark], lineWidth: MonarchShapes.borderWidth)
            .overlay {
                if isSelected {
                    Circle()
                        .fill(theme.colors[role: appearance.mark])
                        .frame(
                            width: markSize * MonarchRadioCardGroupTokens.markDotRatio,
                            height: markSize * MonarchRadioCardGroupTokens.markDotRatio
                        )
                }
            }
            .frame(width: markSize, height: markSize)
            .accessibilityHidden(true)
    }
}

/// Draws the card surface for the pressed, selected and disabled states.
private struct MonarchRadioCardButtonStyle: ButtonStyle {
    let isSelected: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchRadioCardSurface(configuration: configuration, isSelected: isSelected)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchRadioCardSurface: View {
    let configuration: ButtonStyleConfiguration
    let isSelected: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.monarchRadioCardIsTile) private var isTile

    var body: some View {
        let appearance = MonarchRadioCardGroupTokens.appearance(
            isSelected: isSelected,
            isPressed: configuration.isPressed,
            isEnabled: isEnabled
        )
        let shape = RoundedRectangle(cornerRadius: MonarchRadioCardGroupTokens.cornerRadius, style: .continuous)
        configuration.label
            .environment(\.monarchRadioCardAppearance, appearance)
            .environment(\.monarchRadioCardIsSelected, isSelected)
            .padding(.horizontal, MonarchRadioCardGroupTokens.horizontalPadding)
            .padding(.vertical, MonarchRadioCardGroupTokens.verticalPadding)
            .frame(
                maxWidth: .infinity,
                minHeight: MonarchAccessibility.minimumHitTarget,
                alignment: isTile ? .center : .leading
            )
            .background(shape.fill(theme.colors[role: appearance.background]))
            .overlay(
                shape.strokeBorder(
                    theme.colors[role: appearance.border],
                    lineWidth: MonarchRadioCardGroupTokens.borderWidth(isSelected: isSelected && isEnabled)
                )
            )
            .contentShape(shape)
            .monarchAnimation(.fast, value: configuration.isPressed)
            .monarchAnimation(.fast, value: isSelected)
    }
}

#if DEBUG
    #Preview("MonarchRadioCardGroup") {
        MonarchPreviewMatrix {
            MonarchRadioCardGroup(
                "Plan",
                selection: .constant("team"),
                options: [
                    MonarchRadioCardOption("free", title: "Starter", description: "For one person.", meta: "$0"),
                    MonarchRadioCardOption(
                        "team",
                        title: "Team",
                        description: "Up to 10 people.",
                        systemImage: "person.2",
                        meta: "$20"
                    ),
                    MonarchRadioCardOption("business", title: "Business", meta: "$60", isDisabled: true),
                ]
            )
        }
    }
#endif
