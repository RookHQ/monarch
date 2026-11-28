import MonarchUI
import SwiftUI

struct MonarchReadReceiptSection: CataloguePage {
    static let title = "Read receipt"

    @State private var count = 3
    @State private var display = MonarchReadReceiptDisplay.text
    @State private var showsReaders = false

    private let people = ["Ana Silva", "Ben Okafor", "Chen Wei", "Dara Kim", "Eli Roth"].map {
        MonarchReadReceiptReader(name: $0)
    }

    var body: some View {
        Picker("Display", selection: $display) {
            Text("Text").tag(MonarchReadReceiptDisplay.text)
            Text("Count").tag(MonarchReadReceiptDisplay.count)
            Text("Avatars").tag(MonarchReadReceiptDisplay.avatars)
        }
        .pickerStyle(.segmented)
        Stepper("Readers: \(count)", value: $count, in: 0...people.count)

        VStack(alignment: .trailing) {
            Text("See you at ten.").monarchTextStyle(.body)
            MonarchReadReceipt(Array(people.prefix(count)), display: display) { showsReaders = true }
        }
        .frame(maxWidth: .infinity, alignment: .trailing)
        .sheet(isPresented: $showsReaders) {
            NavigationStack {
                List(people.prefix(count)) { Text($0.name) }
                    .navigationTitle("Seen by")
            }
            .presentationDetents([.medium])
        }

        CatalogueCaption("Plain text, not a button")
        MonarchReadReceipt(Array(people.prefix(2)), display: .count)
    }
}
