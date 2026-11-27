import SwiftUI

extension MonarchTypography {
    /// The PostScript name of the face nearest to `weight`, or `nil` when `faces` is empty.
    /// A tie goes to the heavier face.
    func face(for weight: Font.Weight) -> String? {
        guard !faces.isEmpty else { return nil }
        let target = Self.cssWeight(weight)
        return faces.min { lhs, rhs in
            let left = abs(lhs.key - target)
            let right = abs(rhs.key - target)
            return left == right ? lhs.key > rhs.key : left < right
        }?.value
    }

    static func cssWeight(_ weight: Font.Weight) -> Int {
        switch weight {
        case .ultraLight: 200
        case .thin: 100
        case .light: 300
        case .medium: 500
        case .semibold: 600
        case .bold: 700
        case .heavy: 800
        case .black: 900
        default: 400
        }
    }
}
