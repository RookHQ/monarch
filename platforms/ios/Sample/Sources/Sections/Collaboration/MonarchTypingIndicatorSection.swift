import MonarchUI
import SwiftUI

struct MonarchTypingIndicatorSection: CataloguePage {
    static let title = "Typing indicator"

    @State private var count = 1

    private let people = ["Ana", "Ben", "Chen"]

    var body: some View {
        Text("See you at ten.").monarchTextStyle(.body)
        MonarchTypingIndicator(Array(people.prefix(count)))
        Stepper("People typing: \(count)", value: $count, in: 0...people.count)

        CatalogueCaption("A sentence worded by the app")
        MonarchTypingIndicator(["Ana"], text: "Ana is writing a reply")
        CatalogueCaption("No space kept while nobody is typing")
        MonarchTypingIndicator([], reservesSpace: false)
    }
}
