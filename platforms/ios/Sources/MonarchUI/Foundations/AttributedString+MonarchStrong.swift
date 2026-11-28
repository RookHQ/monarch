import SwiftUI

extension AttributedString {
    /// Gives every strongly emphasised run the semibold face of the theme's custom family.
    ///
    /// The system draws strong emphasis as bold only for the system font; a custom family needs its own
    /// semibold face. Without custom faces the text is left to the system.
    func monarchStrongFaces(theme: MonarchTheme, style: MonarchTextStyle) -> AttributedString {
        guard let face = theme.typography.face(for: .semibold) else { return self }
        let spec = style.spec
        var result = self
        for run in result.runs where run.inlinePresentationIntent?.contains(.stronglyEmphasized) == true {
            result[run.range].font = .custom(face, size: spec.size, relativeTo: spec.relativeTo)
        }
        return result
    }
}
