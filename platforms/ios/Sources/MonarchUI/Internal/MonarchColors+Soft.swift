import MonarchTokens
import SwiftUI

extension MonarchColors {
    /// `role` moved `amount` of the way towards the surface (or another role),
    /// channel by channel: a soft fill for a chart slab or a status block, not
    /// a saturated one (PREMIUM rule 8). Opaque, so it never dims with
    /// transparency.
    func soft(_ role: MonarchColorRole, by amount: Double, toward: MonarchColorRole = \.bgSurface) -> Color {
        Color(monarch: MonarchHeatmapTokens.mix(tokens[keyPath: toward], into: tokens[keyPath: role], amount: amount))
    }
}
