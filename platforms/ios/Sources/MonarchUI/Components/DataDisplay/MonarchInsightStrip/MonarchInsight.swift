/// One insight in a ``MonarchInsightStrip``.
public struct MonarchInsight: Identifiable {
    /// A stable identity, unique within a strip.
    public let id: String
    /// The insight in one line.
    public let title: String
    /// Why it was raised, in plain words.
    public let why: String
    /// How much it matters: `danger` for critical, then `warning`, `info`,
    /// `success` for good news, and `neutral`.
    public let tone: MonarchTone
    /// Where it came from.
    public let origin: MonarchInsightOrigin
    /// The figures or records behind it, shown in a collapsed section.
    public let evidence: String?
    /// The title of the follow-up action.
    public let actionTitle: String?
    /// The follow-up action.
    public let action: (@MainActor () -> Void)?

    /// An insight.
    ///
    /// - Parameters:
    ///   - id: A stable identity, unique within a strip.
    ///   - title: The insight in one line, already localised.
    ///   - why: Why it was raised, in plain words.
    ///   - tone: How much it matters.
    ///   - origin: Where it came from: `rule` for a fixed rule people
    ///     wrote, `ai` only when a model produced it.
    ///   - evidence: The figures or records behind it.
    ///   - actionTitle: The title of the follow-up action. No button without
    ///     it.
    ///   - action: Runs when the follow-up action is activated.
    public init(
        id: String,
        title: String,
        why: String,
        tone: MonarchTone = .info,
        origin: MonarchInsightOrigin,
        evidence: String? = nil,
        actionTitle: String? = nil,
        action: (@MainActor () -> Void)? = nil
    ) {
        self.id = id
        self.title = title
        self.why = why
        self.tone = tone
        self.origin = origin
        self.evidence = evidence
        self.actionTitle = actionTitle
        self.action = action
    }
}
