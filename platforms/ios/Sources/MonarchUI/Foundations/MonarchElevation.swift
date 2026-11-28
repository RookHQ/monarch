import MonarchTokens

/// The shadow layers of the current theme for each elevation level.
public struct MonarchElevation: Sendable, Hashable {
    /// The token shadows this value reads from.
    public let tokens: MonarchShadowTokens

    /// Wraps the shadows of one theme.
    public init(_ tokens: MonarchShadowTokens) {
        self.tokens = tokens
    }

    /// The shadow layers for a level. `none` has no layers.
    public func layers(for level: MonarchElevationLevel) -> [MonarchShadowLayer] {
        switch level {
        case .none: []
        case .xs: tokens.xs
        case .sm: tokens.sm
        case .md: tokens.md
        case .lg: tokens.lg
        case .xl: tokens.xl
        }
    }
}
