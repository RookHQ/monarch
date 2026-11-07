import { useState } from 'react';
import { AiSettingsSection, type AiSettingsValue } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const APPS = [
  { id: 'docs', label: 'Documents', description: 'Summaries, drafts and answers about pages.' },
  { id: 'mail', label: 'Mail', description: 'Reply suggestions and thread summaries.' },
  { id: 'calendar', label: 'Calendar' },
];
const PROVIDERS = [
  { id: 'provider-a', label: 'Provider A' },
  { id: 'provider-b', label: 'Provider B' },
];

function Demo({ readOnly = false }: { readOnly?: boolean }) {
  const [value, setValue] = useState<AiSettingsValue>({
    apps: { docs: true, mail: false, calendar: true },
    dataSharing: false,
    memory: true,
    retention: '90d',
    provider: '',
  });
  return (
    <div style={{ width: '100%' }}>
      <AiSettingsSection
        value={value}
        apps={APPS}
        providers={PROVIDERS}
        readOnly={readOnly}
        onSave={async ({ apiKey, ...draft }) => {
          await new Promise((r) => setTimeout(r, 600));
          setValue({ ...draft, keyConfigured: Boolean(apiKey) || value.keyConfigured });
        }}
      />
    </div>
  );
}

export default {
  id: 'ai-settings-section',
  title: 'AiSettingsSection',
  summary:
    'Workspace AI settings: per-app switches, data sharing, memory, retention and your own key.',
  sections: {
    purpose: {
      summary:
        'Admins need one predictable place to decide where AI runs and what happens to the data. AiSettingsSection lays out the common workspace AI settings in a settings card and keeps every change a draft until Save, so nothing changes by accident.',
      useWhen: [
        'Workspace or organisation settings for AI features.',
        'A product offers bring-your-own provider keys.',
      ],
      avoidWhen: [
        'Personal preferences that should apply at once (use SwitchSettingsList).',
        'Per-agent permissions (use AiPermissionScope).',
      ],
    },
    designPrinciples: {
      items: [
        'Nothing is saved without an explicit Save; Discard always returns to the saved settings.',
        'Off by default is honest: data sharing and per-app switches show their real saved state.',
        'Keys are write-only: a saved key is shown as set, never sent back to the page.',
        'Looks like every other settings card (same layout as SettingsSection).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`.',
        '`onSave` receives the whole draft; `apiKey` is present only when a new key was typed for a chosen provider.',
        'Leave out `apps` or `providers`, or set `show`, to hide rows the product does not offer.',
        'Lives in ./ai as a component, so it reproduces SettingsSection’s layout instead of importing the pattern.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'No visual variants. `readOnly` shows the same rows disabled for people who cannot change them.',
      ],
    },
    states: {
      items: [
        { name: 'Saved', description: 'The draft equals `value`; Save and Discard are disabled.' },
        { name: 'Dirty', attribute: 'data-dirty', description: '“You have unsaved changes.”' },
        {
          name: 'Saving',
          attribute: 'aria-busy',
          description: 'Save shows a spinner; controls are disabled.',
        },
        {
          name: 'Saved message',
          attribute: 'data-status="saved"',
          description: '“Settings saved.”',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'The draft is kept and the error is shown in the footer.',
        },
        { name: 'Read-only', attribute: 'data-read-only', description: 'Disabled, no footer.' },
      ],
    },
    interaction: {
      items: [
        'Switches, selects and the key field change the draft only.',
        'Toggling a setting back to its saved value clears the dirty state.',
        'Choosing a provider reveals its key field; choosing Built-in hides it and drops the typed key.',
        'Save calls `onSave` and waits for its promise; a rejection keeps the draft.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through the switches, selects, key field and footer buttons.',
        },
        { keys: 'Space', action: 'Toggles the focused switch.' },
        { keys: 'Arrow keys', action: 'Change the focused select’s value.' },
      ],
    },
    accessibility: {
      role: 'region (section named by its heading)',
      name: 'The title heading; the description describes it.',
      items: [
        'Per-app switches are a group named “Use AI in”, each switch named by its app.',
        'Every other control sits in a Field with a label and description.',
        'The footer status is a polite live region (unsaved, saved, error).',
        'The Bot glyph beside the title is decorative.',
      ],
    },
    responsive: {
      items: [
        'Heading beside the card from 1024px; stacked below.',
        'Rows put the control under the text on narrow screens; nothing scrolls horizontally at 390px.',
        'The footer sticks to the bottom while there are unsaved changes; buttons are 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Card surface and border tokens, `--mn-color-accent-subtle` footer while dirty, `--mn-color-ai-strong` for the glyph.',
        'Forced colours draw the card and dividers with CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'Workspace AI settings', stack: true, render: () => <Demo /> },
        { title: 'Read-only for members', stack: true, render: () => <Demo readOnly /> },
      ],
    },
    doDont: {
      do: [
        'Guard navigation with UnsavedChangesGuard using `onDirtyChange`.',
        'Pass `keyConfigured` so people can see a key is stored.',
        'Explain in the description who can see these settings.',
      ],
      dont: [
        'Do not save on every toggle; that is what SwitchSettingsList is for.',
        'Do not send a stored key back to the page.',
      ],
    },
    related: {
      items: [
        { id: 'settings-section', reason: 'The generic settings card this matches.' },
        { id: 'switch-settings-list', reason: 'The per-app switch group.' },
        { id: 'secret-field', reason: 'The bring-your-own key field.' },
        { id: 'ai-onboarding-tour', reason: 'Points people here to turn AI off.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'form-field', 'secret-field', 'switch', 'switch-settings-list'],
    },
    testing: {
      unit: [
        'Labelled section with per-app group, switches, selects and a disabled Save.',
        'Changes stay a draft until Save; Save sends the full draft and shows “Settings saved.”.',
        'Toggling back clears dirty; Discard restores the saved settings.',
        'The key field appears for a provider and the key is sent only when typed.',
        'A failed save keeps the draft and shows the error.',
        'Read-only and hidden rows.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
