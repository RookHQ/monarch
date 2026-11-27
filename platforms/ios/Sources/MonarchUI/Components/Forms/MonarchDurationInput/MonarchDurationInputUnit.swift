/// The largest unit ``MonarchDurationInput`` writes a duration in.
public enum MonarchDurationInputUnit: Sendable, Hashable, CaseIterable {
    /// Hours and minutes: 26 hours is "26h".
    case hours
    /// Days, hours and minutes: 26 hours is "1d 2h" with 24-hour days.
    case days
}
