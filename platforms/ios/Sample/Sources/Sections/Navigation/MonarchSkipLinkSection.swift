import MonarchUI
import SwiftUI

struct MonarchSkipLinkSection: CataloguePage {
    static let title = "Skip link"

    @AccessibilityFocusState private var isMainFocused: Bool

    var body: some View {
        CatalogueCaption("Always visible; with VoiceOver it moves focus to the text")
        MonarchSkipLink(target: $isMainFocused, isAlwaysVisible: true)
        Text("Main content starts here.").accessibilityFocused($isMainFocused)
    }
}
