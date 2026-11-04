# Component contract

Every public component in `@rookcom/monarch-react` meets all of these before
it is exported. The manifest (`dist/manifest.json`) is generated from the
types and doc comments, so the comment on a prop is the documentation.

1. **Typed props** extending the native element's attributes; no `any`;
   variants as string unions (`ButtonVariant`), never booleans per variant.
2. **Accessible name** is guaranteed by the type where possible
   (`IconButton.label` is required). Decorative icons are `aria-hidden`.
3. **Keyboard**: native element semantics or a Radix primitive; Enter/Space
   activate, Escape closes, arrows move within composites, focus is trapped
   in modals and restored on close.
4. **Focus visible**: `:focus-visible` ring from `--mn-color-focus-ring`;
   never `outline: none` without a replacement.
5. **States**: `disabled` (native), `loading` (`aria-busy`, stays
   focusable), `invalid` (`aria-invalid`, wired by `Field`), `data-*`
   attributes for styling.
6. **Theme-agnostic**: styles use only `--mn-*` variables; the build fails
   on a raw colour. Works in all four themes and every product accent.
7. **SSR-safe**: no browser globals at module scope; `'use client'` only on
   files that need state or Radix.
8. **Composable**: small parts (`Card`, `CardHeader`, `CardTitle`) over
   kitchen-sink props; `asChild` where the element should be swappable.
9. **Tests**: interaction (user-event), axe (`expectNoA11yViolations`),
   SSR (`renderToString`), and the API snapshot.
10. **Stable surface**: `mn-*` class names and `data-*` attributes are part
    of the public API and follow semver.

## Component list (0.2.0)

Actions: Button, IconButton, Link · Forms: Field (+Label/Description/Error),
Input, Textarea, Checkbox, Switch, Select (+parts), Combobox · Display:
Badge, Avatar, Card (+parts), Skeleton, EmptyState, Kbd, Spinner,
VisuallyHidden, Separator, Progress · Data: Table (+Header/Body/Row/Head/
Cell/Caption/SelectCell, SortButton), Pagination · Feedback: Alert, Toast
(ToastProvider, useToast) · Overlays: Dialog (+parts), DropdownMenu
(+parts), Popover (+parts), Tooltip, TooltipProvider · Navigation: Tabs
(+parts), Breadcrumb.

Patterns (see `patterns.md`): PageHeader, Toolbar (+ToolbarGroup),
FilterBar, FilterChip, SettingsSection, SettingsRow, DescriptionList,
DescriptionItem, SplitLayout, StatCard, ErrorState, LoadingState.

### Marketing layer (0.3.0)

SiteHeader, MarketingHero, MarketingSection, MarketingSectionHeader, FeatureGrid,
ProductFrame, StatStrip, CTABand, Accordion (+Item/Trigger/Content), FAQ,
SiteFooter. See [marketing.md](marketing.md).
