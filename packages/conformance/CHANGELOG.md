# @rookcom/monarch-conformance

## 0.5.3

### Patch Changes

- d54a464: - ESLint plugin at `@rookcom/monarch-conformance/eslint` (flat config, ESLint 9, optional peer): `no-deep-imports`, `no-deprecated-api`, `no-raw-colors`, `no-arbitrary-spacing`, `prefer-monarch-component`, `require-accessible-name`, `require-entry-styles` and `no-rook-specific`, with `configs.recommended` and `configs.general`.
  - `monarch create <dir> --template next-app|vite-react [--accent <hex>]`: starter apps with the neutral accent, Tailwind 4, the ESLint plugin and agent instructions.
  - `monarch figma-connect --map <file>` generates Figma Code Connect files from the manifest.
  - SARIF output carries `fixes` for every finding with a machine fix and a `helpUri` per rule.
  - `api/deprecated` reports imports of Rook-specific exports from their old entries, with a safe fix to `@rookcom/monarch-react/rook`.

## 0.5.2

### Patch Changes

- 6850b3e: `monarch check` works without a registry: compatibility, deprecation and product data are bundled at build time. `--product` is optional, an unknown product is a warning instead of a usage error, and `--accent <hex>` serves teams outside Rook.

  `monarch check --fix` applies safe fixes (`--fix=all` also applies fixes that need review). JSON findings carry `help`, `docs` and `fix`.

  New rule `imports/package-path` (warn): deep imports into a Monarch package's `dist/` folder, with a safe fix.

  `components/duplicated-primitive` reads a list generated from the component exports of `@rookcom/monarch-react` instead of a hand-kept list of 38.

  New commands: `monarch search`, `monarch add [--write]`, `monarch init-agents` and `monarch mcp` (a Model Context Protocol server over stdio with search_components, get_component, list_tokens, get_theme and check_code).

  Tailwind 4 is compatible (through `@rookcom/monarch-tokens/css/tailwind-v4`).

  Neutral help text for `icons/no-sparkles`; `overrides/monarch-internals` states which `mn-*` classes are public.

## 0.5.1

### Patch Changes

- 8cff175: `overrides/monarch-internals` no longer reports the documented public variables `--mn-toast-offset-block-start`, `--mn-toast-offset-block-end` and `--mn-toast-offset-inline-end`, in stylesheets or inline styles. Every other `--mn-*` variable and `.mn-*` selector is still reported. The list is exported as `PUBLIC_MONARCH_VARIABLES`.

## 0.5.0

### Minor Changes

- efcd409: Fixes for four problems apps hit when adopting Monarch.

  - Tables and other sideways-scrolling boxes no longer make phone pages scroll sideways. Visually hidden text (an `sr-only` label, `VisuallyHidden`) inside a horizontally scrolling Table escaped the scroll box because the box was not a containing block. `.mn-table-wrap` and every other Monarch container that scrolls horizontally now sets `position: relative`. Nothing else about their layout changes.
  - SidebarNav items take `badgeTone`: `'accent'` (the default, unchanged: a solid bubble for unread or new things, read as "4 new") or `'neutral'` (a quiet muted count styled like a Tabs count, read as the number alone and not drawn as a dot when the sidebar is collapsed). Use `badgeTone: 'neutral'` for counts such as items per category. The default stays `'accent'` because changing a default is a breaking change under the versioning policy. New optional `messages.count` sets the spoken text of neutral counts. New exported type `SidebarNavBadgeTone`.
  - `linkAs` and `linkComponent` now accept Next.js `Link`. The old type made `href` optional, which Next's `Link` (whose `href` is required) does not accept, so `linkAs={Link}` failed to type-check. New exported types `MonarchLinkComponent` (`'a'` or a component that takes a string `href`) and `MonarchLinkProps` are used by Breadcrumb, BottomTabBar, FavoritesList, MegaMenu, SidebarNav and TopNav `linkAs`, and by every shell `linkComponent`. `SidebarNavLinkComponent` and `ShellLinkComponent` remain as aliases. Only `'a'` is accepted as a tag name; other tags never made sense as a link. In Breadcrumb's collapsed menu, a level without `href` is now shown as a disabled item rather than a link with no target.
  - `monarch check` has a new rule, `styles/subpath-css` (warn). It reports components imported from a subpath entry (`@rookcom/monarch-react/patterns`, `/data`, `/charts`, `/ai`, `/shell` …) when no file in the project loads `styles.css`, `styles/<entry>.css` or the matching `styles/components/<id>.css`, and names the import to add. `styles/core.css` alone does not style subpath components. The getting-started guide now has an "if you import from X, also load Y" table.

## 0.4.1

### Patch Changes

- 9af23ef: `a11y/img-alt`, `a11y/click-without-key` and `dialogs/no-native-dialog` no longer report markup or calls quoted in comments, strings and template literals, such as an `<img>` named in a doc comment or a sanitiser demo's `onerror=alert(1)` payload. Multi-line template literals are covered too.

## 0.4.0

### Minor Changes

- 656151a: Wave 10: destructive and confirmation system (strategic scope, ADR 0011) and privacy and compliance (ADR 0012).

  - New components: `PromptDialog`/`usePrompt`, `ChoiceDialog`/`useChoice`, `UnsavedChangesGuard`/`useUnsavedChangesGuard`/`UnsavedChangesDialog`, `useUndoToast`, `StepUpAuthDialog`/`useStepUpAuth`, `TypeToConfirmInput`/`phraseMatches`, `ConsequenceList`, `WizardDialog`, `DangerZone`/`DangerZoneItem`.
  - New flows in `./patterns`: `DeleteWorkspaceFlow`, `DeleteAccountFlow`, `TransferOwnershipFlow`, `LeaveWorkspaceFlow`, `RemoveMemberFlow`, `BulkDestructiveFlow`, `GdprEraseFlow`, `DeactivateFlow`, `RevokeAccessFlow`.
  - `ConfirmDialog`: `tone="warning"` and a required `acknowledgement` checkbox.
  - `Toast`: `onDismiss` now always fires, even when cleanup runs before the close renders.
  - Conformance: new `dialogs/no-native-dialog` rule (error) for `window.confirm`, `alert` and `prompt`.
  - Privacy and compliance: `ConsentProvider`/`useConsent`/`ConsentGate` with jurisdiction-based regimes (opt-in, opt-out, notice), Global Privacy Control and versioned consent records; `CookieConsentBanner`, `ConsentPreferences`/`ConsentSettingsLink`, `PrivacyChoicesLink`, `MarketingConsentCheckbox`, `TermsAcceptance`, `TermsUpdateDialog`, `DataExportRequest`, `ConsentHistory`, and `DataSubjectRequestForm` in `./patterns`.

### Patch Changes

- 9224222: Clearer description and README for everyone using Monarch.

## 0.3.1

### Patch Changes

- 0e0e6b3: The packages are open source under the MIT license. The npm metadata of the earlier tokens and conformance releases still said `UNLICENSED`; this release declares `MIT`, ships the `LICENSE` file, and adds a License section to each README.

## 0.3.0

### Minor Changes

- New rule `icons/no-sparkles` (error): Rook does not use sparkle icons. Flags `Sparkles`, `Sparkle`, `WandSparkles` and its old name `Wand2` imported from lucide-react, Radix icons, react-icons or Heroicons (aliases included). Use `Bot` for AI features.

## 0.2.0

### Minor Changes

- a2d52a3: Tokens: contrast is now measured on 8-bit colour as browsers paint it, so no pair passes in maths and fails on screen (Rook Blue `info` was 4.48:1 in the browser).

  Conformance: recognises the new components and patterns as Monarch primitives; `monarch-ignore-next-line` skips comment-only lines so it can sit next to `eslint-disable-next-line`; 4-digit numbers like `Order #1042` are no longer reported as colours.

- Packages move to the public npm registry as `@rookcom/monarch-*` (were `@rookhq/monarch-*` on GitHub Packages). Apps install with plain `npm ci`; no registry token is needed. Releases are published from CI with npm trusted publishing and provenance.

### Patch Changes

- 6918d34: Fewer false positives in two accessibility rules. `a11y/click-without-key` no longer flags option, menuitem, tab, treeitem and gridcell items in files that use `aria-activedescendant`, where the focused container handles the keys. `a11y/outline-none-without-ring` accepts a `focus-within` ring, outline, border or shadow on a wrapper up to 15 lines above. Across the 20 registered apps this removes 12 and 21 findings respectively, all checked by hand.
