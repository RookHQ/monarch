/// How ``MonarchWorldMap`` shows values.
public enum MonarchWorldMapVariant: Sendable, Hashable, CaseIterable {
    /// Each country is filled with one of five steps of the data colour.
    case choropleth
    /// A circle sized by value on each country, over a neutral map.
    case bubble
}
