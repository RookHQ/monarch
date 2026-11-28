#!/usr/bin/env python3
"""Fails when a source file reads the real clock more often than it did.

A component that reads `Date()`, `.now`, `Date.now`, `Calendar.current` or `TimeZone.current` makes its
screenshot depend on the day. New code takes the current time from an argument (`today`, `now`) or from the
`monarchNow` environment value (Foundations/MonarchClock.swift), and the screenshot fixtures pin it
(`SnapshotEnvironment.pinnedNow`). Existing reads are counted per file in Scripts/real-clock-baseline.json;
a count may go down, never up, and a new file starts at zero.

    Scripts/check-clock.py            check
    Scripts/check-clock.py --update   write the baseline (after removing reads)
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
BASELINE = ROOT / "Scripts" / "real-clock-baseline.json"
PATTERN = re.compile(r"Date\(\)|Date\.now|[ (:\[,]\.now\b|Calendar\.current|TimeZone\.current")
SCOPES = [ROOT / "Sources" / "MonarchUI", ROOT / "Tests" / "MonarchUISnapshotTests"]


def counts():
    found = {}
    for scope in SCOPES:
        for path in sorted(scope.rglob("*.swift")):
            if "Generated" in path.parts:
                continue
            number = 0
            in_preview = False
            for line in path.read_text().split("\n"):
                stripped = line.strip()
                if stripped.startswith("#if DEBUG"):
                    in_preview = True
                if stripped.startswith("#endif") and in_preview and not line.startswith(" "):
                    in_preview = False
                if in_preview or stripped.startswith("//"):
                    continue
                if PATTERN.search(line):
                    number += 1
            if number:
                found[str(path.relative_to(ROOT))] = number
    return found


def main():
    now = counts()
    if "--update" in sys.argv:
        BASELINE.write_text(json.dumps(now, indent=2, sort_keys=True) + "\n")
        print(f"check-clock: wrote {len(now)} files")
        return 0
    base = json.loads(BASELINE.read_text()) if BASELINE.exists() else {}
    worse = {f: (base.get(f, 0), n) for f, n in now.items() if n > base.get(f, 0)}
    for file, (was, is_now) in sorted(worse.items()):
        print(f"check-clock: {file}: {is_now} reads of the real clock, baseline {was}. Use an argument or monarchNow.")
    if worse:
        return 1
    print(f"check-clock: ok ({sum(now.values())} reads in {len(now)} files, none new)")
    return 0


sys.exit(main())
