/// Replacements for the built-in strings of ``MonarchIncidentTimeline``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchIncidentTimelineLabels: Sendable {
    /// The spoken name of the list. Built in: "Incident updates".
    public var list: String?
    /// The tag on system-posted updates. Built in: "Automated".
    public var automated: String?
    /// Shown when there are no updates. Built in: "No updates posted yet".
    public var empty: String?
    /// The label of the investigating stage. Built in: "Investigating".
    public var investigating: String?
    /// The label of the identified stage. Built in: "Identified".
    public var identified: String?
    /// The label of the monitoring stage. Built in: "Monitoring".
    public var monitoring: String?
    /// The label of the resolved stage. Built in: "Resolved".
    public var resolved: String?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        list: String? = nil,
        automated: String? = nil,
        empty: String? = nil,
        investigating: String? = nil,
        identified: String? = nil,
        monitoring: String? = nil,
        resolved: String? = nil
    ) {
        self.list = list
        self.automated = automated
        self.empty = empty
        self.investigating = investigating
        self.identified = identified
        self.monitoring = monitoring
        self.resolved = resolved
    }

    var resolvedList: String { list ?? MonarchIncidentTimelineStrings.string("list") }
    var resolvedAutomated: String { automated ?? MonarchIncidentTimelineStrings.string("automated") }
    var resolvedEmpty: String { empty ?? MonarchIncidentTimelineStrings.string("empty") }

    /// The label of a standard stage.
    func stageLabel(_ stage: MonarchIncidentStage) -> String {
        let custom: String? =
            switch stage {
            case .investigating: investigating
            case .identified: identified
            case .monitoring: monitoring
            case .resolved: resolved
            }
        return custom ?? MonarchIncidentTimelineStrings.string(stage.rawValue)
    }
}
