import { useState } from 'react';
import {
  Button,
  Input,
  SettingsActionRow,
  SettingsRow,
  SettingsSection,
  Switch,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function DirtyDemo() {
  const [saved, setSaved] = useState({ name: 'Northwind', digest: true });
  const [draft, setDraft] = useState(saved);
  const [saving, setSaving] = useState(false);
  const dirty = draft.name !== saved.name || draft.digest !== saved.digest;
  return (
    <SettingsSection
      title="Workspace"
      description="Name and notifications for everyone in this workspace."
      dirty={dirty}
      saving={saving}
      onCancel={() => setDraft(saved)}
      onSave={() => {
        setSaving(true);
        setTimeout(() => {
          setSaved(draft);
          setSaving(false);
        }, 800);
      }}
    >
      <SettingsRow label="Workspace name">
        <Input value={draft.name} onChange={(e) => setDraft({ ...draft, name: e.target.value })} />
      </SettingsRow>
      <SettingsRow label="Weekly digest" description="A summary email every Monday.">
        <Switch
          checked={draft.digest}
          onCheckedChange={(digest) => setDraft({ ...draft, digest })}
        />
      </SettingsRow>
    </SettingsSection>
  );
}

export default {
  id: 'settings-section',
  title: 'SettingsSection',
  summary:
    'Titled card of settings rows with a save footer and dirty state, loading and no-access states, and a danger-zone variant.',
  sections: {
    purpose: {
      summary:
        'Groups related settings under a heading and description, in a card. SettingsRow lays out one setting and wires its label, description and error to the control; SettingsActionRow lays out one action such as "Delete workspace". The footer can be the standard Save/Discard bar driven by a `dirty` flag.',
      useWhen: [
        'Settings and preferences pages.',
        'A form section that saves as a unit and should say when it has unsaved changes.',
        'A destructive zone (delete, transfer) with `tone="danger"` and SettingsActionRow.',
      ],
      avoidWhen: [
        'Data-entry forms that are not settings: use Field directly.',
        'Settings that apply immediately (a single switch list): use SwitchSettingsList.',
      ],
    },
    designPrinciples: {
      items: [
        'Label and help on one side, control on the other, so long settings pages scan quickly.',
        'Each row is a Field, so accessibility wiring is automatic.',
        'Save/Discard live in the section footer, not per row, and stay disabled until something changed.',
        'The product owns the draft and the save; the section only shows the state it is given.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'SettingsSection renders a `<section>` labelled by its heading; native attributes pass through.',
        'SettingsRow is a Field: the control child receives the label, description, required, disabled and invalid wiring. `invalid` defaults to true when `error` is given.',
        'With `onSave`, the footer is a polite status line, any `actions`, Discard (`onCancel`) and Save. Without it, the footer is `actions` alone, as before.',
        'SettingsActionRow renders a row with a title, description and an action node; it is not a form field.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['default', 'danger'],
          description: '`danger` uses danger border and title colours. Exposed as `data-tone`.',
        },
        {
          prop: 'headingLevel',
          values: ['h2', 'h3'],
          description: 'Heading element for the title. Default `h2`.',
        },
        {
          prop: 'stickyFooter',
          values: ['false', 'true'],
          description: 'While `dirty`, the footer sticks to the bottom of the scroll area.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Dirty',
          attribute: 'data-dirty',
          description:
            'Enables Save and Discard, tints the footer and puts `dirtyMessage` in the status line.',
        },
        {
          name: 'Saving',
          attribute: 'aria-busy',
          description: 'Save shows a spinner and ignores clicks; Discard is disabled.',
        },
        {
          name: 'Loading',
          attribute: 'data-state="loading"',
          description: 'A skeleton replaces the rows; the footer is hidden; the section is busy.',
        },
        {
          name: 'No access',
          attribute: 'data-state="no-access"',
          description: 'A locked notice replaces the rows and the footer.',
        },
        {
          name: 'Row error',
          attribute: 'aria-invalid on the control',
          description:
            '`error` renders a polite live message under the control and marks it invalid.',
        },
        { name: 'Row required / disabled', description: 'Passed to the control through Field.' },
      ],
    },
    interaction: {
      items: [
        'Save calls `onSave`; Discard calls `onCancel`. Both are disabled while the section is not dirty.',
        'The status line announces "You have unsaved changes." when the section becomes dirty and clears after saving.',
        'Rows have no behaviour of their own beyond Field wiring; controls behave as usual.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves through the row controls, then Discard and Save.' },
        { keys: 'Enter / Space', action: 'On Save or Discard, activates it.' },
      ],
      notes: ['Disabled Save and Discard are skipped by Tab until the section is dirty.'],
    },
    accessibility: {
      role: 'region (section with aria-labelledby)',
      name: 'The section title.',
      items: [
        'Each row label is the control’s accessible name; the description is its accessible description.',
        'Row errors are announced through `aria-live="polite"`.',
        'The dirty message is a `role="status"` line, so it is announced without moving focus.',
        'Loading and saving set `aria-busy` on the section.',
      ],
    },
    responsive: {
      items: [
        'From 1024px the section header sits beside the card (18rem column); below, it stacks above.',
        'From 768px each row puts its text and control side by side (control column up to 20rem); below, they stack.',
        'The footer wraps: the status line takes its own line on narrow screens and the buttons stay together.',
      ],
    },
    theme: {
      items: [
        'Card, borders, footer, dirty tint and danger colours use Monarch semantic tokens, so every theme applies.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Profile settings',
          stack: true,
          render: () => (
            <SettingsSection
              title="Profile"
              description="How you appear to your teammates."
              actions={<Button variant="primary">Save</Button>}
            >
              <SettingsRow
                label="Display name"
                description="Shown on comments and assignments."
                required
              >
                <Input defaultValue="Ada Lovelace" />
              </SettingsRow>
              <SettingsRow label="Show my status">
                <Switch defaultChecked />
              </SettingsRow>
            </SettingsSection>
          ),
        },
        {
          title: 'Save footer with dirty state',
          stack: true,
          render: () => <DirtyDemo />,
        },
        {
          title: 'Danger zone',
          stack: true,
          render: () => (
            <SettingsSection
              title="Danger zone"
              description="These actions affect everyone in the workspace."
              tone="danger"
            >
              <SettingsActionRow
                title="Transfer ownership"
                description="Make another admin the owner. You stay an admin."
                action={<Button>Transfer</Button>}
              />
              <SettingsActionRow
                title="Delete workspace"
                description="Removes every project, file and member. This can’t be undone."
                action={<Button variant="danger">Delete workspace</Button>}
              />
            </SettingsSection>
          ),
        },
        {
          title: 'Loading and no access',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: '1rem', width: '100%' }}>
              <SettingsSection title="Payroll" state="loading">
                {null}
              </SettingsSection>
              <SettingsSection
                title="Security"
                state="no-access"
                noAccessDescription="Ask a workspace admin to change security settings."
              >
                {null}
              </SettingsSection>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass exactly one control per SettingsRow.',
        'Derive `dirty` from the draft and the saved value, so Discard and Save stay honest.',
        'Use `tone="danger"` with SettingsActionRow for irreversible actions, each behind a ConfirmDialog.',
      ],
      dont: [
        'Do not wrap the control in another Field.',
        'Do not mix destructive and ordinary settings in one section.',
        'Do not add a second Save button inside the rows.',
      ],
    },
    related: {
      items: [
        { id: 'form-field', reason: 'The wiring each row uses.' },
        { id: 'switch-settings-list', reason: 'Lists of switches that apply immediately.' },
        { id: 'section-card', reason: 'A titled card for content that is not settings.' },
        { id: 'confirm-dialog', reason: 'Confirm the actions in a danger zone.' },
        { id: 'page-header', reason: 'Title of the settings page.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'empty-state', 'form-field', 'loading-state'],
    },
    testing: {
      unit: [
        'Section is a region named by its title; rows wire label, description and required to the control; an error marks it invalid.',
        'Danger tone is exposed as data-tone.',
        'The save footer stays disabled until dirty, announces unsaved changes, discards and saves (keyboard).',
        'Saving shows a busy Save and blocks Discard; custom labels, sticky footer and extra actions.',
        'Loading and no-access states replace the rows and the footer.',
        'SettingsActionRow pairs a consequence with its button.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/settings-section',
        'Cross-browser axe: /components/settings-section',
      ],
    },
  },
} satisfies ComponentDocs;
