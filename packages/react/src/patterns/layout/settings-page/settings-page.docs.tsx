import { useState } from 'react';
import {
  Button,
  DangerZoneItem,
  Input,
  SettingsRow,
  SettingsSection,
  Switch,
} from '@rookcom/monarch-react';
import { SettingsPage } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const sampleNav = [
  { id: 'demo-general', label: 'General' },
  { id: 'demo-notifications', label: 'Notifications' },
];

function DirtyDemo() {
  const [saved, setSaved] = useState('Workspace A');
  const [name, setName] = useState(saved);
  const dirty = name !== saved;
  return (
    <SettingsPage
      title="Workspace settings"
      headingLevel="h2"
      description="Edit the name to see the save bar."
      dirty={dirty}
      onSave={() => setSaved(name)}
      onDiscard={() => setName(saved)}
      interceptLinks={false}
    >
      <SettingsSection title="Workspace details" headingLevel="h3">
        <SettingsRow label="Workspace name">
          <Input value={name} onChange={(e) => setName(e.target.value)} />
        </SettingsRow>
      </SettingsSection>
    </SettingsPage>
  );
}

export default {
  id: 'settings-page',
  title: 'SettingsPage',
  summary:
    'A settings page template: section nav, SettingsSections, a danger zone and a page-level save bar with an unsaved-changes guard.',
  sections: {
    purpose: {
      summary:
        'Gives every product the same settings page shape, so navigation, saving, leaving with unsaved changes and destructive actions behave alike. Needed by: Rook ID (`rookid/app/workspaces/[id]/settings/page.tsx`), Atlas (`rookatlas-frontend/src/app/(workspace)/w/[slug]/settings/page.tsx` with `features/settings/components/SettingsShell.tsx`), People, Stride, Calendar and Intellect (`src/app/(workspace)/w/[slug]/settings/page.tsx` in each frontend), Links (`src/app/(dashboard)/workspace/[workspaceId]/settings/page.tsx`).',
      useWhen: [
        'A workspace, product or account settings page with several sections.',
        'Settings saved as one draft for the whole page, which must not be lost on navigation.',
      ],
      avoidWhen: [
        'A single short form: use SettingsSection alone.',
        'App-wide settings across many routes with a sidebar: use the app shell’s settings navigation, with SettingsPage per route.',
      ],
    },
    designPrinciples: {
      items: [
        'One way to leave safely: while the draft is dirty, closing the tab, reloading or following a link asks first.',
        'Destructive actions live together at the end, in a DangerZone, never mixed into ordinary rows.',
        'Navigation is plain links to section ids, so it works without JavaScript and with the back button.',
        'The save bar appears only when there is something to save.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native attributes go to the root `<div>`.',
        '`guardRef` exposes `guard(proceed)` from UnsavedChangesGuard for router navigation.',
        '`defaultSettingsPageMessages` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from shadcn/ui (Rook ID workspace settings): each `Card` + `CardHeader` + `CardTitle` + `CardContent` becomes a `SettingsSection` with `title` and an `id`; the delete-workspace `Dialog` becomes a `DangerZoneItem` in `dangerZone` whose action opens a ConfirmDialog; Atlas’s `SettingsShell` nav becomes `nav`.',
        'Per-section saving still works: give SettingsSections their own `onSave` and leave SettingsPage’s `onSave` out. Pass `dirty` to SettingsPage whenever any section is dirty so leaving is guarded.',
        'Replaces any `window.confirm("You have unsaved changes")` or `beforeunload` handler in the page.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'headingLevel',
          values: ['h1', 'h2'],
          description: '`h2` when the page is embedded under another h1.',
        },
        {
          prop: 'nav',
          values: ['none', 'items'],
          description: 'With items, a side nav on wide screens and a scrolling strip below 768px.',
        },
      ],
    },
    states: {
      items: [
        { name: 'With nav', attribute: 'data-has-nav', description: 'Two-column layout.' },
        {
          name: 'Dirty',
          attribute: 'data-dirty',
          description: 'The save bar shows, a status is announced and leaving is guarded.',
        },
        { name: 'Saving', description: '`saving`: Save shows a spinner and Discard is disabled.' },
        {
          name: 'Current section',
          attribute: 'aria-current',
          description: '`location` on the chosen section link (`page` for `href` items).',
        },
      ],
    },
    interaction: {
      items: [
        'Choosing a nav link jumps to the section and marks it current; the URL hash updates.',
        'On load, a section named in the URL hash is marked current.',
        'Save and Discard in the save bar call `onSave` and `onDiscard`.',
        'While dirty, leaving asks Keep editing / Discard changes / Save and leave.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the nav links, the sections, then the save bar.' },
        { keys: 'Enter', action: 'Follows the focused nav link.' },
        { keys: 'Escape', action: 'In the leave dialog: keeps editing.' },
      ],
    },
    accessibility: {
      role: 'navigation + regions',
      name: 'Nav: `messages.navLabel` ("Settings sections"); save bar: "Unsaved changes"; sections by their titles.',
      items: [
        'A polite status announces "You have unsaved changes." when the draft becomes dirty.',
        'The current section link uses `aria-current`, not colour alone.',
        'The danger-zone link is marked in danger colour and named "Danger zone".',
      ],
    },
    responsive: {
      items: [
        'Below 768px the nav becomes a horizontal strip above the sections that scrolls on its own; the page never scrolls sideways at 390px.',
        'The save bar sticks to the bottom; its buttons share the width on phones.',
        'Nav links and buttons are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Accent, danger, surface and border tokens; works in light, dark and every product accent.',
        'Logical properties throughout, so the nav sits on the right in RTL.',
        'Forced-colours mode outlines the current link in Highlight and keeps the save bar border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sections with nav and a danger zone',
          render: () => (
            <SettingsPage
              title="Workspace settings"
              headingLevel="h2"
              nav={sampleNav}
              interceptLinks={false}
              dangerZone={
                <DangerZoneItem
                  title="Delete this workspace"
                  description="Removes the workspace and everything in it."
                  action={<Button variant="danger">Delete workspace</Button>}
                />
              }
            >
              <SettingsSection id="demo-general" title="General" headingLevel="h3">
                <SettingsRow label="Workspace name">
                  <Input defaultValue="Workspace A" />
                </SettingsRow>
              </SettingsSection>
              <SettingsSection id="demo-notifications" title="Notifications" headingLevel="h3">
                <SettingsRow label="Email digest" description="A summary once a week.">
                  <Switch defaultChecked />
                </SettingsRow>
              </SettingsSection>
            </SettingsPage>
          ),
        },
        { title: 'Page-level save bar', render: () => <DirtyDemo /> },
      ],
    },
    doDont: {
      do: [
        'Give every section an `id` that matches its nav item.',
        'Pass `dirty` whenever anything on the page is unsaved.',
        'Confirm every danger-zone action with ConfirmDialog or a typed phrase.',
      ],
      dont: [
        'Do not combine a page save bar with per-section Save buttons for the same fields.',
        'Do not put destructive actions in ordinary sections.',
        'Do not call `window.confirm` before leaving; the guard already asks.',
      ],
    },
    related: {
      items: [
        { id: 'settings-section', reason: 'Each titled group of settings.' },
        { id: 'danger-zone', reason: 'The destructive actions at the end.' },
        { id: 'unsaved-changes-guard', reason: 'The leave guard used here.' },
        { id: 'page-header', reason: 'A richer header via `header`.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'visually-hidden', 'unsaved-changes-guard', 'danger-zone'],
    },
    testing: {
      unit: [
        'Title, named section nav with hash links and current marker, sections; axe clean.',
        'Keyboard choice of a nav link moves the current marker and calls back.',
        'Danger rows wrapped in a DangerZone with its nav link; unsafe hrefs fall back to the id.',
        'Save bar only while dirty; Save and Discard run; saving disables Discard; status announced; axe clean.',
        '`guardRef.guard` asks first and proceeds after Discard changes.',
        'Custom header, translated text, no nav, ref forwarding.',
        'Server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
