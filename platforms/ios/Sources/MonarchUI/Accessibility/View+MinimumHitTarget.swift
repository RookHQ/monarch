import SwiftUI

extension View {
    /// Makes the touch target at least 44 by 44 points.
    ///
    /// The view keeps its visible size and is centred in the larger frame,
    /// and the whole frame responds to touch. Apply it to the label of a
    /// control, inside the control, so the control's hit area grows with it.
    public func monarchMinimumHitTarget() -> some View {
        frame(
            minWidth: MonarchAccessibility.minimumHitTarget,
            minHeight: MonarchAccessibility.minimumHitTarget
        )
        .contentShape(Rectangle())
    }
}
