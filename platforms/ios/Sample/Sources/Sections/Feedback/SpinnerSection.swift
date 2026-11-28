import MonarchUI
import SwiftUI

/// Native adoption: the standard circular `ProgressView`, tinted by the theme.
struct SpinnerSection: CataloguePage {
    static let title = "Spinner (standard ProgressView)"

    var body: some View {
        ProgressView()
            .accessibilityLabel("Loading")
        ProgressView()
            .controlSize(.large)
            .accessibilityLabel("Loading, large")
    }
}
