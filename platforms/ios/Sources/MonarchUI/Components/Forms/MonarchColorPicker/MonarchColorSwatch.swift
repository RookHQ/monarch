import SwiftUI

/// A named colour that ``MonarchColorPicker`` offers.
///
/// The colour is kept as lower-case `#rrggbb` text, the form the picker
/// writes to its selection and the form to store.
public struct MonarchColorSwatch: Sendable, Hashable, Identifiable {
    /// The colour as lower-case `#rrggbb`.
    public let hex: String
    /// The name read by VoiceOver, for example "Blue", already localised.
    public let name: String

    /// The identity of the swatch: its colour.
    public var id: String { hex }

    /// A swatch, or `nil` when `hex` is not a hex colour.
    ///
    /// - Parameters:
    ///   - hex: `#rgb` or `#rrggbb`, with or without the `#`, in any case.
    ///   - name: The name read by VoiceOver, already localised.
    public init?(hex: String, name: String) {
        guard let normalized = Self.normalizedHex(hex) else { return nil }
        self.hex = normalized
        self.name = name
    }

    /// The colour to draw.
    public var color: Color {
        Self.color(hex: hex) ?? Color.clear
    }

    /// Ten presets that read well on light and dark surfaces: slate, red,
    /// orange, yellow, green, teal, blue, indigo, purple and pink. The same
    /// ten are the default on the web. The names come from the picker's
    /// strings table.
    public static var defaults: [MonarchColorSwatch] {
        MonarchColorPickerTokens.defaultSwatches.compactMap { preset in
            MonarchColorSwatch(hex: preset.hex, name: MonarchColorPickerStrings.swatchName(preset.key))
        }
    }

    /// Reads `#rgb`, `#rrggbb`, `rgb` or `rrggbb` in any case, with spaces
    /// around it, and returns lower-case `#rrggbb`, or `nil` when the text
    /// is not a hex colour.
    public static func normalizedHex(_ text: String) -> String? {
        MonarchColorPickerTokens.normalizedHex(text)
    }

    /// The SwiftUI colour of hex text in sRGB, or `nil` when the text is not
    /// a hex colour.
    public static func color(hex: String) -> Color? {
        guard let components = MonarchColorPickerTokens.components(hex: hex) else { return nil }
        // Not a literal colour: the value is the app's data.
        return Color(.sRGB, red: components.red, green: components.green, blue: components.blue)
    }
}
