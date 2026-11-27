import MonarchUI
import SwiftUI
import UniformTypeIdentifiers

/// Native adoption: `.dropDestination` on a file list, with MonarchFileUpload's picker as the main path.
struct DropZoneSection: CataloguePage {
    static let title = "Drop zone (dropDestination)"

    /// A dropped file, copied before the system removes its temporary file.
    struct DroppedFile: Transferable {
        let url: URL

        static var transferRepresentation: some TransferRepresentation {
            FileRepresentation(importedContentType: .item) { received in
                let copy = FileManager.default.temporaryDirectory
                    .appendingPathComponent(received.file.lastPathComponent)
                try? FileManager.default.removeItem(at: copy)
                try FileManager.default.copyItem(at: received.file, to: copy)
                return DroppedFile(url: copy)
            }
        }
    }

    @Environment(\.monarchTheme) private var theme
    @State private var files: [MonarchFileUploadFile] = []
    @State private var refused: [MonarchFileUploadRejection] = []
    @State private var isTargeted = false
    @State private var isFull = false

    private var maxFiles: Int { isFull ? files.count : 10 }

    var body: some View {
        CatalogueCaption("Options")
        Toggle("Full (no more files)", isOn: $isFull)
            .monarchTextStyle(.body)

        CatalogueCaption("A file list that accepts drops")
        MonarchFileUpload(
            allowedContentTypes: [.pdf, .image],
            maxFiles: maxFiles,
            currentCount: files.count,
            variant: .compact
        ) { accepted, _ in
            files += accepted
        }
        if !refused.isEmpty {
            MonarchInlineNotice(refused.map(\.message).joined(separator: "\n"), tone: .danger)
        }
        VStack(alignment: .leading, spacing: theme.spacing.space2) {
            ForEach(files, id: \.self) { file in
                Label {
                    Text(file.name)
                } icon: {
                    MonarchFileTypeIcon(fileName: file.name, size: .sm, isDecorative: true)
                }
            }
            Text(isTargeted ? "Drop to add" : "Drop PDFs or images here on iPad")
                .foregroundStyle(theme.colors.textMuted)
        }
        .monarchTextStyle(.body)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(theme.spacing.space3)
        .background(
            RoundedRectangle(cornerRadius: theme.radius.md)
                .fill(isTargeted ? theme.colors.accentSubtle : theme.colors.bgSurface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: theme.radius.md)
                .strokeBorder(isTargeted ? theme.colors.accentBorder : theme.colors.borderSubtle)
        )
        .dropDestination(
            for: DroppedFile.self,
            action: { dropped, _ in
                let result = MonarchFileUpload.validate(
                    dropped.map { MonarchFileUploadFile(url: $0.url) },
                    allowedContentTypes: [.pdf, .image],
                    maxFiles: maxFiles,
                    currentCount: files.count
                )
                files += result.accepted
                refused = result.rejected
                AccessibilityNotification.Announcement(
                    "\(result.accepted.count) added, \(result.rejected.count) not added"
                )
                .post()
                return !result.accepted.isEmpty
            },
            isTargeted: { isTargeted = $0 }
        )
    }
}
