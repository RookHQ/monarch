/// Replacements for the built-in strings of ``MonarchAiClaimSources``.
///
/// Every property is `nil` by default, which keeps the built-in string.
@_spi(Experimental) public struct MonarchAiClaimSourcesLabels: Sendable {
    /// The heading. Built in: "Where this came from".
    public var heading: String?
    /// The line under the heading. Built in: "5 claims: 3 supported, 1 partly, 1 without a source".
    public var summary: (@Sendable (MonarchAiClaimCounts) -> String)?
    /// The name of the filter. Built in: "Show claims".
    public var filterLabel: String?
    /// The filter option for every claim. Built in: "All".
    public var all: String?
    /// The filter option for claims that are not fully supported. Built in: "Needs checking".
    public var needsChecking: String?
    /// The support level in words, such as "Partly supported".
    public var support: (@Sendable (MonarchAiClaimSupport) -> String)?
    /// The sentence at the top of an open claim that is not fully supported.
    public var note: (@Sendable (MonarchAiClaimSupport) -> String)?
    /// The number of passages. Built in: "No passages", "1 passage", "<n> passages".
    public var passages: (@Sendable (Int) -> String)?
    /// The spoken name of a claim's passages, given the claim. Built in: "Passages for: <claim>".
    public var evidenceLabel: (@Sendable (String) -> String)?
    /// The stance of a passage. Built in: "Supports the claim", "Contradicts the claim".
    public var stance: (@Sendable (MonarchAiClaimEvidenceStance) -> String)?
    /// The title of a passage whose source is not in `sources`. Built in: "Unknown source".
    public var unknownSource: String?
    /// The publication date, given the formatted date. Built in: "Published <date>".
    public var published: (@Sendable (String) -> String)?
    /// The name of a kind of source, such as "Web page".
    public var kind: (@Sendable (MonarchAiSourceKind) -> String)?
    /// The button that opens a passage's source. Built in: "Open source".
    public var openSource: String?
    /// The spoken name of an action, given the source title, so passages can be told apart. Built in: "from <title>".
    public var fromSource: (@Sendable (String) -> String)?
    /// The button that copies a passage. Built in: "Copy quote".
    public var copyQuote: String?
    /// The copy button after copying. Built in: "Copied".
    public var copied: String?
    /// Announced after copying. Built in: "Quote copied to clipboard".
    public var copiedAnnouncement: String?
    /// The button that reports a problem with a claim. Built in: "Report a problem".
    public var flag: String?
    /// The button that checks an unchecked claim. Built in: "Check again".
    public var verify: String?
    /// Shown when there are no claims. Built in: "There are no claims to check.".
    public var empty: String?
    /// Shown when the filter has nothing to show.
    public var noneNeedChecking: String?
    /// Shown in an open claim with no passages and no note.
    public var noEvidence: String?
    /// Announced when the filter changes. Built in: "Showing <shown> of <total> claims".
    public var showing: (@Sendable (Int, Int) -> String)?

    /// Replacements for built-in strings. Pass only the ones to replace.
    public init(
        heading: String? = nil,
        summary: (@Sendable (MonarchAiClaimCounts) -> String)? = nil,
        filterLabel: String? = nil,
        all: String? = nil,
        needsChecking: String? = nil,
        support: (@Sendable (MonarchAiClaimSupport) -> String)? = nil,
        note: (@Sendable (MonarchAiClaimSupport) -> String)? = nil,
        passages: (@Sendable (Int) -> String)? = nil,
        evidenceLabel: (@Sendable (String) -> String)? = nil,
        stance: (@Sendable (MonarchAiClaimEvidenceStance) -> String)? = nil,
        unknownSource: String? = nil,
        published: (@Sendable (String) -> String)? = nil,
        kind: (@Sendable (MonarchAiSourceKind) -> String)? = nil,
        openSource: String? = nil,
        fromSource: (@Sendable (String) -> String)? = nil,
        copyQuote: String? = nil,
        copied: String? = nil,
        copiedAnnouncement: String? = nil,
        flag: String? = nil,
        verify: String? = nil,
        empty: String? = nil,
        noneNeedChecking: String? = nil,
        noEvidence: String? = nil,
        showing: (@Sendable (Int, Int) -> String)? = nil
    ) {
        self.heading = heading
        self.summary = summary
        self.filterLabel = filterLabel
        self.all = all
        self.needsChecking = needsChecking
        self.support = support
        self.note = note
        self.passages = passages
        self.evidenceLabel = evidenceLabel
        self.stance = stance
        self.unknownSource = unknownSource
        self.published = published
        self.kind = kind
        self.openSource = openSource
        self.fromSource = fromSource
        self.copyQuote = copyQuote
        self.copied = copied
        self.copiedAnnouncement = copiedAnnouncement
        self.flag = flag
        self.verify = verify
        self.empty = empty
        self.noneNeedChecking = noneNeedChecking
        self.noEvidence = noEvidence
        self.showing = showing
    }
}
