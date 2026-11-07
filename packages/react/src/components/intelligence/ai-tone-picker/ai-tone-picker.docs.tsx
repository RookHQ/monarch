import { useState } from 'react';
import {
  AiPromptInput,
  AiPromptToolbar,
  AiTonePicker,
  AI_TONE_CUSTOM,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function CardsDemo() {
  const [tone, setTone] = useState('professional');
  const [custom, setCustom] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiTonePicker
        value={tone}
        onChange={setTone}
        allowCustom
        customText={custom}
        onCustomTextChange={setCustom}
      />
      <p style={{ margin: 0 }}>
        Selected: {tone === AI_TONE_CUSTOM ? `custom (${custom || 'not described yet'})` : tone}
      </p>
    </div>
  );
}

function CompactDemo() {
  const [tone, setTone] = useState('casual');
  const [sent, setSent] = useState('');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', width: '100%', maxWidth: '40rem' }}>
      <AiPromptInput
        label="Describe what to write"
        placeholder="A reply to the customer about the delay"
        onSubmit={(text) => setSent(`${text} (tone: ${tone})`)}
        toolbar={
          <AiPromptToolbar>
            <AiTonePicker variant="compact" value={tone} onChange={setTone} />
          </AiPromptToolbar>
        }
      />
      {sent ? <p style={{ margin: 0 }}>Sent: {sent}</p> : null}
    </div>
  );
}

export default {
  id: 'ai-tone-picker',
  title: 'AiTonePicker',
  summary:
    'Chooses the tone the assistant writes in, with descriptions and an optional custom tone.',
  sections: {
    purpose: {
      summary:
        'Lets people pick how generated text should sound: Professional, Technical, Casual or Executive by default, each explained in one line, or a tone described in their own words.',
      useWhen: [
        'The assistant drafts or rewrites text for someone else to read.',
        'A rewrite action needs a tone before it runs.',
      ],
      avoidWhen: [
        'Choosing what the assistant may do: use AiModeSwitcher.',
        'Choosing the model: use AiModelPicker.',
        'Formatting (length, bullet points): use separate controls or a template.',
      ],
    },
    designPrinciples: {
      items: [
        'Explain each choice: every tone has a one-line description, read with the option.',
        'Native controls: real radio buttons in a fieldset, or a native select in compact form.',
        'Room for nuance: the custom option takes the person’s own words instead of adding more presets.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        '`AI_DEFAULT_TONES` holds the four default tones; pass `tones` to replace them.',
        'With `allowCustom`, the value of the custom option is `AI_TONE_CUSTOM` (`"custom"`); its text is `customText` (controlled) or `defaultCustomText`.',
        '`AI_TONE_PICKER_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['cards', 'compact'],
          description:
            '`cards`: radio cards with descriptions. `compact`: a small native select with an inline label, for toolbars. Exposed as `data-variant`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Selected',
          attribute: 'data-state="on" on the card, data-tone on the root',
          description: 'AI-subtle fill and a doubled AI border.',
        },
        {
          name: 'Custom',
          description: 'The custom option is selected: a "Describe the tone" field appears below.',
        },
        {
          name: 'Disabled',
          description: 'The fieldset (or select) and the custom field are disabled.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a card or its text selects the tone and calls `onChange(id)`.',
        'Choosing Custom shows the text field; typing calls `onCustomTextChange(text)`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the selected tone, then to the custom field when shown.' },
        { keys: 'Arrow keys', action: 'Select the next or previous tone (native radio group).' },
        { keys: 'Arrow keys / Enter (compact)', action: 'The native select’s keys.' },
      ],
    },
    accessibility: {
      role: 'group (fieldset) of radios, or a labelled select',
      name: '"Tone" (messages.label) from the legend or label',
      items: [
        'Each radio is named by its tone and described by its description.',
        'The custom field has a visible label, "Describe the tone".',
        'The selected card is marked by a doubled border as well as colour.',
      ],
    },
    responsive: {
      items: [
        'Cards flow into as many 11rem columns as fit, down to one column at 390px.',
        'The compact variant wraps the custom field onto its own line when needed.',
        'On coarse pointers cards are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Cards: `--mn-color-bg-surface`, `--mn-color-border-default` (hover `--mn-color-border-strong`).',
        'Selected: `--mn-color-ai-subtle`, `--mn-color-ai-border`; radios use `--mn-color-ai-solid`.',
        'In forced colours the selected card has a Highlight border.',
      ],
    },
    examples: {
      items: [
        { title: 'Cards with a custom tone', stack: true, render: () => <CardsDemo /> },
        { title: 'Compact, in a prompt toolbar', stack: true, render: () => <CompactDemo /> },
      ],
    },
    doDont: {
      do: [
        'Keep the list to about four tones and describe each.',
        'Remember the last tone per person or per task.',
        'Send the custom text to the model as an instruction, not as a label.',
      ],
      dont: [
        'Do not use tone names that are judgements of people.',
        'Do not hide the descriptions in the cards variant.',
        'Do not change the tone without the person choosing it.',
      ],
    },
    related: {
      items: [
        { id: 'ai-prompt-toolbar', reason: 'Where the compact variant sits.' },
        { id: 'ai-mode-switcher', reason: 'Choosing what the assistant may do.' },
        { id: 'ai-prompt-template-picker', reason: 'Reusable prompts that can include a tone.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['form-field', 'text-field'] },
    testing: {
      unit: [
        'Default tones as a named radio group with descriptions; props and ref.',
        'Arrow keys and clicks select; the custom option shows a labelled field and reports its text.',
        'Custom tones, controlled custom text, disabled, messages.',
        'Compact select and its custom field.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
