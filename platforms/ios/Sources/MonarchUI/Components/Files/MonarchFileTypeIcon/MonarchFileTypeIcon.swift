import SwiftUI

/// A glyph for the family of a file: image, PDF, spreadsheet, archive and so on.
///
/// The family is worked out from the file name and MIME type, or given directly.
///
/// ```swift
/// MonarchFileTypeIcon(fileName: "report.pdf")
/// MonarchFileTypeIcon(.spreadsheet, size: .lg)
/// ```
///
/// Accessibility: the icon is an image whose label is the name of the family
/// ("PDF document"), so the type does not depend on the shape or the tint.
/// Pass `isDecorative: true` when text beside it already says the type. The
/// glyph and the tile scale with Dynamic Type.
public struct MonarchFileTypeIcon: View {
    private let family: MonarchFileTypeFamily
    private let size: MonarchFileTypeIconSize
    private let label: String?
    private let isDecorative: Bool

    @Environment(\.monarchTheme) private var theme
    @ScaledMetric(relativeTo: .body) private var tileSize = MonarchFileTypeIconTokens.tileSize

    /// The icon of a family.
    ///
    /// - Parameters:
    ///   - family: The family to draw.
    ///   - size: The size of the glyph. `lg` adds a tinted tile.
    ///   - label: Replaces the built-in spoken name of the family.
    ///   - isDecorative: Whether to hide the icon from assistive technology.
    public init(
        _ family: MonarchFileTypeFamily,
        size: MonarchFileTypeIconSize = .md,
        label: String? = nil,
        isDecorative: Bool = false
    ) {
        self.family = family
        self.size = size
        self.label = label
        self.isDecorative = isDecorative
    }

    /// The icon of a file, from its name and MIME type.
    ///
    /// - Parameters:
    ///   - fileName: The file name; its extension decides the family when the MIME type does not.
    ///   - contentType: The MIME type. It takes precedence over the extension.
    ///   - size: The size of the glyph. `lg` adds a tinted tile.
    ///   - label: Replaces the built-in spoken name of the family.
    ///   - isDecorative: Whether to hide the icon from assistive technology.
    public init(
        fileName: String?,
        contentType: String? = nil,
        size: MonarchFileTypeIconSize = .md,
        label: String? = nil,
        isDecorative: Bool = false
    ) {
        self.init(
            MonarchFileTypeFamily(fileName: fileName, contentType: contentType),
            size: size,
            label: label,
            isDecorative: isDecorative
        )
    }

    /// The content of the view.
    public var body: some View {
        if isDecorative {
            mark.accessibilityHidden(true)
        } else {
            mark
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(MonarchFileTypeIconTokens.accessibilityLabel(family: family, label: label))
                .accessibilityAddTraits(.isImage)
        }
    }

    @ViewBuilder private var mark: some View {
        let pair = MonarchFileTypeIconTokens.symbols(family)
        let iconSize = MonarchFileTypeIconTokens.iconSize(size)
        let glyph = ZStack {
            MonarchIcon(systemName: pair.outline, size: iconSize)
            if let inner = pair.inner {
                MonarchIcon(systemName: inner, size: .sm)
                    .scaleEffect(size == .lg ? 0.55 : 0.5)
                    .offset(y: 1.5)
            }
        }
        .foregroundStyle(theme.colors[role: MonarchFileTypeIconTokens.color(family)])
        if MonarchFileTypeIconTokens.hasTile(size) {
            glyph
                .frame(width: tileSize, height: tileSize)
                .background(MonarchShapes.control.fill(theme.colors[role: MonarchFileTypeIconTokens.tileBackground]))
                .overlay(
                    MonarchShapes.control.strokeBorder(
                        theme.colors[role: MonarchFileTypeIconTokens.tileBorder],
                        lineWidth: MonarchShapes.borderWidth
                    )
                )
        } else {
            glyph
        }
    }
}

#if DEBUG
    #Preview("MonarchFileTypeIcon") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchFileTypeIconSize.allCases, id: \.self) { size in
                    HStack {
                        ForEach(MonarchFileTypeFamily.allCases, id: \.self) { family in
                            MonarchFileTypeIcon(family, size: size)
                        }
                    }
                }
            }
        }
    }
#endif
