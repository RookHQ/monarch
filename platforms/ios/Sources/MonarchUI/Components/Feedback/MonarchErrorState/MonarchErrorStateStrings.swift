/// Built-in strings of the error state, from the `MonarchErrorState` strings table.
enum MonarchErrorStateStrings {
    private static let table = "MonarchErrorState"

    /// The title of the retry button.
    static var retry: String { MonarchStrings.string("monarch.errorState.retry", table: table) }
    /// Spoken as the value of the retry button while a retry is in progress.
    static var retrying: String { MonarchStrings.string("monarch.errorState.retrying", table: table) }
    /// The text before the request id.
    static var requestID: String { MonarchStrings.string("monarch.errorState.requestID", table: table) }
}
