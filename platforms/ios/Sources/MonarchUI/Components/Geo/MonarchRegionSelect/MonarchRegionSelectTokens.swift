import Foundation
import MonarchTokens
import SwiftUI

/// Built-in lists and state logic for ``MonarchRegionSelect``.
enum MonarchRegionSelectTokens {
    /// The gap between the field and the load error under it.
    static let retrySpacing: CGFloat = MonarchTokens.Space.space2

    /// What the picker shows.
    enum Source: Equatable {
        /// No country is chosen: a disabled field.
        case noCountry
        /// The loader is running.
        case loading
        /// The loader failed: the field with a retry.
        case failed
        /// A list of regions to choose from.
        case list([MonarchRegion])
        /// A text field, because the country has no list.
        case textEntry
    }

    /// The compact built-in lists, keyed by ISO 3166-1 code: US (states and
    /// DC), CA, AU, GB (nations) and IN, with English names and ISO 3166-2
    /// codes. The same lists as the web.
    static let builtIn: [String: [MonarchRegion]] = [
        "US": list(
            "US",
            "AL:Alabama;AK:Alaska;AZ:Arizona;AR:Arkansas;CA:California;CO:Colorado;CT:Connecticut;"
                + "DE:Delaware;DC:District of Columbia;FL:Florida;GA:Georgia;HI:Hawaii;ID:Idaho;IL:Illinois;"
                + "IN:Indiana;IA:Iowa;KS:Kansas;KY:Kentucky;LA:Louisiana;ME:Maine;MD:Maryland;"
                + "MA:Massachusetts;MI:Michigan;MN:Minnesota;MS:Mississippi;MO:Missouri;MT:Montana;"
                + "NE:Nebraska;NV:Nevada;NH:New Hampshire;NJ:New Jersey;NM:New Mexico;NY:New York;"
                + "NC:North Carolina;ND:North Dakota;OH:Ohio;OK:Oklahoma;OR:Oregon;PA:Pennsylvania;"
                + "RI:Rhode Island;SC:South Carolina;SD:South Dakota;TN:Tennessee;TX:Texas;UT:Utah;"
                + "VT:Vermont;VA:Virginia;WA:Washington;WV:West Virginia;WI:Wisconsin;WY:Wyoming"
        ),
        "CA": list(
            "CA",
            "AB:Alberta;BC:British Columbia;MB:Manitoba;NB:New Brunswick;NL:Newfoundland and Labrador;"
                + "NS:Nova Scotia;NT:Northwest Territories;NU:Nunavut;ON:Ontario;PE:Prince Edward Island;"
                + "QC:Quebec;SK:Saskatchewan;YT:Yukon"
        ),
        "AU": list(
            "AU",
            "ACT:Australian Capital Territory;NSW:New South Wales;NT:Northern Territory;QLD:Queensland;"
                + "SA:South Australia;TAS:Tasmania;VIC:Victoria;WA:Western Australia"
        ),
        "GB": list("GB", "ENG:England;NIR:Northern Ireland;SCT:Scotland;WLS:Wales"),
        "IN": list(
            "IN",
            "AN:Andaman and Nicobar Islands;AP:Andhra Pradesh;AR:Arunachal Pradesh;AS:Assam;BR:Bihar;"
                + "CH:Chandigarh;CG:Chhattisgarh;DH:Dadra and Nagar Haveli and Daman and Diu;DL:Delhi;GA:Goa;"
                + "GJ:Gujarat;HR:Haryana;HP:Himachal Pradesh;JK:Jammu and Kashmir;JH:Jharkhand;KA:Karnataka;"
                + "KL:Kerala;LA:Ladakh;LD:Lakshadweep;MP:Madhya Pradesh;MH:Maharashtra;MN:Manipur;"
                + "ML:Meghalaya;MZ:Mizoram;NL:Nagaland;OD:Odisha;PY:Puducherry;PB:Punjab;RJ:Rajasthan;"
                + "SK:Sikkim;TN:Tamil Nadu;TS:Telangana;TR:Tripura;UP:Uttar Pradesh;UK:Uttarakhand;"
                + "WB:West Bengal"
        ),
    ]

    /// Parses "CODE:Name;CODE:Name" into regions with the country prefix.
    private static func list(_ prefix: String, _ entries: String) -> [MonarchRegion] {
        entries.split(separator: ";").map { entry in
            let parts = entry.split(separator: ":", maxSplits: 1)
            return MonarchRegion(code: "\(prefix)-\(parts[0])", name: parts.count > 1 ? String(parts[1]) : "")
        }
    }

    /// What the picker shows. Supplied `regions` win, then the loader, then
    /// the built-in lists; an empty list becomes a text field when allowed.
    static func source(
        country: String?,
        regions: [MonarchRegion]?,
        hasLoader: Bool,
        loaded: [MonarchRegion]?,
        hasFailed: Bool,
        allowsTextEntry: Bool
    ) -> Source {
        guard let country = MonarchGeoSearch.normalized(country) else { return .noCountry }
        let list: [MonarchRegion]
        if let regions {
            list = regions
        } else if hasLoader {
            if let loaded {
                list = loaded
            } else {
                return hasFailed ? .failed : .loading
            }
        } else {
            list = builtIn[country] ?? []
        }
        return list.isEmpty && allowsTextEntry ? .textEntry : .list(list)
    }

    /// The region's code without its country prefix: "US-CA" gives "CA".
    static func shortCode(_ code: String) -> String {
        guard let dash = code.firstIndex(of: "-"), code.distance(from: code.startIndex, to: dash) == 2 else {
            return code
        }
        return String(code[code.index(after: dash)...])
    }

    /// The combobox options of a list of regions, in the list's order.
    static func options(_ regions: [MonarchRegion], showsCode: Bool) -> [MonarchComboboxOption] {
        regions.map {
            MonarchComboboxOption(
                id: $0.code,
                label: $0.name,
                hint: showsCode ? shortCode($0.code) : nil,
                keywords: $0.code
            )
        }
    }

    /// The text field's value for a typed region: an empty field is no region.
    static func typedValue(_ text: String) -> String? {
        text.isEmpty ? nil : text
    }
}
