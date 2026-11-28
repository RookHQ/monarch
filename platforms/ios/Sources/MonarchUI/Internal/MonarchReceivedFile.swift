import CoreTransferable
import UniformTypeIdentifiers

/// A file received from the photo picker or a drop, copied so it outlives
/// the import.
struct MonarchReceivedFile: Transferable, Sendable {
    let file: MonarchFileUploadFile

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(importedContentType: .image) { received in
            Self(file: MonarchFileUploadFile(url: try MonarchFileUploadTokens.temporaryCopy(of: received.file)))
        }
        FileRepresentation(importedContentType: .movie) { received in
            Self(file: MonarchFileUploadFile(url: try MonarchFileUploadTokens.temporaryCopy(of: received.file)))
        }
        FileRepresentation(importedContentType: .item) { received in
            Self(file: MonarchFileUploadFile(url: try MonarchFileUploadTokens.temporaryCopy(of: received.file)))
        }
    }
}
