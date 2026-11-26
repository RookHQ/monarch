# ADR 0007: Keep Poppins; standardise the scale

Status: accepted (2026-11-03), revisit after the pilot

## Decision

Poppins stays the brand face (`--mn-font-sans` reads `--font-poppins` from
`next/font`, then falls back to system UI). The type scale uses Tailwind's
names and sizes, so `text-sm` means the same everywhere. Components default to
14px text with 500 weight for controls and 600 for titles, with slight negative
tracking on headings.

## Open question

Poppins is geometric with wide letterforms; dense tables show it. A UI face
with tabular figures (for example Inter) could be introduced as
`--mn-font-ui` for data-dense areas. Needs a design decision by the brand owner.
