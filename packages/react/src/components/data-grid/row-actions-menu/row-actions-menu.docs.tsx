import { RowActionsMenu } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const noop = () => {};

export default {
  id: 'row-actions-menu',
  title: 'RowActionsMenu',
  summary: 'The per-row “⋯” menu with destructive items grouped last and optional confirmation.',
  sections: {
    purpose: {
      summary:
        'Every table, list and card row needs the same overflow menu: a labelled “⋯” button, a menu of actions, destructive items in red at the end, and a confirmation before anything irreversible runs. Needed by: Atlas (file rows, `components/atlas/files/FileRows.tsx`), Base (admin members and moderation pages), People (`components/admin/PermissionsView.tsx`).',
      useWhen: [
        'A row has more actions than fit as buttons.',
        'A destructive row action should ask first (pass `confirm`).',
      ],
      avoidWhen: [
        'One or two actions: show them as buttons.',
        'Actions on many rows at once: use BulkActionBar.',
        'A right-click menu: use ContextMenu.',
      ],
    },
    designPrinciples: {
      items: [
        'Destructive items always sit last, after a separator, in the danger tone.',
        'Confirmation runs the action inside the ConfirmDialog, so pending and error states show there.',
        'The trigger is named after the row (“Actions for Report 12”), never just “More”.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`RowAction` has the same shape as DataTable’s `DataTableRowAction` (plus `shortcut` and `confirm`); `inline` actions are skipped.',
        'The ref goes to the trigger `<button>`. Renders nothing when there are no menu actions.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui (`DropdownMenu` + `Button size="icon-sm"` + `MoreHorizontal`): the trigger’s `aria-label` → `rowLabel` (or `messages.trigger`); each `DropdownMenuItem` → an entry in `actions` (`onSelect`, `disabled`, `icon`); `className="text-destructive"` → `tone: "danger"`; a `window.confirm` or follow-up AlertDialog → `confirm: { title, description, confirmLabel }`.',
      ],
    },
    variants: {
      items: [
        { prop: 'size', values: ['xs', 'sm', 'md'], description: 'Trigger size. Default `sm`.' },
        {
          prop: 'align',
          values: ['start', 'center', 'end'],
          description: 'Menu alignment. Default `end`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'The menu is showing; the trigger stays highlighted.',
        },
        { name: 'Danger item', attribute: 'data-tone="danger"', description: 'Red text, last.' },
        {
          name: 'Disabled item',
          attribute: 'aria-disabled="true"',
          description: 'Skipped by arrow keys.',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing an item closes the menu and runs it; focus returns to the trigger.',
        'An item with `confirm` opens a ConfirmDialog; the action runs only on confirm.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / ArrowDown', action: 'Opens the menu.' },
        { keys: 'ArrowUp / ArrowDown', action: 'Moves between items.' },
        { keys: 'Enter', action: 'Runs the focused item.' },
        { keys: 'Escape', action: 'Closes and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'button with aria-haspopup="menu", then menu with menuitems',
      name: '“Actions for {rowLabel}”, or “Row actions”.',
      items: [
        'The menu is labelled by its trigger.',
        'After a confirmation, focus returns to the trigger.',
      ],
    },
    responsive: {
      items: [
        'The trigger is 44 px on touch screens (IconButton).',
        'The menu repositions to stay on screen.',
      ],
    },
    theme: {
      items: [
        'Uses DropdownMenu and IconButton styles; the danger tone uses danger tokens.',
        'Forced colours: the open trigger gets a Highlight outline.',
      ],
    },
    examples: {
      items: [
        {
          title: 'With a confirmed delete',
          render: () => (
            <RowActionsMenu
              rowLabel="Report 12"
              actions={[
                { id: 'open', label: 'Open', onSelect: noop },
                { id: 'rename', label: 'Rename', shortcut: 'F2', onSelect: noop },
                {
                  id: 'delete',
                  label: 'Delete',
                  tone: 'danger',
                  onSelect: noop,
                  confirm: {
                    title: 'Delete Report 12?',
                    description: 'The report and its history are removed.',
                    confirmLabel: 'Delete report',
                  },
                },
              ]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass `rowLabel` so each trigger has a distinct name.',
        'Use `confirm` for actions that cannot be undone; prefer an undo toast for those that can.',
      ],
      dont: [
        'Do not hide the only way to do something important in the menu.',
        'Do not call `window.confirm` in `onSelect`.',
      ],
    },
    related: {
      items: [
        { id: 'dropdown-menu', reason: 'The menu it is built on.' },
        { id: 'confirm-dialog', reason: 'The confirmation for destructive items.' },
        { id: 'data-table', reason: 'Its `rowActions` render the same shape.' },
        { id: 'bulk-action-bar', reason: 'Actions on many rows.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'confirm-dialog', 'dropdown-menu'],
    },
    testing: {
      unit: [
        'Opens by keyboard, orders danger items last after a separator, runs an item, returns focus.',
        'Confirmed action runs inside the dialog; cancel does not run it.',
        'Disabled items, custom messages, nothing rendered without menu actions.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
