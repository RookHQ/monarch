# ADR 0012: Privacy & compliance is a strategic family

Status: accepted (2026-11-07), wave 10, under [ADR 0011](0011-strategic-scope.md)

## Context

Rook products set analytics and preference cookies, and their public pages
publish privacy and cookie policies. No product has a consent banner, a way to
change or withdraw consent, a "Do not sell or share" choice, a privacy-request
form or a record of consent decisions. The rules differ by jurisdiction: some
require asking before optional tracking (opt-in), some allow it but require an
easy way to refuse sale or sharing and respect the Global Privacy Control
signal (opt-out), and some only require a notice.

Waiting for three products to build their own banners would produce three
different, unreviewed interpretations of the same rules.

## Decision

Add **Privacy & compliance** as a strategic family (ADR 0011), wave 10:

| Id                           | What it is                                                                             |
| ---------------------------- | -------------------------------------------------------------------------------------- |
| `consent-manager`            | `ConsentProvider`, `useConsent`, `ConsentGate`, regime resolver, storage adapters      |
| `cookie-consent-banner`      | Regime-aware, non-modal banner                                                         |
| `consent-preferences`        | Per-category preferences dialog and the footer settings link                           |
| `privacy-choices-link`       | "Do not sell or share" choice for opt-out regimes, honouring Global Privacy Control    |
| `marketing-consent-checkbox` | Never pre-ticked marketing consent, per channel, with the exact wording for the record |
| `terms-acceptance`           | Required agreement to versioned terms at sign-up                                       |
| `terms-update-dialog`        | Asks for agreement again when terms change                                             |
| `data-export-request`        | "Download your data" with its preparation states                                       |
| `consent-history`            | The stored consent records, for the person or an admin                                 |
| `data-subject-request-form`  | Public form for access, deletion, correction and other privacy requests                |

Rules for the family:

1. **One model.** Every component reads `ConsentProvider`. Jurisdiction →
   regime is a data map (`DEFAULT_CONSENT_REGIMES`) that products replace, not
   code. Unknown location falls back to opt-in.
2. **Patterns, not legal advice.** Docs and copy say what a component does,
   never that a product "is compliant". Each product's legal team owns the
   regime map, the categories, the vendor list and the wording.
3. **Parity.** Where consent is asked for, refusing is as easy and as prominent
   as accepting, and nothing optional is pre-selected.
4. **Signals are choices.** Global Privacy Control turns off sale-or-sharing
   categories and, in opt-out regimes, is recorded as the decision.
5. **Proof.** Every decision produces a versioned `ConsentRecord`; a new
   policy version asks again.
6. **No early loading.** Nothing is decided on the server; `ConsentGate`
   renders nothing until the stored choice has been read.

## Consequences

- Products get the same banner and choices in every app, and can show a
  visitor their own consent history.
- The default regime map will age as laws change. It is data with a
  documented owner (each product's legal review), so updating it is a data
  change, not a component change.
- Server-side gating (not sending a script at all) needs the cookie storage
  adapter so the server can read the record.
