import MonarchUI
import SwiftUI

struct MonarchKeyValueSection: CataloguePage {
    static let title = "Key value"

    @State private var separator = MonarchKeyValueSeparator.dot
    @State private var lastCopied = ""

    private let items = [
        MonarchKeyValueItem("Region", value: "eu-west"),
        MonarchKeyValueItem("Plan", value: "Team"),
        MonarchKeyValueItem("ID", value: "4f2a…", copyValue: "4f2a91c0-7d1e", isMonospaced: true),
        MonarchKeyValueItem("Owner", value: nil),
    ]

    var body: some View {
        Picker("Separator", selection: $separator) {
            ForEach(MonarchKeyValueSeparator.allCases, id: \.self) { separator in
                Text(String(describing: separator)).tag(separator)
            }
        }
        .pickerStyle(.segmented)
        CatalogueCaption("Wrap, small")
        MonarchKeyValue(items, separator: separator) { lastCopied = $0 }
        if !lastCopied.isEmpty {
            Text("Copied \(lastCopied)").monarchTextStyle(.caption)
        }
        CatalogueCaption("Stack, medium")
        MonarchKeyValue(items, size: .md, layout: .stack)
    }
}
