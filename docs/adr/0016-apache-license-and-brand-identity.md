# ADR 0016: Apache License 2.0 and the trademark policy

Status: accepted (2026-11-14). Supersedes the license part of
[ADR 0008](0008-distribution.md).

## Context

Monarch was published under the MIT License. MIT has no patent grant and no
NOTICE mechanism, and it says nothing about trademarks. Many large open-source
projects use the Apache License 2.0 for those reasons.

All commits so far are by Rook Platforms, so the code can be relicensed
without asking anyone else.

## Decision

1. From the next release, all packages, the documentation site and the native
   libraries are licensed under the Apache License 2.0. The root has the
   unmodified license text (`LICENSE`), a `NOTICE` file, and every npm
   package ships both. Earlier releases stay available under MIT.
2. `TRADEMARK.md` states what the license does not grant: the Rook and
   Monarch names and logos, endorsement, and impersonation of Rook. "Built
   with Monarch" and other factual references are allowed.
3. Every component, design token and theme, including every accent and
   product theme, is free to use. Teams can also bring their own colour with
   `deriveTheme` and `brandThemeCss`.
4. The logo, icon and social image files of the documentation site are brand
   assets and are excluded from the license (`NOTICE`,
   `apps/docs/public/BRAND-ASSETS.md`).
5. Third-party dependencies must use a permissive license compatible with the
   Apache License 2.0 (MIT, BSD, ISC, 0BSD or Apache-2.0).

## Consequences

- Users get an explicit patent grant and clear attribution rules.
- Redistributions must keep `LICENSE` and `NOTICE`.
- This is not legal advice. The trademark policy has not been reviewed by a
  lawyer, and the marks are not registered by this change.
