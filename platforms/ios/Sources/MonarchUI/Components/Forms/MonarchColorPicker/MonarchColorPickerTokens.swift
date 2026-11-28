import MonarchTokens
import SwiftUI

/// Component tokens for ``MonarchColorPicker``.
enum MonarchColorPickerTokens {
    /// The diameter of a swatch, before Dynamic Type.
    static let swatchSize: CGFloat = MonarchTokens.Control.xs
    /// The gap between a selected swatch and the ring around it.
    static let ringGap: CGFloat = MonarchTokens.Space.space0_5
    /// The width of the ring around the selected swatch.
    static let ringWidth: CGFloat = MonarchTokens.Space.space0_5
    /// The diameter of the disc behind the check mark, before Dynamic Type.
    static let checkSize: CGFloat = MonarchTokens.Space.space4
    /// The point size of the check mark, before Dynamic Type.
    static let checkGlyphSize: CGFloat = MonarchTokens.Space.space2_5
    /// The SF Symbol on the selected swatch.
    static let checkSymbol = "checkmark"
    /// The gap between swatches.
    static let gridSpacing: CGFloat = 0
    /// The least width of a grid cell, which is the touch target of a swatch.
    static let cellSize: CGFloat = 34
    /// The gap between the label, the swatches, the custom row and the helper.
    static let stackSpacing: CGFloat = MonarchTokens.Space.space2
    /// The gap between the two lines of the custom row's label.
    static let customTextSpacing: CGFloat = MonarchTokens.Space.space0_5
    /// The least height of the custom row, which is its touch target.
    static let customMinHeight: CGFloat = MonarchAccessibility.minimumHitTarget

    /// The hairline around every swatch, so a swatch close to the surface colour keeps its edge.
    static var swatchBorder: MonarchColorRole { \.borderDefault }
    /// The ring around the selected swatch.
    static var ring: MonarchColorRole { \.textDefault }
    /// The disc behind the check mark.
    static var checkBackground: MonarchColorRole { \.bgInverse }
    /// The check mark.
    static var checkGlyph: MonarchColorRole { \.textInverse }

    /// The colour of the label and the custom row's title.
    static func title(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textDefault : \.textDisabled
    }

    /// The colour of the hex value and the helper text.
    static func detail(isEnabled: Bool) -> MonarchColorRole {
        isEnabled ? \.textMuted : \.textDisabled
    }

    /// The default presets: the key of the name in the strings table and the colour.
    static let defaultSwatches: [(key: String, hex: String)] = [
        ("slate", "#64748b"), ("red", "#dc2626"), ("orange", "#ea580c"), ("yellow", "#ca8a04"),
        ("green", "#16a34a"), ("teal", "#0d9488"), ("blue", "#2563eb"), ("indigo", "#4f46e5"),
        ("purple", "#9333ea"), ("pink", "#db2777"),
    ]

    private static let hexDigits = Set("0123456789abcdef")
    private static let shortLength = 3
    private static let fullLength = 6
    private static let channelMax: Double = 255
    private static let hexRadix = 16

    /// Lower-case `#rrggbb` for `#rgb`, `#rrggbb`, `rgb` or `rrggbb` in any
    /// case with spaces around it; `nil` for anything else.
    static func normalizedHex(_ text: String) -> String? {
        var value = text.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if value.hasPrefix("#") { value.removeFirst() }
        guard value.allSatisfy(hexDigits.contains) else { return nil }
        if value.count == shortLength {
            return "#" + value.map { "\($0)\($0)" }.joined()
        }
        return value.count == fullLength ? "#" + value : nil
    }

    /// The sRGB components, 0 to 1, of hex text; `nil` when it is not a hex colour.
    static func components(hex: String) -> (red: Double, green: Double, blue: Double)? {
        guard let normalized = normalizedHex(hex), let value = Int(normalized.dropFirst(), radix: hexRadix) else {
            return nil
        }
        let red = Double((value >> 16) & 0xFF) / channelMax
        let green = Double((value >> 8) & 0xFF) / channelMax
        let blue = Double(value & 0xFF) / channelMax
        return (red, green, blue)
    }

    /// Lower-case `#rrggbb` for sRGB components. Components outside 0 to 1
    /// (a wide-gamut colour) are clamped.
    static func hex(red: Double, green: Double, blue: Double) -> String {
        let channels = [red, green, blue].map { component -> String in
            let clamped = component.isFinite ? min(1, max(0, component)) : 0
            let digits = String(Int((clamped * channelMax).rounded()), radix: hexRadix)
            return digits.count == 1 ? "0" + digits : digits
        }
        return "#" + channels.joined()
    }

    /// Lower-case `#rrggbb` for a SwiftUI colour, as the system colour picker returns it.
    static func hex(of color: Color, in environment: EnvironmentValues) -> String {
        let resolved = color.resolve(in: environment)
        return hex(red: Double(resolved.red), green: Double(resolved.green), blue: Double(resolved.blue))
    }

    /// The selection as lower-case `#rrggbb`, or `nil` when there is none or it is not a hex colour.
    static func selectedHex(_ selection: String?) -> String? {
        selection.flatMap(normalizedHex)
    }

    /// What VoiceOver reads for a swatch: "Blue, #2563eb".
    static func swatchLabel(_ swatch: MonarchColorSwatch) -> String {
        MonarchColorPickerStrings.swatchLabel(name: swatch.name, hex: swatch.hex)
    }

    /// The second line of the custom row: the selected colour as hex text, or "No colour selected".
    static func valueText(selection: String?, emptyLabel: String?) -> String {
        selectedHex(selection) ?? emptyLabel ?? MonarchColorPickerStrings.empty
    }
}
