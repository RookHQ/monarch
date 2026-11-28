import MonarchUI
import SwiftUI

struct MonarchFileTypeIconSection: CataloguePage {
    static let title = "File type icon"

    @State private var fileName = "report.pdf"

    private let names = [
        "holiday.heic", "launch.mov", "voice-memo.m4a", "contract.pdf", "letter.docx", "budget.xlsx",
        "pitch.key", "backup.zip", "App.swift", "notes.txt", "unknown",
    ]

    var body: some View {
        CatalogueCaption("Sizes")
        HStack {
            ForEach(MonarchFileTypeIconSize.allCases, id: \.self) { size in
                MonarchFileTypeIcon(.spreadsheet, size: size)
            }
        }

        CatalogueCaption("From a file name")
        MonarchTextField("File name", text: $fileName, helper: "Type a name to see its icon.")
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
        HStack {
            MonarchFileTypeIcon(fileName: fileName, size: .lg)
            Text(String(describing: MonarchFileTypeFamily(fileName: fileName))).monarchTextStyle(.body)
        }

        CatalogueCaption("Every family")
        ForEach(names, id: \.self) { name in
            HStack {
                MonarchFileTypeIcon(fileName: name, isDecorative: true)
                Text(name).monarchTextStyle(.body)
            }
        }
    }
}
