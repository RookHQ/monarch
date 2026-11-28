import MonarchTokens
import SwiftUI

extension Color {
    /// A SwiftUI colour from a Monarch token colour, in the sRGB colour space.
    public init(monarch rgba: MonarchRGBA) {
        self.init(.sRGB, red: rgba.red, green: rgba.green, blue: rgba.blue, opacity: rgba.alpha)
    }
}
