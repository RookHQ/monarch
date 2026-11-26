import Foundation
import Testing

@testable import MonarchTokens

/// The golden vectors written by packages/tokens/scripts/emit-brand-vectors.ts.
private struct Vectors: Decodable {
    struct Parse: Decodable {
        let input: String?
        let hex: String?
        let h: Double?
        let s: Double?
        let l: Double?
    }

    struct Report: Decodable, Equatable {
        struct Ratios: Decodable, Equatable {
            let onSolid: Double
            let strongOnCanvas: Double
            let strongOnSurface: Double
            let strongOnSubtle: Double
        }

        let base: String
        let adjustments: [String]
        let ratios: Ratios
    }

    struct Mode: Decodable, Equatable {
        let accent: [String: String]
        let report: Report
        let ai: [String: String]?
        let aiReport: Report?
    }

    struct AICheck: Decodable, Equatable {
        struct Closest: Decodable, Equatable {
            let distance: Double
            let to: String
            let theme: String
        }

        let input: String
        let color: String?
        let valid: Bool
        let closest: Closest?
        let distinct: Bool
        let warnings: [String]
    }

    struct Result: Decodable, Equatable {
        let input: String
        let brand: String
        let valid: Bool
        let modes: [String: Mode]
        let ai: AICheck?
    }

    struct Theme: Decodable {
        let brand: String
        let ai: String?
        let result: Result
    }

    let linear: [Double]
    let parse: [Parse]
    let themes: [Theme]

    static let shared: Vectors = {
        let url = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("Fixtures/brand-theme-vectors.json")
        do {
            return try JSONDecoder().decode(Vectors.self, from: Data(contentsOf: url))
        } catch {
            fatalError("Cannot read \(url.path): \(error)")
        }
    }()
}

private func hex(_ c: MonarchRGBA) -> String {
    let v = [c.red, c.green, c.blue].map { Int(($0 * 255).rounded()) }
    return String(format: "#%02X%02X%02X", v[0], v[1], v[2])
}

private func scaleMap(_ s: MonarchAccentScale) -> [String: String] {
    [
        "solid": hex(s.solid), "solidHover": hex(s.solidHover), "solidActive": hex(s.solidActive),
        "onSolid": hex(s.onSolid), "subtle": hex(s.subtle), "subtleHover": hex(s.subtleHover),
        "strong": hex(s.strong), "onStrong": hex(s.onStrong), "border": hex(s.border),
        "strongInverse": hex(s.strongInverse),
    ]
}

private func report(_ r: MonarchBrandReport) -> Vectors.Report {
    Vectors.Report(
        base: r.base,
        adjustments: r.adjustments,
        ratios: .init(
            onSolid: r.ratios.onSolid,
            strongOnCanvas: r.ratios.strongOnCanvas,
            strongOnSurface: r.ratios.strongOnSurface,
            strongOnSubtle: r.ratios.strongOnSubtle
        )
    )
}

/// The native result in the shape of the JSON, so one equality compares every value.
private func result(_ t: MonarchBrandTheme) -> Vectors.Result {
    var modes: [String: Vectors.Mode] = [:]
    for m in t.modes {
        modes[m.themeID] = Vectors.Mode(
            accent: scaleMap(m.accent),
            report: report(m.report),
            ai: m.ai.map(scaleMap),
            aiReport: m.aiReport.map(report)
        )
    }
    let ai = t.ai.map { c in
        Vectors.AICheck(
            input: c.input,
            color: c.color,
            valid: c.isValid,
            closest: c.closest.map { .init(distance: jsRound($0.distance * 1e9) / 1e9, to: $0.to, theme: $0.themeID) },
            distinct: c.isDistinct,
            warnings: c.warnings
        )
    }
    return Vectors.Result(input: t.input, brand: t.brand, valid: t.isValid, modes: modes, ai: ai)
}

@Suite("Brand themes")
struct BrandThemeTests {
    @Test("There are at least 40 vectors, with AI colours and invalid input")
    func inventory() {
        let themes = Vectors.shared.themes
        #expect(themes.count >= 40)
        #expect(themes.contains { $0.ai != nil })
        #expect(themes.contains { !$0.result.valid })
    }

    @Test("The linearised channels match the web (12 significant digits)")
    func linear() {
        #expect(Vectors.shared.linear.count == 256)
        for (i, web) in Vectors.shared.linear.enumerated() {
            #expect(abs(web - linearChannel[i]) <= 1e-11 * web + 1e-18, "channel \(i)")
        }
    }

    @Test("parseHexColor matches the web")
    func parse() {
        for c in Vectors.shared.parse {
            let parsed = parseHexColor(c.input)
            #expect(parsed?.hex == c.hex, "parse \(String(describing: c.input))")
            #expect(parsed?.h == c.h, "h of \(String(describing: c.input))")
            #expect(parsed?.s == c.s, "s of \(String(describing: c.input))")
            #expect(parsed?.l == c.l, "l of \(String(describing: c.input))")
        }
    }

    @Test("Every golden vector matches the web exactly")
    func goldenVectors() {
        for v in Vectors.shared.themes {
            #expect(
                result(MonarchBrandTheme.derive(brand: v.brand, ai: v.ai)) == v.result,
                "\(v.brand) ai \(v.ai ?? "-")"
            )
        }
    }

    @Test("The contrast guarantees hold natively")
    func contrastGuarantees() {
        let inputs = Vectors.shared.themes.map(\.brand) + Vectors.shared.themes.compactMap(\.ai)
        for input in inputs {
            let base = parseHexColor(input) ?? BrandBase.defaultAccent
            for def in BrandThemeDefinition.all {
                let i = deriveIntent(base, def)
                for fill in [i.solid, i.solidHover, i.solidActive] {
                    #expect(contrast(i.onSolid, fill) >= def.text, "\(input) \(def.id) on-solid")
                    #expect(contrast(fill, def.canvas) >= def.canvasFloor, "\(input) \(def.id) fill")
                }
                for bg in [def.canvas, def.surface, def.subtle, def.muted, i.subtle, i.subtleHover] {
                    #expect(contrast(i.strong, bg) >= def.text, "\(input) \(def.id) strong")
                }
                #expect(contrast(i.strongInverse, def.inverse) >= def.text, "\(input) \(def.id) strong-inverse")
            }
        }
    }

    @Test("Rook Red reproduces the generated default accent")
    func defaultAccent() {
        let derived = MonarchBrandTheme.derive(brand: "#dc2e3e").accent
        let rook = MonarchTokens.Accents.rook
        #expect(derived.light == rook.light)
        #expect(derived.dark == rook.dark)
        #expect(derived.hcLight == rook.hcLight)
        #expect(derived.hcDark == rook.hcDark)
    }

    @Test("Invalid input falls back to the default accent", arguments: ["", "red", "#12345", "#12345678", "#ggg"])
    func invalidInput(input: String) {
        let t = MonarchBrandTheme.derive(brand: input)
        #expect(!t.isValid)
        #expect(t.input == input)
        #expect(t.brand == "#dc2e3e")
        #expect(t.light.accent == MonarchTokens.Accents.rook.light)
    }

    @Test("Short and long hex parse; everything else is nil")
    func parsing() {
        #expect(parseHexColor("#ABC")?.hex == "#aabbcc")
        #expect(parseHexColor(" abc ")?.hex == "#aabbcc")
        #expect(parseHexColor("0A7CFF")?.hex == "#0a7cff")
        #expect(parseHexColor(nil) == nil)
        #expect(parseHexColor("#0a7cff00") == nil)
        #expect(parseHexColor("rgb(0,0,0)") == nil)
    }

    @Test("An AI colour too close to the accent is reported")
    func aiTooClose() {
        let t = MonarchBrandTheme.derive(brand: "#0a7cff", ai: "#0b7dff")
        #expect(t.ai?.isValid == true)
        #expect(t.ai?.isDistinct == false)
        #expect(t.ai?.closest?.to == "accent")
        #expect(t.ai?.warnings.count == 1)
        #expect(t.aiAccent != nil)
        #expect(MonarchBrandTheme.derive(brand: "#0a7cff").ai == nil)
        let invalid = MonarchBrandTheme.derive(brand: "#0a7cff", ai: "purple").ai
        #expect(invalid?.isValid == false)
        #expect(invalid?.color == nil)
    }

    @Test("withAccent applies the derived accent", arguments: MonarchTokens.themes)
    func withAccent(theme: MonarchThemeTokens) {
        let brand = MonarchBrandTheme.derive(brand: "#0A7CFF")
        let colors = theme.withAccent(brand.accent).colors
        #expect(colors.accentSolid == brand.mode(themeID: theme.id).accent.solid)
        #expect(colors.accentStrong == brand.mode(themeID: theme.id).accent.strong)
    }
}
