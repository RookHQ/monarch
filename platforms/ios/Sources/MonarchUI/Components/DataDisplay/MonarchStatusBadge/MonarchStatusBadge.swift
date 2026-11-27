import SwiftUI

/// A status shown as words, a tone and a distinct icon or dot, so colour is
/// never the only signal.
///
/// ```swift
/// MonarchStatusBadge("partial")                     // "Partial outage", warning
/// MonarchStatusBadge(ticket.status.rawValue)        // from monarchStatusRegistry(_:)
/// MonarchStatusBadge("down", variant: .dot, size: .sm)
/// ```
///
/// Statuses come from a registry: the `registry` passed to the badge, then
/// those installed with `monarchStatusRegistry(_:)`, then the built-in
/// statuses. An unknown key shows the neutral "unknown" look with a readable
/// label made from the key, never the raw key.
///
/// Accessibility: the badge is one element read as its label. The icon and
/// the dot are decorative; the label carries the meaning. Text scales with
/// Dynamic Type.
public struct MonarchStatusBadge: View {
    private let status: String?
    private let definition: MonarchStatusDefinition?
    private let registry: MonarchStatusRegistry?
    private let label: String?
    private let systemImage: String?
    private let showsIcon: Bool
    private let variant: MonarchStatusBadgeVariant
    private let size: MonarchStatusBadgeSize

    @Environment(\.monarchStatusRegistry) private var installed

    /// A badge for a status key.
    ///
    /// - Parameters:
    ///   - status: The key: a built-in one or one from a registry. `nil` is unknown.
    ///   - registry: Statuses for this badge only, looked up first.
    ///   - label: Replaces the registry's label, already localised.
    ///   - systemImage: Replaces the registry's SF Symbol.
    ///   - showsIcon: Whether an icon is shown. The label still carries the meaning without it.
    ///   - variant: Tinted, outlined, filled, or a dot with plain text.
    ///   - size: The text size.
    public init(
        _ status: String?,
        registry: MonarchStatusRegistry? = nil,
        label: String? = nil,
        systemImage: String? = nil,
        showsIcon: Bool = true,
        variant: MonarchStatusBadgeVariant = .subtle,
        size: MonarchStatusBadgeSize = .md
    ) {
        self.status = status
        self.definition = nil
        self.registry = registry
        self.label = label
        self.systemImage = systemImage
        self.showsIcon = showsIcon
        self.variant = variant
        self.size = size
    }

    /// A badge for a status the app defines directly.
    ///
    /// - Parameters:
    ///   - definition: The label, tone and symbol.
    ///   - showsIcon: Whether an icon is shown.
    ///   - variant: Tinted, outlined, filled, or a dot with plain text.
    ///   - size: The text size.
    public init(
        _ definition: MonarchStatusDefinition,
        showsIcon: Bool = true,
        variant: MonarchStatusBadgeVariant = .subtle,
        size: MonarchStatusBadgeSize = .md
    ) {
        self.status = nil
        self.definition = definition
        self.registry = nil
        self.label = nil
        self.systemImage = nil
        self.showsIcon = showsIcon
        self.variant = variant
        self.size = size
    }

    private var resolved: MonarchStatusDefinition {
        definition ?? MonarchStatusBadgeTokens.resolve(status, in: [registry, installed].compactMap { $0 })
    }

    /// The content of the view.
    public var body: some View {
        let resolved = resolved
        let text = label.flatMap { $0.isEmpty ? nil : $0 } ?? resolved.label
        let symbol = MonarchStatusBadgeTokens.symbol(
            definition: resolved,
            override: systemImage,
            showsIcon: showsIcon,
            variant: variant
        )
        Group {
            if variant == .dot && symbol == nil {
                MonarchStatusDot(text, tone: resolved.tone, size: size == .sm ? .sm : .md)
            } else {
                capsule(text: text, tone: resolved.tone, symbol: symbol)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(text)
    }

    @ViewBuilder
    private func capsule(text: String, tone: MonarchTone, symbol: String?) -> some View {
        let content = HStack(spacing: MonarchStatusBadgeTokens.iconSpacing) {
            if let symbol {
                MonarchIcon(systemName: symbol, size: .sm)
            }
            Text(text)
                .monarchTextStyle(
                    MonarchStatusBadgeTokens.textStyle(size),
                    weight: MonarchStatusBadgeTokens.fontWeight
                )
        }
        if let appearance = MonarchStatusBadgeTokens.appearance(tone: tone, variant: variant) {
            content
                .padding(.horizontal, MonarchStatusBadgeTokens.horizontalPadding(size))
                .padding(.vertical, MonarchStatusBadgeTokens.verticalPadding)
                .monarchSurface(appearance, in: Capsule())
        } else {
            // The dot variant with an icon the app chose: the icon and the
            // text take the tone's strong colour, with no surface.
            DotWithIcon(content: content, tone: tone)
        }
    }
}

/// The dot variant with an explicit icon in place of the dot.
private struct DotWithIcon<Content: View>: View {
    let content: Content
    let tone: MonarchTone

    @Environment(\.monarchTheme) private var theme

    var body: some View {
        content.foregroundStyle(theme.colors[role: MonarchTonePalette.palette(for: tone).strong])
    }
}

#if DEBUG
    #Preview("MonarchStatusBadge") {
        MonarchPreviewMatrix {
            VStack(alignment: .leading) {
                ForEach(MonarchStatusBadgeVariant.allCases, id: \.self) { variant in
                    HStack {
                        MonarchStatusBadge("operational", variant: variant)
                        MonarchStatusBadge("partial", variant: variant)
                        MonarchStatusBadge("down", variant: variant, size: .sm)
                    }
                }
                MonarchStatusBadge("pending_delete")
            }
        }
    }
#endif
