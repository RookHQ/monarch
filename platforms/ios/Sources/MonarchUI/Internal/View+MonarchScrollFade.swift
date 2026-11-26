import SwiftUI

extension View {
    /// Fades the trailing edge of a horizontal scroller, so an item cut by the
    /// edge reads as "more to scroll" and not as clipped text. When the content
    /// fits, the fade only covers empty space.
    func monarchTrailingFade(width: CGFloat = 28) -> some View {
        mask(
            HStack(spacing: 0) {
                Rectangle().fill(Color.black)
                LinearGradient(colors: [.black, .clear], startPoint: .leading, endPoint: .trailing)
                    .frame(width: width)
            }
        )
    }
}
