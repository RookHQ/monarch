import { useState } from 'react';
import {
  AiModeSwitcher,
  AiPromptInput,
  AiPromptToolbar,
  type AiMode,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { PencilIcon, SearchIcon, GearIcon } from '../../../internal/docs-demo.js';

const MODES: AiMode[] = [
  { id: 'ask', label: 'Ask', description: 'Answers questions. Changes nothing.' },
  { id: 'edit', label: 'Edit', description: 'Proposes edits you review before they apply.' },
  {
    id: 'agent',
    label: 'Agent',
    description: 'Works through steps and uses tools. Asks before anything risky.',
  },
];

function Demo() {
  const [mode, setMode] = useState('ask');
  return <AiModeSwitcher modes={MODES} value={mode} onChange={setMode} />;
}

function InPromptDemo() {
  const [mode, setMode] = useState('ask');
  const [sent, setSent] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiPromptInput
        label="Message the assistant"
        onSubmit={(text) => setSent(`${text} (mode: ${mode})`)}
        toolbar={
          <AiPromptToolbar>
            <AiModeSwitcher modes={MODES} value={mode} onChange={setMode} showDescription={false} />
          </AiPromptToolbar>
        }
        footer={MODES.find((m) => m.id === mode)?.description}
      />
      {sent ? <p style={{ margin: 0 }}>Sent: {sent}</p> : null}
    </div>
  );
}

function IconDemo() {
  const [mode, setMode] = useState('research');
  return (
    <AiModeSwitcher
      size="md"
      value={mode}
      onChange={setMode}
      modes={[
        {
          id: 'research',
          label: 'Research',
          icon: <SearchIcon />,
          description: 'Reads sources and cites them.',
        },
        {
          id: 'write',
          label: 'Write',
          icon: <PencilIcon />,
          description: 'Drafts text for you to edit.',
        },
        {
          id: 'automate',
          label: 'Automate',
          icon: <GearIcon />,
          disabled: true,
          description: 'Not set up in this workspace.',
        },
      ]}
    />
  );
}

export default {
  id: 'ai-mode-switcher',
  title: 'AiModeSwitcher',
  summary: 'Switches how the assistant works (Ask, Edit, Agent) with the consequence shown.',
  sections: {
    purpose: {
      summary:
        'Lets people choose how much the assistant may do, from answering only to editing or running tools, and always shows what the selected mode means.',
      useWhen: [
        'An assistant has 2 to 5 modes with different permissions or behaviour.',
        'In a prompt toolbar, to set the mode for the next messages.',
      ],
      avoidWhen: [
        'Choosing a model: use AiModelPicker.',
        'Choosing a writing style: use AiTonePicker.',
        'More than five modes or long names: use a select.',
      ],
    },
    designPrinciples: {
      items: [
        'Consequence first: the selected mode’s description is shown and announced, so people know what the assistant may now change.',
        'One choice, one control: a radio group with one Tab stop, built on SegmentedControl.',
        'Neutral styling: modes are a setting, not a status, so no status colours.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the wrapper; the radio group inside is a SegmentedControl.',
        'Controlled only: pass `value` and update it in `onChange(id)`.',
        '`AI_MODE_SWITCHER_MESSAGES` holds the English group name ("Mode").',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '`sm` (default) fits a prompt toolbar.',
        },
        {
          prop: 'showDescription',
          values: ['true', 'false'],
          description:
            'Hide the description when it is shown elsewhere, such as the prompt footer.',
        },
        {
          prop: 'modes[].hideLabel',
          values: ['false', 'true'],
          description: 'Icon-only mode; the label remains the name and tooltip.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'data-mode on the root',
          description: 'The selected mode id; the radio is checked.',
        },
        {
          name: 'Disabled mode',
          description: '`disabled` on a mode: shown, skipped by the arrow keys.',
        },
        {
          name: 'Disabled',
          description: '`disabled` on the switcher: every mode, e.g. while a response runs.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a mode selects it and calls `onChange(id)`.',
        'The description under the switcher updates to the selected mode.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the selected mode (one Tab stop).' },
        {
          keys: 'Arrow keys',
          action:
            'Move to and select the next or previous enabled mode, wrapping; follows text direction.',
        },
        { keys: 'Home / End', action: 'Select the first or last enabled mode.' },
      ],
    },
    accessibility: {
      role: 'radiogroup',
      name: '"Mode" (messages.label)',
      items: [
        'The group is described by the selected mode’s description (`aria-describedby`).',
        'The description is a polite live region, so arrowing to a new mode also reads what it does.',
        'Icons are decorative; icon-only modes are named by their label.',
      ],
    },
    responsive: {
      items: [
        'Below 30rem the switcher takes the full width; SegmentedControl scrolls its own items if they still do not fit, never the page.',
        'On coarse pointers each mode is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'The control uses SegmentedControl tokens; the description uses `--mn-color-text-muted`.',
        'Forced colours follow SegmentedControl; the description uses CanvasText.',
      ],
    },
    examples: {
      items: [
        { title: 'With descriptions', render: () => <Demo /> },
        { title: 'In a prompt toolbar', stack: true, render: () => <InPromptDemo /> },
        { title: 'Icons and a disabled mode', render: () => <IconDemo /> },
      ],
    },
    doDont: {
      do: [
        'Order modes from least to most capable.',
        'Write descriptions as what the assistant will and will not do.',
        'Disable the switcher while a response is running if a change would not apply.',
      ],
      dont: [
        'Do not switch modes for people based on what they type.',
        'Do not use more than five modes.',
        'Do not colour modes as statuses.',
      ],
    },
    related: {
      items: [
        { id: 'segmented-control', reason: 'The control it is built on.' },
        { id: 'ai-prompt-toolbar', reason: 'Where it usually sits.' },
        { id: 'ai-model-picker', reason: 'Choosing the model rather than the behaviour.' },
        { id: 'ai-tone-picker', reason: 'Choosing the writing style.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['segmented-control'] },
    testing: {
      unit: [
        'Named radio group described by the selected mode; disabled mode; props and ref.',
        'Arrow keys select and skip disabled modes; the description is a polite live region and updates.',
        'Hidden description, disabled switcher, messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
