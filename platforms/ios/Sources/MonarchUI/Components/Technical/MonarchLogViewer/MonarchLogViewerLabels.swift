/// Replacements for the built-in strings of ``MonarchLogViewer``.
/// Every value is optional; leave one out to keep the built-in English text.
public struct MonarchLogViewerLabels: Sendable {
    /// The spoken name of the scrolling log.
    public var log: String
    /// The spoken name of the level filters.
    public var levels: String
    /// The short name of a level, shown on lines and filters.
    public var levelName: @Sendable (_ level: MonarchLogLevel) -> String
    /// The spoken name of the search field.
    public var search: String
    /// The placeholder of the search field.
    public var searchPrompt: String
    /// The spoken name of the button that empties the search field.
    public var clearSearch: String
    /// The follow toggle.
    public var follow: String
    /// The wrap toggle.
    public var wrap: String
    /// The line count under the log: "120 of 5,000 lines", or "5,000 lines" when nothing is hidden.
    public var count: @Sendable (_ shown: Int, _ total: Int) -> String
    /// The jump button while following is paused.
    public var jumpToLatest: String
    /// The jump button when lines arrived while following was paused: "3 new lines".
    public var newLines: @Sendable (_ count: Int) -> String
    /// Announced when following stops.
    public var followPaused: String
    /// Announced when following starts again.
    public var followResumed: String
    /// Shown when there are no lines at all.
    public var empty: String
    /// Shown when the filters hide every line.
    public var noMatches: String
    /// Shown while the first lines load.
    public var loading: String

    /// Labels with optional replacements.
    ///
    /// - Parameters:
    ///   - log: Replaces "Log".
    ///   - levels: Replaces "Levels".
    ///   - levelName: Replaces "Debug", "Info", "Warn" and "Error".
    ///   - search: Replaces "Search log".
    ///   - searchPrompt: Replaces "Search".
    ///   - clearSearch: Replaces "Clear search".
    ///   - follow: Replaces "Follow".
    ///   - wrap: Replaces "Wrap lines".
    ///   - count: Replaces "… of … lines" and "… lines".
    ///   - jumpToLatest: Replaces "Jump to latest".
    ///   - newLines: Replaces "… new lines".
    ///   - followPaused: Replaces "Following paused".
    ///   - followResumed: Replaces "Following new lines".
    ///   - empty: Replaces "No log lines yet".
    ///   - noMatches: Replaces "No lines match the filters".
    ///   - loading: Replaces "Loading log".
    public init(
        log: String? = nil,
        levels: String? = nil,
        levelName: (@Sendable (_ level: MonarchLogLevel) -> String)? = nil,
        search: String? = nil,
        searchPrompt: String? = nil,
        clearSearch: String? = nil,
        follow: String? = nil,
        wrap: String? = nil,
        count: (@Sendable (_ shown: Int, _ total: Int) -> String)? = nil,
        jumpToLatest: String? = nil,
        newLines: (@Sendable (_ count: Int) -> String)? = nil,
        followPaused: String? = nil,
        followResumed: String? = nil,
        empty: String? = nil,
        noMatches: String? = nil,
        loading: String? = nil
    ) {
        self.log = log ?? MonarchLogViewerStrings.log
        self.levels = levels ?? MonarchLogViewerStrings.levels
        self.levelName = levelName ?? { MonarchLogViewerStrings.levelName($0) }
        self.search = search ?? MonarchLogViewerStrings.search
        self.searchPrompt = searchPrompt ?? MonarchLogViewerStrings.searchPrompt
        self.clearSearch = clearSearch ?? MonarchLogViewerStrings.clearSearch
        self.follow = follow ?? MonarchLogViewerStrings.follow
        self.wrap = wrap ?? MonarchLogViewerStrings.wrap
        self.count = count ?? { MonarchLogViewerStrings.count(shown: $0, total: $1) }
        self.jumpToLatest = jumpToLatest ?? MonarchLogViewerStrings.jumpToLatest
        self.newLines = newLines ?? { MonarchLogViewerStrings.newLines($0) }
        self.followPaused = followPaused ?? MonarchLogViewerStrings.followPaused
        self.followResumed = followResumed ?? MonarchLogViewerStrings.followResumed
        self.empty = empty ?? MonarchLogViewerStrings.empty
        self.noMatches = noMatches ?? MonarchLogViewerStrings.noMatches
        self.loading = loading ?? MonarchLogViewerStrings.loading
    }
}
