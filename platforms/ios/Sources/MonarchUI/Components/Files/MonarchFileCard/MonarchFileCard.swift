import SwiftUI

/// One file as a card or a row: a thumbnail or the type icon, the name, the
/// type and size, optional badges, an optional selection checkbox and an
/// optional menu of actions.
///
/// ```swift
/// MonarchFileCard(
///     "Q3 report.pdf",
///     isSelected: $isSelected,
///     size: 2_400_000,
///     meta: "Edited yesterday",
///     onOpen: { preview(file) },
///     menu: {
///         Button("Share", systemImage: "square.and.arrow.up") { share(file) }
///         Button("Delete", systemImage: "trash", role: .destructive) { delete(file) }
///     }
/// )
/// ```
///
/// Use the card layout in a `LazyVGrid` and the row layout in a list. The
/// app opens the file, for example with Quick Look, from `onOpen`.
///
/// Accessibility: the preview, name and meta line are one element whose
/// label is the file name and whose value is the type, size, meta and
/// badges. With `onOpen` it is a button. The checkbox ("Select Q3
/// report.pdf") and the menu ("Actions for Q3 report.pdf") are separate
/// controls with 44-point touch targets. Selection is also shown by the
/// check mark and a thicker border, not by colour alone. The thumbnail and
/// the type icon are decorative, because the meta line says the type.
public struct MonarchFileCard<MenuItems: View>: View {
    private let name: String
    private let isSelected: Binding<Bool>?
    private let size: Int64?
    private let contentType: String?
    private let meta: String?
    private let thumbnail: Image?
    private let thumbnailURL: URL?
    private let badges: [String]
    private let layout: MonarchFileCardLayout
    private let labels: MonarchFileCardLabels
    private let onOpen: (@MainActor () -> Void)?
    private let menu: MenuItems
    private let hasMenu: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.locale) private var locale
    @ScaledMetric(relativeTo: .body) private var rowPreviewSize = MonarchFileCardTokens.rowPreviewSize

    /// A file card with a local thumbnail and a menu of actions.
    ///
    /// - Parameters:
    ///   - name: The file name with its extension.
    ///   - isSelected: Shows a selection checkbox bound to this value.
    ///   - size: The size in bytes, shown in the locale's units.
    ///   - contentType: The MIME type; with the name it decides the icon and the type.
    ///   - meta: More text after the size, already localised, such as "Edited yesterday".
    ///   - thumbnail: A preview image. The type icon is shown without one.
    ///   - badges: Short states shown under the name, such as "Shared".
    ///   - layout: A card for grids or a row for lists.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Opens the file. Makes the card a button.
    ///   - menu: The items of the actions menu: buttons, toggles, sections and dividers.
    public init(
        _ name: String,
        isSelected: Binding<Bool>? = nil,
        size: Int64? = nil,
        contentType: String? = nil,
        meta: String? = nil,
        thumbnail: Image? = nil,
        badges: [String] = [],
        layout: MonarchFileCardLayout = .card,
        labels: MonarchFileCardLabels = MonarchFileCardLabels(),
        onOpen: (@MainActor () -> Void)? = nil,
        @ViewBuilder menu: () -> MenuItems
    ) {
        self.init(
            name,
            isSelected: isSelected,
            size: size,
            contentType: contentType,
            meta: meta,
            image: thumbnail,
            url: nil,
            badges: badges,
            layout: layout,
            labels: labels,
            onOpen: onOpen,
            menu: menu(),
            hasMenu: true
        )
    }

    /// A file card with a thumbnail loaded from a URL and a menu of actions.
    ///
    /// - Parameters:
    ///   - name: The file name with its extension.
    ///   - isSelected: Shows a selection checkbox bound to this value.
    ///   - size: The size in bytes, shown in the locale's units.
    ///   - contentType: The MIME type; with the name it decides the icon and the type.
    ///   - meta: More text after the size, already localised, such as "Edited yesterday".
    ///   - thumbnailURL: Where the preview image is. The type icon is shown while it loads, if it
    ///     fails and without one.
    ///   - badges: Short states shown under the name, such as "Shared".
    ///   - layout: A card for grids or a row for lists.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Opens the file. Makes the card a button.
    ///   - menu: The items of the actions menu: buttons, toggles, sections and dividers.
    public init(
        _ name: String,
        isSelected: Binding<Bool>? = nil,
        size: Int64? = nil,
        contentType: String? = nil,
        meta: String? = nil,
        thumbnailURL: URL?,
        badges: [String] = [],
        layout: MonarchFileCardLayout = .card,
        labels: MonarchFileCardLabels = MonarchFileCardLabels(),
        onOpen: (@MainActor () -> Void)? = nil,
        @ViewBuilder menu: () -> MenuItems
    ) {
        self.init(
            name,
            isSelected: isSelected,
            size: size,
            contentType: contentType,
            meta: meta,
            image: nil,
            url: thumbnailURL,
            badges: badges,
            layout: layout,
            labels: labels,
            onOpen: onOpen,
            menu: menu(),
            hasMenu: true
        )
    }

    init(
        _ name: String,
        isSelected: Binding<Bool>?,
        size: Int64?,
        contentType: String?,
        meta: String?,
        image: Image?,
        url: URL?,
        badges: [String],
        layout: MonarchFileCardLayout,
        labels: MonarchFileCardLabels,
        onOpen: (@MainActor () -> Void)?,
        menu: MenuItems,
        hasMenu: Bool
    ) {
        self.name = name
        self.isSelected = isSelected
        self.size = size
        self.contentType = contentType
        self.meta = meta
        self.thumbnail = image
        self.thumbnailURL = url
        self.badges = badges
        self.layout = layout
        self.labels = labels
        self.onOpen = onOpen
        self.menu = menu
        self.hasMenu = hasMenu
    }

    private var family: MonarchFileTypeFamily {
        MonarchFileTypeFamily(fileName: name, contentType: contentType)
    }

    private var metaParts: [String] {
        MonarchFileCardTokens.metaParts(
            family: family,
            size: size,
            meta: meta,
            typeLabel: labels.type,
            locale: locale
        )
    }

    private var selected: Bool { isSelected?.wrappedValue ?? false }

    /// The content of the view.
    public var body: some View {
        switch layout {
        case .card:
            openable(cardContent)
                .overlay(alignment: .topLeading) {
                    checkbox.padding(MonarchFileCardTokens.cornerInset)
                }
                .overlay(alignment: .topTrailing) {
                    actionsMenu.padding(MonarchFileCardTokens.cornerInset)
                }
        case .row:
            HStack(spacing: MonarchFileCardTokens.rowSpacing) {
                checkbox
                openable(rowContent)
                actionsMenu
            }
            .padding(.horizontal, MonarchFileCardTokens.padding)
            .background(surfaceShape.fill(theme.colors[role: staticAppearance.background]))
            .overlay(surfaceBorder(staticAppearance))
        }
    }

    // MARK: Parts

    private var cardContent: some View {
        VStack(alignment: .leading, spacing: MonarchFileCardTokens.cardSpacing) {
            preview
                .frame(maxWidth: .infinity)
                .aspectRatio(MonarchFileCardTokens.previewAspectRatio, contentMode: .fit)
                .clipShape(previewShape)
            text
            badgeRow
        }
        .padding(MonarchFileCardTokens.padding)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var rowContent: some View {
        HStack(spacing: MonarchFileCardTokens.rowSpacing) {
            preview
                .frame(width: rowPreviewSize, height: rowPreviewSize)
                .clipShape(previewShape)
            VStack(alignment: .leading, spacing: MonarchFileCardTokens.textSpacing) {
                text
                badgeRow
            }
        }
        .padding(.vertical, MonarchFileCardTokens.padding)
        .frame(maxWidth: .infinity, minHeight: MonarchAccessibility.minimumHitTarget, alignment: .leading)
    }

    @ViewBuilder private var preview: some View {
        let placeholder = ZStack {
            previewShape.fill(theme.colors.bgSubtle)
            MonarchFileTypeIcon(family, size: layout == .card ? .lg : .md, isDecorative: true)
        }
        Group {
            if let thumbnailURL {
                MonarchRemoteImage(url: thumbnailURL) { placeholder }
            } else {
                MonarchRemoteImage(image: thumbnail) { placeholder }
            }
        }
        .accessibilityHidden(true)
    }

    private var text: some View {
        VStack(alignment: .leading, spacing: MonarchFileCardTokens.textSpacing) {
            Text(name)
                .monarchTextStyle(.label)
                .lineLimit(layout == .card ? 2 : 1)
                .truncationMode(.middle)
                .foregroundStyle(theme.colors[role: MonarchFileCardTokens.nameRole(isEnabled: isEnabled)])
            Text(MonarchFileCardTokens.metaLine(metaParts))
                .monarchTextStyle(.caption)
                .foregroundStyle(theme.colors[role: MonarchFileCardTokens.metaRole(isEnabled: isEnabled)])
        }
        .multilineTextAlignment(.leading)
    }

    @ViewBuilder private var badgeRow: some View {
        let shown = badges.filter { !$0.isEmpty }
        if !shown.isEmpty {
            MonarchFlowLayout(spacing: MonarchFileCardTokens.badgeSpacing) {
                ForEach(shown, id: \.self) { MonarchBadge($0) }
            }
        }
    }

    @ViewBuilder private func openable(_ content: some View) -> some View {
        let value = MonarchFileCardTokens.accessibilityValue(metaParts: metaParts, badges: badges)
        if let onOpen {
            Button(action: onOpen) { content }
                .buttonStyle(MonarchFileCardButtonStyle(layout: layout, isSelected: selected))
                .accessibilityLabel(name)
                .accessibilityValue(value)
        } else {
            Group {
                if layout == .card {
                    content
                        .background(surfaceShape.fill(theme.colors[role: staticAppearance.background]))
                        .overlay(surfaceBorder(staticAppearance))
                } else {
                    content
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(name)
            .accessibilityValue(value)
        }
    }

    @ViewBuilder private var checkbox: some View {
        if let isSelected {
            Toggle(isOn: isSelected) {
                Text(labels.select?(name) ?? MonarchFileCardStrings.select(name))
            }
            .toggleStyle(.monarchCheckbox)
            .labelsHidden()
        }
    }

    @ViewBuilder private var actionsMenu: some View {
        if hasMenu {
            Menu {
                menu
            } label: {
                MonarchIcon(systemName: MonarchFileCardTokens.menuSymbol, size: .sm)
            }
            .buttonStyle(.monarch(layout == .card ? .secondary : .ghost, size: .sm))
            .accessibilityLabel(labels.actions?(name) ?? MonarchFileCardStrings.actions(name))
        }
    }

    // MARK: Surface

    private var staticAppearance: MonarchFileCardTokens.Appearance {
        MonarchFileCardTokens.appearance(isSelected: selected, isPressed: false, isEnabled: isEnabled)
    }

    private var surfaceShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchFileCardTokens.radius, style: .continuous)
    }

    private var previewShape: RoundedRectangle {
        RoundedRectangle(cornerRadius: MonarchFileCardTokens.previewRadius, style: .continuous)
    }

    private func surfaceBorder(_ appearance: MonarchFileCardTokens.Appearance) -> some View {
        surfaceShape
            .strokeBorder(
                theme.colors[role: appearance.border],
                lineWidth: MonarchFileCardTokens.borderWidth(isSelected: selected && isEnabled)
            )
            .allowsHitTesting(false)
    }
}

extension MonarchFileCard where MenuItems == EmptyView {
    /// A file card with a local thumbnail and no menu.
    ///
    /// - Parameters:
    ///   - name: The file name with its extension.
    ///   - isSelected: Shows a selection checkbox bound to this value.
    ///   - size: The size in bytes, shown in the locale's units.
    ///   - contentType: The MIME type; with the name it decides the icon and the type.
    ///   - meta: More text after the size, already localised, such as "Edited yesterday".
    ///   - thumbnail: A preview image. The type icon is shown without one.
    ///   - badges: Short states shown under the name, such as "Shared".
    ///   - layout: A card for grids or a row for lists.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Opens the file. Makes the card a button.
    public init(
        _ name: String,
        isSelected: Binding<Bool>? = nil,
        size: Int64? = nil,
        contentType: String? = nil,
        meta: String? = nil,
        thumbnail: Image? = nil,
        badges: [String] = [],
        layout: MonarchFileCardLayout = .card,
        labels: MonarchFileCardLabels = MonarchFileCardLabels(),
        onOpen: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            name,
            isSelected: isSelected,
            size: size,
            contentType: contentType,
            meta: meta,
            image: thumbnail,
            url: nil,
            badges: badges,
            layout: layout,
            labels: labels,
            onOpen: onOpen,
            menu: EmptyView(),
            hasMenu: false
        )
    }

    /// A file card with a thumbnail loaded from a URL and no menu.
    ///
    /// - Parameters:
    ///   - name: The file name with its extension.
    ///   - isSelected: Shows a selection checkbox bound to this value.
    ///   - size: The size in bytes, shown in the locale's units.
    ///   - contentType: The MIME type; with the name it decides the icon and the type.
    ///   - meta: More text after the size, already localised, such as "Edited yesterday".
    ///   - thumbnailURL: Where the preview image is. The type icon is shown while it loads, if it
    ///     fails and without one.
    ///   - badges: Short states shown under the name, such as "Shared".
    ///   - layout: A card for grids or a row for lists.
    ///   - labels: Replacements for the built-in strings.
    ///   - onOpen: Opens the file. Makes the card a button.
    public init(
        _ name: String,
        isSelected: Binding<Bool>? = nil,
        size: Int64? = nil,
        contentType: String? = nil,
        meta: String? = nil,
        thumbnailURL: URL?,
        badges: [String] = [],
        layout: MonarchFileCardLayout = .card,
        labels: MonarchFileCardLabels = MonarchFileCardLabels(),
        onOpen: (@MainActor () -> Void)? = nil
    ) {
        self.init(
            name,
            isSelected: isSelected,
            size: size,
            contentType: contentType,
            meta: meta,
            image: nil,
            url: thumbnailURL,
            badges: badges,
            layout: layout,
            labels: labels,
            onOpen: onOpen,
            menu: EmptyView(),
            hasMenu: false
        )
    }
}

/// Draws the pressed, selected and disabled surface of an openable file card.
private struct MonarchFileCardButtonStyle: ButtonStyle {
    let layout: MonarchFileCardLayout
    let isSelected: Bool

    func makeBody(configuration: Configuration) -> some View {
        MonarchFileCardSurface(configuration: configuration, layout: layout, isSelected: isSelected)
    }
}

/// Reads the environment, which a `ButtonStyle` value cannot do itself.
private struct MonarchFileCardSurface: View {
    let configuration: ButtonStyleConfiguration
    let layout: MonarchFileCardLayout
    let isSelected: Bool

    @Environment(\.monarchTheme) private var theme
    @Environment(\.isEnabled) private var isEnabled

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: MonarchFileCardTokens.radius, style: .continuous)
        if layout == .card {
            let appearance = MonarchFileCardTokens.appearance(
                isSelected: isSelected,
                isPressed: configuration.isPressed,
                isEnabled: isEnabled
            )
            configuration.label
                .background(shape.fill(theme.colors[role: appearance.background]))
                .overlay(
                    shape.strokeBorder(
                        theme.colors[role: appearance.border],
                        lineWidth: MonarchFileCardTokens.borderWidth(isSelected: isSelected && isEnabled)
                    )
                )
                .contentShape(shape)
                .monarchAnimation(.fast, value: configuration.isPressed)
        } else {
            // A row draws its surface around the checkbox and the menu too; the pressed state tints the
            // open area only.
            configuration.label
                .background(configuration.isPressed ? theme.colors.bgSubtle : .clear)
                .contentShape(Rectangle())
                .monarchAnimation(.fast, value: configuration.isPressed)
        }
    }
}

#if DEBUG
    #Preview("MonarchFileCard") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                HStack(alignment: .top) {
                    MonarchFileCard("Q3 report.pdf", isSelected: .constant(true), size: 2_400_000, onOpen: {})
                    MonarchFileCard(
                        "Budget.xlsx",
                        size: 48_000,
                        badges: ["Shared"],
                        onOpen: {},
                        menu: { Button("Delete", role: .destructive) {} }
                    )
                }
                MonarchFileCard(
                    "notes.txt",
                    isSelected: .constant(false),
                    size: 900,
                    layout: .row,
                    onOpen: {},
                    menu: { Button("Rename") {} }
                )
                MonarchFileCard("archive.zip", layout: .row).disabled(true)
            }
        }
    }
#endif
