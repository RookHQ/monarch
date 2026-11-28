import MonarchUI
import SwiftUI

struct MonarchAiResponseSkeletonSection: CataloguePage {
    static let title = "AI response skeleton"

    @State private var shape: MonarchAiResponseSkeletonShape = .text

    var body: some View {
        CatalogueCaption("Choose the shape of the expected answer")
        Picker("Shape", selection: $shape) {
            Text("Text").tag(MonarchAiResponseSkeletonShape.text)
            Text("Cards").tag(MonarchAiResponseSkeletonShape.cards)
            Text("Table").tag(MonarchAiResponseSkeletonShape.table)
        }
        .pickerStyle(.segmented)
        MonarchAiResponseSkeleton(shape: shape)

        CatalogueCaption("Table with 3 rows and 4 columns")
        MonarchAiResponseSkeleton(shape: .table, lines: 3, columns: 4)
    }
}
