/// The kinds of question a ``MonarchFieldRenderer`` asks. The raw value is the web's type name, such as
/// `short-text`, so stored form definitions decode with `MonarchFormFieldType(rawValue:)`.
public enum MonarchFormFieldType: String, CaseIterable, Hashable, Sendable {
    /// One line of text.
    case shortText = "short-text"
    /// Several lines of text.
    case longText = "long-text"
    /// An email address, with the email keyboard.
    case email
    /// A phone number, with the phone keyboard.
    case phone
    /// An `http` or `https` web address, with the web address keyboard.
    case url
    /// A number.
    case number
    /// A day, chosen with the system date picker.
    case date
    /// A time of day, chosen with the system time picker and stored as `HH:MM`.
    case time
    /// One option from a menu.
    case select
    /// Several options, chosen in a sheet.
    case multiSelect = "multi-select"
    /// One option from a visible list.
    case radio
    /// Several options from a visible list of checkboxes.
    case checkboxes
    /// Yes or no.
    case yesNo = "yes-no"
    /// One checkbox that people tick to agree to a statement.
    case consent
    /// A rating on a scale, 1 to 5 by default.
    case rating
    /// One or more files.
    case file
    /// A heading with optional text and no answer.
    case section
}
