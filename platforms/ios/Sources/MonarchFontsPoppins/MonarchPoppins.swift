import CoreText
import Foundation
import MonarchUI

/// Poppins for Monarch, as an optional product.
///
/// Monarch's default is the system typeface. Add the `MonarchFontsPoppins` product to use Poppins:
///
/// ```swift
/// WindowGroup {
///     RootView().monarchTheme(typography: .poppins)
/// }
/// ```
///
/// `MonarchTypography.poppins` registers the font files with the process the first time it is read, so the
/// app needs no `UIAppFonts` entry. The font files are licensed under the SIL Open Font License 1.1; the
/// licence text is bundled as `Fonts/OFL.txt`.
public enum MonarchPoppins {
    /// The family name of the regular face.
    public static let family = "Poppins"

    /// The PostScript names of the bundled faces, by CSS weight (400, 500, 600). Poppins names each weight as
    /// its own family, so the faces are listed instead of relying on a family lookup.
    public static let faces: [Int: String] = [
        400: "Poppins-Regular",
        500: "Poppins-Medium",
        600: "Poppins-SemiBold",
    ]

    /// The bundled font files, relative to the resource bundle.
    static let fileNames = ["Poppins-Regular", "Poppins-Medium", "Poppins-SemiBold"]

    /// The licence text that ships with the fonts.
    public static var licenseURL: URL? {
        Bundle.module.url(forResource: "OFL", withExtension: "txt", subdirectory: "Fonts")
    }

    private static let registration: Bool = {
        var ok = true
        for name in fileNames {
            guard let url = Bundle.module.url(forResource: name, withExtension: "ttf", subdirectory: "Fonts") else {
                ok = false
                continue
            }
            var error: Unmanaged<CFError>?
            if !CTFontManagerRegisterFontsForURL(url as CFURL, .process, &error) {
                // An app that lists the files in UIAppFonts has registered them already.
                let code = error.map { CFErrorGetCode($0.takeRetainedValue()) }
                if code != CTFontManagerError.alreadyRegistered.rawValue as CFIndex { ok = false }
            }
        }
        return ok
    }()

    /// Registers the font files with the process. Safe to call more than once. Returns `false` when a file
    /// is missing or could not be registered.
    @discardableResult
    public static func register() -> Bool { registration }
}

extension MonarchTypography {
    /// The Monarch type roles set in Poppins. Registers the fonts on first use. The code role keeps the
    /// system monospaced font.
    public static var poppins: MonarchTypography {
        MonarchPoppins.register()
        return MonarchTypography(fontFamily: MonarchPoppins.family, faces: MonarchPoppins.faces)
    }
}
