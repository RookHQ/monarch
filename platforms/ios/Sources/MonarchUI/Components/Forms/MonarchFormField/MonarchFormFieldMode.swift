/// How a form field gives its label to what it holds.
public enum MonarchFormFieldMode: Sendable, Hashable, CaseIterable {
    /// One control without a label of its own. The field's label, help and
    /// error are applied to it. The default.
    case single
    /// Several controls that together give one value, such as a date and a
    /// time, or a number and a unit. Each keeps its own label; the field is
    /// a VoiceOver container named by the label, with the help, error and
    /// "Required" as its hint.
    case group
}
