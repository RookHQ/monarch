/// Built-in strings of the "Sign in with Rook ID" button, from the `MonarchSignInWithRookButton` strings table.
enum MonarchSignInWithRookButtonStrings {
    /// The default title.
    static var title: String {
        MonarchStrings.string("monarch.signInWithRookButton.title", table: "MonarchSignInWithRookButton")
    }

    /// Spoken as the value of the button while the sign-in is starting.
    static var loading: String {
        MonarchStrings.string("monarch.signInWithRookButton.loading", table: "MonarchSignInWithRookButton")
    }
}
