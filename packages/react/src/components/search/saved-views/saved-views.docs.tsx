import { useState } from 'react';
import { FilterBar, FilterChip } from '@rookcom/monarch-react';
import { SavedViews, type SavedView } from '@rookcom/monarch-react/data';
import type { ComponentDocs } from '../../../internal/component-docs.js';

type Filters = { status: string | null; owner: string | null };

const INITIAL: (SavedView & { filters: Filters })[] = [
  {
    id: 'all',
    name: 'All tickets',
    count: 64,
    readOnly: true,
    group: 'Built-in',
    filters: { status: null, owner: null },
  },
  {
    id: 'open',
    name: 'Open',
    count: 18,
    readOnly: true,
    group: 'Built-in',
    filters: { status: 'Open', owner: null },
  },
  {
    id: 'mine',
    name: 'My open tickets',
    count: 7,
    isDefault: true,
    group: 'My views',
    filters: { status: 'Open', owner: 'Me' },
  },
  {
    id: 'team',
    name: 'Team backlog',
    count: 23,
    shared: true,
    group: 'My views',
    filters: { status: 'Pending', owner: null },
  },
];

const same = (a: Filters, b: Filters) => a.status === b.status && a.owner === b.owner;

function ViewsWithFilters() {
  const [views, setViews] = useState(INITIAL);
  const [viewId, setViewId] = useState<string | null>('mine');
  const [filters, setFilters] = useState<Filters>({ status: 'Open', owner: null });
  const current = views.find((v) => v.id === viewId);
  const modified = !!current && !same(current.filters, filters);
  return (
    <div style={{ display: 'grid', gap: 12 }}>
      <SavedViews
        views={views}
        value={viewId}
        modified={modified}
        onValueChange={(id) => {
          setViewId(id);
          const v = views.find((x) => x.id === id);
          if (v) setFilters(v.filters);
        }}
        onDiscardChanges={() => current && setFilters(current.filters)}
        onSaveChanges={(id) =>
          setViews((all) => all.map((v) => (v.id === id ? { ...v, filters } : v)))
        }
        onSaveNew={(name) => {
          const id = `view-${views.length + 1}`;
          setViews((all) => [...all, { id, name, group: 'My views', filters }]);
          setViewId(id);
        }}
        onRename={(id, name) =>
          setViews((all) => all.map((v) => (v.id === id ? { ...v, name } : v)))
        }
        onSetDefault={(id) => setViews((all) => all.map((v) => ({ ...v, isDefault: v.id === id })))}
        onDelete={(id) => {
          setViews((all) => all.filter((v) => v.id !== id));
          setViewId('all');
          setFilters({ status: null, owner: null });
        }}
      />
      <FilterBar>
        {filters.status ? (
          <FilterChip
            name="Status"
            value={filters.status}
            onRemove={() => setFilters((f) => ({ ...f, status: null }))}
          />
        ) : null}
        {filters.owner ? (
          <FilterChip
            name="Owner"
            value={filters.owner}
            onRemove={() => setFilters((f) => ({ ...f, owner: null }))}
          />
        ) : null}
      </FilterBar>
    </div>
  );
}

export default {
  id: 'saved-views',
  title: 'SavedViews',
  summary:
    'View switcher for a list: named combinations of filters, sort and columns, with save, save as new, rename, delete, set default and an "Edited" marker.',
  sections: {
    purpose: {
      summary:
        'Lets people jump between named views of a list and keep the filters, sort and columns they set up. It shows when the current state differs from the selected view and offers to save or discard the change. The app stores the views; the component only renders them and reports choices.',
      useWhen: [
        'A list or table has filters, sorting or column choices people return to.',
        'Built-in views and personal or shared views live side by side.',
      ],
      avoidWhen: [
        'A handful of fixed views with no saving: use Tabs or TabsFilter.',
        'Saving a single search query: use SearchInput history in the app.',
      ],
    },
    designPrinciples: {
      items: [
        'No data: views come in as `views`, changes go out through `onSaveNew`, `onSaveChanges`, `onRename`, `onDelete`, `onSetDefault` and `onDiscardChanges`. What a view stores is up to the app.',
        'The app decides "modified" (it compares its state with the view), so any state can be part of a view.',
        'Read-only views (built-in or someone else’s) can be applied and copied, never changed.',
        'Destructive actions are confirmed; names are validated (required, unique, `maxNameLength`).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Imported from `@rookcom/monarch-react/data`. The ref is the trigger button.',
        '`SavedView`: `id`, `name`, `count`, `shared`, `isDefault`, `readOnly`, `group`.',
        'Callbacks may return a promise: the name dialog waits for it and shows `messages.saveFailed` if it rejects; delete uses ConfirmDialog, which shows the error.',
        '`messages` translates every string.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        { prop: 'size', values: ['xs', 'sm', 'md'], description: 'Trigger size. Default `sm`.' },
        {
          prop: 'group (view)',
          values: ['string'],
          description: 'Views with the same `group` are listed under that heading, in order.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Modified',
          attribute: 'data-modified',
          description:
            'An "Edited" marker in the trigger; the menu offers "Save changes to …" (editable views) and "Discard changes".',
        },
        {
          name: 'Selected view',
          attribute: 'aria-checked',
          description: 'Radio item of the current view.',
        },
        { name: 'Default view', description: 'A star, announced as "Default view".' },
        { name: 'Shared view', description: 'A "Shared" marker.' },
        {
          name: 'Saving',
          attribute: 'aria-busy',
          description:
            'The name dialog is busy while the save promise runs and keeps the error on failure.',
        },
        { name: 'No view', description: 'The trigger shows `messages.noView` ("All items").' },
      ],
    },
    interaction: {
      items: [
        'Choosing a view calls `onValueChange`; the app applies its filters, sort and columns.',
        '"Save as new view…" and "Rename…" open a dialog with a name field; Enter submits.',
        '"Delete view…" asks for confirmation first.',
        '"Set as default" is offered for views that are not already the default.',
        'After a dialog closes, focus returns to the trigger.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / Arrow Down', action: 'Opens the menu from the trigger.' },
        {
          keys: 'Arrow Up / Down, Home / End',
          action: 'Move through views and actions; type-ahead jumps by name.',
        },
        { keys: 'Enter', action: 'Selects a view or runs an action; submits the name dialog.' },
        { keys: 'Escape', action: 'Closes the menu or dialog and returns focus to the trigger.' },
      ],
    },
    accessibility: {
      role: 'button with a menu (menuitemradio items)',
      name: 'Visually hidden "View:" plus the view name, count and "Edited", e.g. "View: My open tickets 7 Edited".',
      items: [
        'Views are a radio group, so the current one is announced as checked.',
        'The default star has a text alternative; "Edited" is text, not colour alone.',
        'Actions are real menu items, not buttons nested in menu items.',
        'The name field is labelled and its errors are linked (FieldError).',
      ],
    },
    responsive: {
      items: [
        'The trigger truncates long names; the menu is at most 22rem or the screen width minus 2rem.',
        'Dialogs follow Dialog’s small size on phones.',
      ],
    },
    theme: {
      items: [
        'Trigger, menu and dialog use Button, DropdownMenu and Dialog tokens; the "Edited" marker uses the warning subtle tokens in every theme.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Views with filters, save and discard',
          stack: true,
          render: () => <ViewsWithFilters />,
        },
      ],
    },
    doDont: {
      do: [
        'Store filters, sort and visible columns in a view, and compute `modified` from all of them.',
        'Mark built-in and other people’s views `readOnly`.',
        'Return promises from callbacks so failures stay visible.',
      ],
      dont: [
        'Do not store row data in a view, only the settings that produce it.',
        'Do not apply a view without updating the URL if the app keeps filters there.',
      ],
    },
    related: {
      items: [
        { id: 'filter-bar', reason: 'Shows and edits the filters a view stores.' },
        { id: 'data-table', reason: 'Sort and columns a view can store.' },
        { id: 'dropdown-menu', reason: 'The menu SavedViews is built on.' },
        { id: 'tabs', reason: 'A few fixed views without saving.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'dropdown-menu',
        'dialog',
        'confirm-dialog',
        'form-field',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Trigger shows the view, count and "Edited".',
        'Switching views through grouped radio items (uncontrolled), default marker announced.',
        'Save changes, discard, rename, set default and delete appear only where allowed (read-only, modified, default).',
        'Save as new validates required and unique names, then returns focus to the trigger.',
        'A rejected save keeps the dialog open with an error; delete is confirmed.',
        'Keyboard open and Escape; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
