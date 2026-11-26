import { useState } from 'react';
import { AiModelPicker, type AiModelOption } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const models: AiModelOption[] = [
  {
    id: 'standard',
    provider: 'Workspace models',
    name: 'Standard',
    description: 'Everyday questions and drafts',
    capabilities: ['tools', 'web'],
    contextTokens: 128000,
    speed: 'fast',
    costTier: 1,
  },
  {
    id: 'advanced',
    provider: 'Workspace models',
    name: 'Advanced',
    description: 'Long documents and careful analysis',
    capabilities: ['vision', 'tools', 'reasoning', 'web'],
    contextTokens: 200000,
    speed: 'slow',
    costTier: 3,
  },
  {
    id: 'own-large',
    provider: 'Your provider',
    name: 'Large (your key)',
    capabilities: ['vision', 'tools'],
    contextTokens: 1000000,
    speed: 'balanced',
    costTier: 2,
    requiresKey: true,
    keyConfigured: false,
  },
  {
    id: 'own-small',
    provider: 'Your provider',
    name: 'Small (your key)',
    capabilities: ['tools'],
    contextTokens: 32000,
    speed: 'fast',
    costTier: 1,
    requiresKey: true,
    keyConfigured: true,
  },
];

function Demo() {
  const [value, setValue] = useState('standard');
  const [note, setNote] = useState('');
  return (
    <div style={{ display: 'grid', gap: 8, justifyItems: 'start' }}>
      <AiModelPicker
        models={models}
        value={value}
        onChange={(id) => {
          setValue(id);
          setNote('');
        }}
        onConfigureKey={(model) => setNote(`Open key settings for ${model.name}`)}
      />
      {note ? <p style={{ margin: 0 }}>{note}</p> : null}
    </div>
  );
}

export default {
  id: 'ai-model-picker',
  title: 'AiModelPicker',
  summary: 'Choose the model that answers, grouped by provider, with capabilities and key state.',
  sections: {
    purpose: {
      summary:
        'Lets people choose which model answers in an assistant or a feature, with enough information to choose well: capabilities, context size, speed, relative cost, and whether a bring-your-own provider key is set up.',
      useWhen: [
        'A product offers more than one model and people may reasonably choose.',
        'Workspaces can bring their own provider key and need to see which models depend on it.',
      ],
      avoidWhen: [
        'Switching how the assistant behaves (ask, edit, agent): use AiModeSwitcher.',
        'Choosing an agent with its own instructions and tools: use AiAgentPicker.',
        'Products with a single model: do not show a picker.',
      ],
    },
    designPrinciples: {
      items: [
        'Compact trigger, rich list: the composer stays small; detail appears only when choosing.',
        'Capabilities are words, not icons alone; cost is shown as $ to $$$ and read as “Cost: low/medium/high”.',
        'A model with a missing key is visible but leads to key setup, never to a broken answer.',
        'Grouped by provider in the order given; nothing is re-sorted behind people’s backs.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref and native button attributes go to the trigger `<button>`.',
        'Built on Popover (Radix); the list is a single-select `listbox` with `aria-activedescendant`.',
        '`messages.capabilities` and `messages.speed` merge with the defaults, so you can override one label.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md'],
          description:
            '`md` (default) for toolbars; `sm` inside a prompt toolbar. Exposed as `data-size`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'aria-expanded',
          description: 'On the trigger while the list is open.',
        },
        {
          name: 'Selected',
          attribute: 'aria-selected',
          description: 'On the current model’s option, with a check mark.',
        },
        {
          name: 'Highlighted',
          attribute: 'data-active',
          description: 'The keyboard or pointer position in the list.',
        },
        {
          name: 'Key state',
          attribute: 'data-state',
          description:
            '`ok`; `key` (missing key, routes to `onConfigureKey`); `blocked` (`disabled`, or missing key with no `onConfigureKey`; also `aria-disabled`).',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking the trigger opens the list with the current model highlighted.',
        'Picking a model calls `onChange` and closes the list; focus returns to the trigger.',
        'Picking a model whose key is missing calls `onConfigureKey` instead.',
        'Clicking outside or pressing Escape closes without changing anything.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space / ArrowDown', action: 'On the trigger: opens the list.' },
        { keys: 'ArrowDown / ArrowUp', action: 'Moves the highlight.' },
        { keys: 'Home / End', action: 'First / last model.' },
        {
          keys: 'Letters',
          action: 'Jumps to the next model whose name starts with the typed text.',
        },
        { keys: 'Enter / Space', action: 'Picks the highlighted model.' },
        { keys: 'Escape', action: 'Closes and returns focus to the trigger.' },
        { keys: 'Tab', action: 'Closes the list and moves on.' },
      ],
    },
    accessibility: {
      role: 'button (aria-haspopup="listbox") opening a listbox',
      name: 'Trigger: “Model” plus the visible model and provider. List: “Model”.',
      items: [
        'Providers are `group`s named by their heading.',
        'Each option is named by the model and its key badge; capabilities, context, speed and cost are its description.',
        'Options that cannot be picked are `aria-disabled` but still reachable, so people learn why.',
        'The visible trigger text is part of its accessible name (label in name).',
      ],
    },
    responsive: {
      items: [
        'The trigger truncates long names; the list is at most 22rem and never wider than the screen minus 2rem.',
        'The list scrolls within the available height.',
        'Trigger and options are 44px tall on coarse pointers. Logical properties for RTL.',
      ],
    },
    theme: {
      items: [
        'Trigger: `--mn-color-bg-surface`, `--mn-color-border-input`.',
        'Selected option: `--mn-color-ai-subtle` with an `--mn-color-ai-strong` check.',
        'Key needed: warning tokens; your key: success tokens.',
        'Forced colours: Highlight outline on the highlighted option, GrayText for unavailable ones.',
      ],
    },
    examples: {
      items: [
        { title: 'Models with bring-your-own keys', render: () => <Demo /> },
        {
          title: 'Small, in a prompt toolbar',
          render: () => (
            <AiModelPicker
              size="sm"
              models={models.slice(0, 2)}
              value="advanced"
              onChange={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Describe each model by what it is good for.',
        'Pass `onConfigureKey` when people can set up keys, and take them straight to that setting.',
      ],
      dont: [
        'Do not list models people cannot use without saying why.',
        'Do not change the selected model silently when a key is removed; show the state instead.',
      ],
    },
    related: {
      items: [
        { id: 'ai-mode-switcher', reason: 'How the assistant behaves, not which model.' },
        { id: 'ai-agent-picker', reason: 'Choosing an agent.' },
        { id: 'ai-prompt-toolbar', reason: 'Where a small picker usually sits.' },
        { id: 'ai-settings-section', reason: 'Where provider keys are configured.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['popover'] },
    testing: {
      unit: [
        'Trigger name includes the visible model; ref and props forwarding.',
        'Grouped listbox with option names, descriptions and key badges.',
        'Arrow, Home, End and typeahead navigation; Enter picks and returns focus.',
        'Missing-key models are not picked and route to `onConfigureKey`.',
        'Escape closes; placeholder and localised strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
