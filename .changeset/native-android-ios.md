---
'@rookcom/monarch-react': patch
---

Monarch for Android and Monarch for iOS release with this version, as `0.14.N` of `@rookcom/monarch-react`: Maven `dev.monarchui:monarch-compose`, `monarch-tokens` and the optional `monarch-fonts-poppins`, and the plain git tag for Swift Package Manager (products `MonarchUI`, `MonarchTokens` and the optional `MonarchFontsPoppins`). The native changelogs are `platforms/android/CHANGELOG.md` and `platforms/ios/CHANGELOG.md`; the web package itself has no code change from the native work.

The deprecated import paths of the Rook-specific exports (`/shell`, `/patterns`, `/ai`) now say, in their `@deprecated` tags, that they are removed no earlier than 2027-02-11 (90-day window); they said they were removed in 0.15.0, and there is no 0.15.0. MONARCH-COMPAT: none needed; the paths still export the names, and `monarch check --fix` still rewrites the imports.
