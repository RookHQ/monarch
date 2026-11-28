/// Namespace for facts about the MonarchUI library itself.
///
/// ## Experimental API
///
/// A declaration that is not yet stable is marked `@_spi(Experimental)` and is
/// visible only to a file that opts in:
///
/// ```swift
/// @_spi(Experimental) import MonarchUI
/// ```
///
/// Experimental declarations may change or be removed in any release, never
/// appear in the signature of a stable declaration, and are listed with
/// `"status": "experimental"` in their inventory file. They are the AI panels,
/// product screens and domain composites of the catalogue's patterns layer, and
/// a few general components that still wait for a device check.
/// `Scripts/generate.py` checks that every public declaration of an
/// experimental component is marked, and `Scripts/api.sh` that no stable
/// declaration mentions one. The API reference lists them separately.
public enum MonarchUI {
    /// The version of the library, matching `CHANGELOG.md`.
    public static let version = "0.14.5"
}
