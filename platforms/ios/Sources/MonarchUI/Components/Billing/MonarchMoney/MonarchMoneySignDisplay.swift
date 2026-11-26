/// When an amount shows a plus or minus sign.
public enum MonarchMoneySignDisplay: Sendable, Hashable, CaseIterable {
    /// A minus sign on negative amounts only.
    case automatic
    /// A sign on every amount, including zero.
    case always
    /// A sign on every amount except zero, so gains read +$42.00.
    case exceptZero
    /// No sign at all.
    case never
}
