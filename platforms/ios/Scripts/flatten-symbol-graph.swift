// Turns the symbol graphs of one module into a sorted, readable list of its
// public declarations: one line per symbol, "kind  Path.To.Symbol  declaration".
//
// Usage: swift Scripts/flatten-symbol-graph.swift <symbolgraph dir> <module> <output file>

import Foundation

func fail(_ message: String) -> Never {
    FileHandle.standardError.write(Data("flatten-symbol-graph: \(message)\n".utf8))
    exit(1)
}

let arguments = CommandLine.arguments
guard arguments.count == 4 else { fail("expected <symbolgraph dir> <module> <output file>") }
let directory = URL(fileURLWithPath: arguments[1])
let module = arguments[2]
let outputURL = URL(fileURLWithPath: arguments[3])

// A module has one main graph and one graph per module it extends, for
// example MonarchUI.symbols.json and MonarchUI@SwiftUI.symbols.json.
let names = ((try? FileManager.default.contentsOfDirectory(atPath: directory.path)) ?? [])
    .filter { $0 == "\(module).symbols.json" || $0.hasPrefix("\(module)@") }
    .sorted()
guard !names.isEmpty else { fail("no symbol graph for \(module) in \(directory.path)") }

var lines = Set<String>()
for name in names {
    guard let data = try? Data(contentsOf: directory.appendingPathComponent(name)),
        let graph = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
        let symbols = graph["symbols"] as? [[String: Any]]
    else {
        fail("cannot read \(name)")
    }
    for symbol in symbols {
        let kind = (symbol["kind"] as? [String: Any])?["identifier"] as? String ?? "unknown"
        let path = (symbol["pathComponents"] as? [String] ?? []).joined(separator: ".")
        let fragments = symbol["declarationFragments"] as? [[String: Any]] ?? []
        let declaration = fragments.compactMap { $0["spelling"] as? String }.joined()
            .split(whereSeparator: \.isWhitespace).joined(separator: " ")
        lines.insert("\(kind.replacingOccurrences(of: "swift.", with: ""))  \(path)  \(declaration)")
    }
}

let output = lines.sorted().joined(separator: "\n") + "\n"
do {
    try output.write(to: outputURL, atomically: true, encoding: .utf8)
} catch {
    fail("cannot write \(outputURL.path): \(error)")
}
