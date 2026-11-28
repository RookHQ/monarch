import MonarchUI
import SwiftUI

/// Every type role.
struct TypographyPage: View {
    var body: some View {
        ForEach(MonarchTextStyle.allCases, id: \.self) { style in
            Text(String(describing: style)).monarchTextStyle(style)
        }
    }
}
