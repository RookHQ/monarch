import Foundation

/// What the app's download returns.
public enum MonarchDownloadReportResult: Sendable, Hashable {
    /// The app saved or shared the file itself.
    case downloaded
    /// The file is prepared in the background. The message replaces the
    /// built-in note.
    case queued(message: String?)
    /// The file is ready at this local URL. The button opens the system's
    /// save dialog for it.
    case file(URL)
}
