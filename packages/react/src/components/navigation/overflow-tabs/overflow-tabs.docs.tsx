import { OverflowTabs, TabsContent, type OverflowTabsItem } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const TABS: OverflowTabsItem[] = [
  { value: 'overview', label: 'Overview' },
  { value: 'activity', label: 'Activity', count: 4, countTone: 'accent' },
  { value: 'files', label: 'Files' },
  { value: 'members', label: 'Members' },
  { value: 'integrations', label: 'Integrations' },
  { value: 'billing', label: 'Billing', disabled: true },
  { value: 'audit', label: 'Audit log' },
  { value: 'settings', label: 'Settings' },
];

function Demo({ width, variant }: { width: number; variant?: 'line' | 'pill' }) {
  return (
    <div style={{ width: '100%', maxWidth: width }}>
      <OverflowTabs
        aria-label={`Workspace sections ${width}`}
        tabs={TABS}
        variant={variant ?? 'line'}
      >
        {TABS.map((t) => (
          <TabsContent key={t.value} value={t.value}>
            {t.label} content
          </TabsContent>
        ))}
      </OverflowTabs>
    </div>
  );
}

export default {
  id: 'overflow-tabs',
  title: 'OverflowTabs',
  summary: 'Tabs whose extra tabs collapse into a “More” menu as the width shrinks.',
  sections: {
    purpose: {
      summary:
        'A tab set with many sections that must stay on one row: tabs that do not fit move into a “More” menu, and the selected tab is always shown. An alternative to a scrolling tab list when people should see that more sections exist. Needed by: none recorded yet; it is for any record or settings page whose tab set no longer fits on phones.',
      useWhen: [
        'Six or more tabs in a container whose width varies (side panels, phones, resizable layouts).',
        'Sections where the first few are used most and the rest can sit behind “More”.',
      ],
      avoidWhen: [
        'Up to four short tabs: use Tabs.',
        'Links to separate pages: use TopNav (or TabsNav).',
        'When swiping through every tab matters more than seeing them: use `TabsList scrollable`.',
      ],
    },
    designPrinciples: {
      items: [
        'Built on Tabs: same look, same keyboard model, same TabsContent panels.',
        'Priority order: the last tabs move into “More” first.',
        'The selected tab never hides; choosing one from the menu brings it into the row and focuses it.',
        'The More button sits outside the tablist, so the tablist only contains tabs.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Accepts every Tabs root prop except `orientation` (always horizontal): `value`, `defaultValue`, `onValueChange`, `activationMode`, `dir`. The ref is the Tabs root `<div>`.',
        '`tabs` are data: `{ value, label, count?, countTone?, disabled? }`. Panels are your own TabsContent children.',
        'Widths are measured from hidden clones with a ResizeObserver; before measuring (and on the server) every tab is rendered and the row clips.',
        '`OVERFLOW_TABS_MESSAGES` holds the default strings.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['line', 'pill'],
          description:
            '`line` draws the bottom line under the tabs and the More button; `pill` keeps the track and puts More beside it. Exposed as `data-variant`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'aria-selected="true", data-state="active"',
          description: 'As in Tabs; always in the row.',
        },
        {
          name: 'Overflowing',
          attribute: 'data-overflowing (on the bar)',
          description: 'Some tabs are in the More menu.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled / aria-disabled',
          description: 'Skipped by arrows in the row; not selectable in the menu.',
        },
        {
          name: 'Menu open',
          attribute: 'data-state="open" (More)',
          description: 'More shows the default text colour.',
        },
      ],
    },
    interaction: {
      items: [
        'Resizing re-measures and moves tabs between the row and the menu.',
        'Choosing a tab in the menu selects it, shows its panel, brings it into the row in place of the last visible tab, and moves focus to it.',
        'Controlled with `value` + `onValueChange`, or uncontrolled with `defaultValue` (first enabled tab otherwise).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the selected tab, then the More button, then the panel.' },
        { keys: 'ArrowLeft / ArrowRight', action: 'Moves between the visible tabs.' },
        { keys: 'Home / End', action: 'First or last visible tab.' },
        {
          keys: 'Enter / Space / ArrowDown (More)',
          action: 'Opens the menu of the remaining tabs.',
        },
        { keys: 'ArrowUp / ArrowDown, Enter', action: 'Moves in the menu and selects a tab.' },
        { keys: 'Escape', action: 'Closes the menu, focus returns to More.' },
      ],
    },
    accessibility: {
      role: 'tablist, tab, tabpanel; the overflow is a menu button',
      name: '`aria-label` names the tablist (required).',
      items: [
        'Panels stay labelled by their tab, because the selected tab is always rendered.',
        'The More button has `aria-haspopup="menu"` and `aria-expanded`; it is not inside the tablist.',
        'Counts are part of each tab’s name, in the row and in the menu.',
        'Measuring clones are `aria-hidden` and not focusable.',
      ],
    },
    responsive: {
      items: [
        'Never wider than its container: at 390px a few tabs and More remain.',
        'Tabs are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Uses Tabs’ tokens (accent underline, pill surface, count tones) and DropdownMenu for the menu.',
        'Forced colours: the line is drawn as a CanvasText border.',
      ],
    },
    examples: {
      items: [
        { title: 'Wide', stack: true, render: () => <Demo width={900} /> },
        { title: 'Narrow', stack: true, render: () => <Demo width={360} /> },
        { title: 'Pill', stack: true, render: () => <Demo width={420} variant="pill" /> },
      ],
    },
    doDont: {
      do: [
        'Order tabs by how often they are used.',
        'Give every tab a matching TabsContent.',
        'Name the tablist with `aria-label`.',
      ],
      dont: [
        'Do not use it for page links.',
        'Do not put more than about a dozen tabs in one set; group sections instead.',
      ],
    },
    related: {
      items: [
        { id: 'tabs', reason: 'The base component; use it when the tabs fit.' },
        { id: 'top-nav', reason: 'The same overflow behaviour for page links.' },
        { id: 'dropdown-menu', reason: 'The menu used for the overflow.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['dropdown-menu', 'tabs'] },
    testing: {
      unit: [
        'All tabs render with Tabs semantics when there is room; no axe violations.',
        'With a measured width, extra tabs move into More; arrows and End rove the visible tabs; Tab reaches More; no axe violations.',
        'Choosing from the menu selects the tab, shows its panel, brings it into the row and focuses it; disabled entries are marked.',
        'A controlled selected tab stays visible; More is translatable; pill variant.',
        'Ref and class names pass through; `defaultValue` works.',
        'Renders on the server with every tab.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
