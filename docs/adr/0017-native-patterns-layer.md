# ADR 0017: Native patterns layer

Status: accepted (2026-11-15). The split is at the documentation level; the
module split is a recorded follow-up with its triggers below.

## Context

The decision of 2026-11-14 named a patterns layer for the native libraries: a
SwiftPM target `MonarchPatterns` and a Gradle module `:monarch-patterns`, with
the rule that the core library never imports patterns.

Before moving code, the repository was checked:

1. On the web, "patterns" are recipes and sample pages. In
   `platforms/parity.json` the 64 ids with `layer: "patterns"` are, on
   Android, all `recipe`, `native` or `none` entries, and none is a custom
   build. There is no pattern code to move.
2. The code that a module split would move is the experimental domain
   composites in the components layer: about 56 on iOS and 72 on Android.
   - On Android all 72 folders use `internal` declarations of other folders
     (about 230 references; for example `block-editor` 9, `timesheet-grid` 7,
     `team-calendar` 6, `query-builder` 6). Kotlin `internal` is not visible
     across Gradle modules, so each would have to become public API or an
     opt-in internal API surface.
   - On iOS the `package` access level works across targets of one package,
     but about 2,000 declarations would have to be audited again.
3. Some stable entries depend on experimental ones (`kanban-board` on
   `issue-card`, `bubble-menu` on `rich-text-editor`, `rook-rail` on
   `support-panel`, `delete-account-flow` on `data-export-request`,
   `invite-members-dialog` on `app-access-picker`). "Core never imports
   patterns" needs those resolved first. The status rule of this pass (stable
   public API never exposes an experimental type) resolves them.
4. Experimental marking already gives apps the separation that matters: no
   stability promise and a compiler-enforced opt-in
   (`@ExperimentalMonarchApi` with `RequiresOptIn.Level.ERROR` on Android,
   `@_spi(Experimental)` on iOS). The purpose recorded for the layer was
   "patterns ship experimental until a Rook app adopts them".

## Decision

1. In this pass the split is made at the documentation level. Code does not
   move between modules or targets.
2. A **pattern** is an inventory entry that is experimental **and** a domain
   composite: an AI panel, a product screen or another composite specific to
   one domain. Its inventory file has `"layer": "patterns"`. The inventory
   schemas of both platforms define the field, and the inventory checks fail
   when a pattern is not experimental.
3. The documentation site shows a pattern with a **Pattern (experimental)**
   badge in place of the status badge, and lists the patterns in their own
   group, "Patterns (experimental)", at the end of each platform catalogue.
4. The rule "core never imports patterns" holds for public API now: a stable
   declaration never exposes a pattern's type. A stable component may use a
   pattern internally until the module split.

## Follow-up: the module split

The physical split (`MonarchPatterns`, `:monarch-patterns`, core never
imports patterns) is made when either of these happens:

- a pattern is promoted to stable, which needs a home that is not the
  experimental part of the core library; or
- the shared internal helpers have been consolidated and made `public` (on
  Android, behind an opt-in) or `package` (on iOS), so a pattern can build on
  them from another module.

The lowest-risk first step, when one of these happens, is iOS: a target
`MonarchPatterns` that depends on `MonarchUI`, with the core helpers it needs
changed from `internal` to `package`. Android follows when its helpers have a
public or opt-in surface.

## Consequences

- Apps see one library per platform, as today. A pattern needs the
  experimental opt-in.
- The inventory field and the badge make the layer visible before it exists
  as a module, so the later split changes imports, not documentation.
- Until the split, a pattern can use any internal helper of the core library.
  The split will turn some of those uses into public or `package` API, and
  that API goes through the normal review.
