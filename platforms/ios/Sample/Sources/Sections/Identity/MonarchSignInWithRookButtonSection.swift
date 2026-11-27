import MonarchUI
import SwiftUI

struct MonarchSignInWithRookButtonSection: CataloguePage {
    static let title = "Sign in with Rook ID"

    @State private var isLoading = false

    var body: some View {
        MonarchSignInWithRookButton(isLoading: isLoading, fullWidth: true) {
            isLoading = true
        }
        Toggle("Loading", isOn: $isLoading)
            .monarchTextStyle(.body)

        CatalogueCaption("Variants")
        ForEach(MonarchSignInWithRookButtonVariant.allCases, id: \.self) { variant in
            MonarchSignInWithRookButton(variant: variant) {}
        }

        CatalogueCaption("Sizes")
        ForEach(MonarchSignInWithRookButtonSize.allCases, id: \.self) { size in
            MonarchSignInWithRookButton(size: size) {}
        }

        CatalogueCaption("Disabled")
        MonarchSignInWithRookButton {}
            .disabled(true)
    }
}
