---
'@rookcom/monarch-conformance': patch
---

Import-path deprecations (`registry/deprecations.json`, `importPaths`) name a date instead of a version: the field `removeIn` is now `removeNoEarlierThan`, a `YYYY-MM-DD` date, and the `api/deprecated` message says "no earlier than <date> (90-day window)". The bundled data carries the same value for the Rook-specific export moves (2027-02-11).

MONARCH-COMPAT: an app or a plugin that reads `ImportPathDeprecation.removeIn` reads `removeNoEarlierThan` instead; a registry file that still has `removeIn` is read without the removal text. Nothing else changes: the moved names are still exported from the old paths and the fix is the same.
