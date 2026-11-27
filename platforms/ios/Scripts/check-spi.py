#!/usr/bin/env python3
"""Fails when a stable public declaration of MonarchUI mentions an experimental one.

The compiler does not check this: `@_spi(Experimental)` hides a declaration from a plain import,
but a stable signature can still name an SPI type. This reads the symbol graphs that were written
with `--include-spi-symbols` and looks at the declaration of every public symbol that is not SPI
(and is not a member of an SPI type): if a type in it is SPI, the stable API leaks (D1).

Usage: Scripts/check-spi.py <symbol graph directory>   (called by Scripts/api.sh)
Standard library only.
"""

import json
import os
import sys


def fragments(symbol):
    """The preciseIdentifier of every type that the declaration of a symbol names."""
    found = set()
    for fragment in symbol.get("declarationFragments", []):
        if fragment.get("preciseIdentifier"):
            found.add(fragment["preciseIdentifier"])
    signature = symbol.get("functionSignature") or {}
    for part in signature.values():
        for item in part if isinstance(part, list) else [part]:
            if isinstance(item, dict):
                for fragment in item.get("declarationFragments", []):
                    if fragment.get("preciseIdentifier"):
                        found.add(fragment["preciseIdentifier"])
    return found


def main():
    directory = sys.argv[1]
    symbols = {}
    for name in sorted(os.listdir(directory)):
        if name.startswith("MonarchUI") and name.endswith(".symbols.json"):
            with open(os.path.join(directory, name), encoding="utf-8") as handle:
                for symbol in json.load(handle)["symbols"]:
                    symbols[symbol["identifier"]["precise"]] = symbol
    experimental = {key for key, symbol in symbols.items() if symbol.get("spi")}
    experimental_paths = {tuple(symbols[key]["pathComponents"]) for key in experimental}
    leaks = []
    for key, symbol in symbols.items():
        path = tuple(symbol["pathComponents"])
        if key in experimental or any(path[:size] in experimental_paths for size in range(1, len(path))):
            continue
        for used in sorted(fragments(symbol) & experimental):
            leaks.append("%s mentions the experimental %s" % (".".join(path), ".".join(symbols[used]["pathComponents"])))
    for leak in sorted(set(leaks)):
        print("check-spi: " + leak, file=sys.stderr)
    print("check-spi: %d experimental symbols, %d stable declarations that mention one" % (len(experimental), len(set(leaks))))
    sys.exit(1 if leaks else 0)


if __name__ == "__main__":
    main()
