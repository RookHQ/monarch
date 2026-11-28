/// The font families a theme uses.
///
/// The default is the system typeface (San Francisco). A product that ships
/// its own font registers it with the app and passes the family name here.
/// Monarch bundles no font files; the optional `MonarchFontsPoppins` product
/// ships Poppins and `MonarchTypography.poppins`.
public struct MonarchTypography: Sendable, Hashable {
    /// The family for every role except `code`, or `nil` for the system font.
    public let fontFamily: String?
    /// The family for the `code` role, or `nil` for the system monospaced font.
    public let codeFontFamily: String?
    /// PostScript names of the faces of `fontFamily`, by CSS weight (400, 500, 600, 700). Empty when the
    /// family is selected by name and weight. For a typeface whose weights are separate families, such as
    /// Poppins Medium.
    public let faces: [Int: String]

    /// Typography with optional custom families.
    public init(fontFamily: String? = nil, codeFontFamily: String? = nil, faces: [Int: String] = [:]) {
        self.fontFamily = fontFamily
        self.codeFontFamily = codeFontFamily
        self.faces = faces
    }

    /// The system typeface.
    public static let system = MonarchTypography()
}
