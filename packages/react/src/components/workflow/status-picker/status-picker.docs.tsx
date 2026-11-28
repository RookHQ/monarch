import { useState } from 'react';
import { StatusPicker, type StatusOption } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './status-picker.fixture.json';

const STATUSES = fixture.data.statuses as StatusOption[];

function StatusDemo({ compact, opened }: { compact?: boolean; opened?: boolean }) {
  const [value, setValue] = useState(fixture.data.value);
  const [open, setOpen] = useState(opened ?? false);
  return (
    <StatusPicker
      open={open}
      onOpenChange={setOpen}
      statuses={STATUSES}
      value={value}
      onValueChange={setValue}
      showLabel={!compact}
      size={compact ? 'sm' : 'md'}
    />
  );
}

export default {
  id: 'status-picker',
  title: 'StatusPicker',
  summary:
    'A menu button for an issue’s status, grouped by category, with a glyph and name for each.',
  sections: {
    purpose: {
      summary:
        'Shows and changes the workflow status of an issue, task or ticket. Statuses are the team’s own names, grouped under five fixed categories that decide the glyph and colour. Needed by: Rook Stride (`components/issues/pickers.tsx` `StatusPicker` and `StatusIcon`), Rook Tickets and any board or list with workflow states.',
      useWhen: [
        'An issue row, card or detail panel shows a status that people can change.',
        'Teams define their own status names but share categories.',
      ],
      avoidWhen: [
        'Read-only status: use StatusGlyph with text, or StatusBadge.',
        'Moving through linear steps of a form: use Stepper.',
      ],
    },
    designPrinciples: {
      items: [
        'Glyph plus name, never colour alone: dashed ring (backlog), ring (not started), half-filled (in progress), check (completed), cross (cancelled).',
        'Categories order the menu so the workflow reads top to bottom.',
        'Follows the WAI-ARIA menu button pattern with `menuitemradio` items, built on DropdownMenu.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`StatusGlyph({ category })` is exported for read-only displays (IssueCard uses it).',
        '`STATUS_CATEGORIES` lists the categories in order.',
        'The ref is the trigger `<button>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from Stride’s `StatusPicker({ teamId, value, onChange })`: pass the team’s `states` mapped to `{ id, name, category: state.category.toLowerCase() }` (`CANCELED` → `cancelled`) as `statuses`, and `onChange` as `onValueChange`. The state’s hex `color` is not used; colours come from the category tokens.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showLabel',
          values: ['true', 'false'],
          description: 'Name and chevron (default), or a glyph-only trigger for dense rows.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '24 or 32px trigger (44px on coarse pointers).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Category',
          attribute: 'data-category',
          description: 'The current status’s category.',
        },
        {
          name: 'Icon only',
          attribute: 'data-icon-only',
          description: 'No visible name; the accessible name still says the status.',
        },
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'The menu is open (from Radix).',
        },
        { name: 'Disabled', description: 'The trigger is disabled.' },
      ],
    },
    interaction: {
      items: [
        'Clicking the trigger opens the menu; choosing a status calls `onValueChange` and closes it.',
        'The current status is checked in the menu.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / Arrow Down', action: 'Open the menu and focus the first status.' },
        { keys: 'Arrow Up / Down', action: 'Move between statuses.' },
        { keys: 'Letters', action: 'Jump to a status by name.' },
        { keys: 'Enter / Space', action: 'Choose the focused status.' },
        { keys: 'Escape', action: 'Close and return focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'button with `aria-haspopup="menu"`; menu of menuitemradio grouped under labels',
      name: '“Status: {name}”, which includes the visible name.',
      items: [
        'Glyphs are decorative; names carry the meaning.',
        'Category headings label each group in the menu.',
      ],
    },
    responsive: {
      items: [
        'The trigger truncates long names; the menu follows DropdownMenu positioning.',
        'On coarse pointers the trigger is at least 44px.',
      ],
    },
    theme: {
      items: [
        'Category colours: backlog and cancelled `--mn-color-text-subtle`, not started `--mn-color-text-muted`, in progress `--mn-color-warning-strong`, completed `--mn-color-success-strong`.',
        'Forced colours: glyphs in `CanvasText`, trigger border `ButtonText`.',
      ],
    },
    examples: {
      items: [
        { title: 'Status picker', stage: true, render: () => <StatusDemo opened /> },
        { title: 'Compact, glyph only', render: () => <StatusDemo compact /> },
      ],
    },
    doDont: {
      do: [
        'Keep status names short (“In review”, not “Waiting for code review”).',
        'Use the category that matches the meaning, so reports and colours stay consistent.',
      ],
      dont: [
        'Do not colour statuses with your own hex values.',
        'Do not hide the name in places where people scan for status.',
      ],
    },
    related: {
      items: [
        { id: 'priority-picker', reason: 'The same pattern for priority.' },
        { id: 'issue-card', reason: 'Shows status with the issue.' },
        { id: 'dropdown-menu', reason: 'The menu it is built on.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['dropdown-menu'] },
    testing: {
      unit: [
        'Trigger name, category, popup and decorative glyph.',
        'Keyboard open, category groups in order, checked item, arrow keys, choose, focus return.',
        'Glyph-only trigger keeps its name; no status; translated and disabled.',
        'A glyph per category; axe on trigger and menu; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
