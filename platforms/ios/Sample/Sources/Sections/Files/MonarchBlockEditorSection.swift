@_spi(Experimental) import MonarchUI
import SwiftUI

struct MonarchBlockEditorSection: CataloguePage {
    static let title = "Block editor"

    @State private var blocks = [
        MonarchBlock(kind: .heading(1), text: "Launch plan"),
        MonarchBlock(text: "Type / at the start of a word for blocks, or # and a space for a heading."),
        MonarchBlock(kind: .taskList, text: "Write the changelog", isChecked: true),
        MonarchBlock(kind: .taskList, text: "Record the demo"),
        MonarchBlock(kind: .orderedList, text: "Freeze the build"),
        MonarchBlock(kind: .orderedList, text: "Submit for review"),
        MonarchBlock(kind: .callout(.warning), text: "The store review can take two days."),
        MonarchBlock(kind: .toggle, text: "Rollback steps", detail: "Revert the release tag and redeploy."),
        MonarchBlock(
            kind: .table,
            table: MonarchTableBlockData(rows: [["Platform", "Status"], ["iOS", "Ready"], ["Android", "In review"]])
        ),
        MonarchBlock(kind: .codeBlock, text: "make release"),
    ]
    @State private var inserted = "nothing"

    var body: some View {
        CatalogueCaption("Editable. Swipe a block, touch and hold to drag, or use its action button.")
        MonarchBlockEditor(
            "Launch plan",
            blocks: $blocks,
            slashItems: [
                MonarchSlashMenuItem(
                    id: "today",
                    label: "Today's date",
                    description: "A product item",
                    systemImage: "calendar",
                    group: "Insert"
                )
            ],
            onSlashItem: { item, _ in inserted = item.label }
        )
        .frame(height: 560)
        Text("Last product item: \(inserted). Blocks: \(blocks.count).")
            .monarchTextStyle(.caption)

        CatalogueCaption("Read-only")
        MonarchBlockEditor("Published plan", blocks: .constant(blocks), isReadOnly: true)
            .frame(height: 360)
    }
}
