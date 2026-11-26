# Marketing pages

Public pages (product sites, launch pages, solution pages) use the marketing
layer: `import { SiteHeader, MarketingHero, … } from '@rookcom/monarch-react/marketing'`.
It is a separate entry so signed-in apps never load it. The reference composition is the
[`/__marketing` example](#/__marketing) in the docs site.

## The page, top to bottom

| Pattern                  | Use it for                    | Rules                                                                                                                      |
| ------------------------ | ----------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `SiteHeader`             | Wordmark, navigation, actions | One per page. Groups open a disclosure panel of links (not a menu). At most two actions: "Log in" (ghost) and one primary. |
| `MarketingHero`          | The opening statement         | The page's only `h1`, under ~8 words. One primary action, one secondary. `media` is a `ProductFrame`.                      |
| `MarketingSection`       | Every band after the hero     | `tone="subtle"` to alternate, `tone="inverse"` **at most once** per page. Spacing `lg` between major sections.             |
| `MarketingSectionHeader` | Eyebrow, `h2`, description    | Start-aligned by default; centre only short headers. In-app headings use the planned `section-header`.                     |
| `FeatureGrid`            | Benefits, capabilities        | 3 columns for 6 items, 2 for 4. Plain icons, never a sparkle (AI uses `Bot`).                                              |
| `ProductFrame`           | Product illustrations         | Build the inside from Monarch components. It is one image (`role="img"`, `inert`): give it a real `label`.                 |
| `StatStrip`              | Key figures                   | Only true, current numbers. No invented customers or vanity metrics.                                                       |
| `FAQ`                    | Buyer questions               | Questions a buyer asks; never internal notes ("Is this implemented?").                                                     |
| `CTABand`                | The closing call to action    | One per page, `tone="accent"`. Buttons invert automatically.                                                               |
| `SiteFooter`             | Columns, legal                | Column titles are real headings; links meet the 24px target.                                                               |

## Colour and type

- **One accent.** The product's registry accent is the only colour beyond
  neutrals. Gradients are derived from it (`--mn-gradient-wash`,
  `--mn-gradient-accent`, `--mn-gradient-inverse`); never hand-pick a gradient.
- **The spectrum** (`--mn-gradient-spectrum`) is for Rook brand moments, not
  product pages.
- **Display type:** `--mn-display-{sm,md,lg,xl}`, Poppins 600 with tight
  tracking. Body copy uses `--mn-lead` and the text scale. Never light weights
  for headings, never `text-[54px]`.
- **Dark bands** come only from `tone="inverse"`. Inside it, text, borders,
  surfaces, accent text and focus rings are remapped and tested for contrast in
  every theme and for every product.

## Don't

- Hex colours, `bg-[#…]`, `#111817` sections, hand-made gradients.
- Two inverse sections, two CTA bands, or more than one primary action per view.
- Headings inside illustrations that reach the page outline (use `ProductFrame`).
- Sparkle icons, crowns, gold, ornament.
