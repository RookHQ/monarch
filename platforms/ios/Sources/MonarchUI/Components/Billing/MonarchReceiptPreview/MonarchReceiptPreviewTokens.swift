import Foundation
import MonarchTokens
import SwiftUI

/// Component tokens and pure logic for ``MonarchReceiptPreview``.
enum MonarchReceiptPreviewTokens {
    /// The SF Symbol before the button title.
    static let symbol = "paperclip"
    /// The gap between the button and an error under it.
    static let errorSpacing: CGFloat = MonarchTokens.Space.space2

    /// What pressing the button does.
    enum Action: Equatable {
        /// Preview this local file now.
        case preview(URL)
        /// Run the app's loader, then preview the file it returns.
        case load
        /// Nothing: there is no receipt.
        case none
    }

    /// The action for the sources the app gave. A local file is previewed
    /// directly; a loader is run only when the button is pressed. A URL that
    /// is not a file cannot be previewed by Quick Look and counts as none.
    static func action(url: URL?, hasLoader: Bool) -> Action {
        if hasLoader { return .load }
        if let url, url.isFileURL { return .preview(url) }
        return .none
    }

    /// The file to hand back to the app when the preview closes: the one
    /// that was showing, once the preview is dismissed.
    static func closedFile(old: URL?, new: URL?) -> URL? {
        new == nil ? old : nil
    }
}
