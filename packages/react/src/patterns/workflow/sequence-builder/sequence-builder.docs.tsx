import { useState } from 'react';
import {
  SequenceBuilder,
  type SequenceChannel,
  type SequenceStep,
  type SequenceVariable,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const CHANNELS: SequenceChannel[] = [
  { value: 'email', label: 'Email', hasSubject: true },
  { value: 'chat', label: 'Chat message', maxLength: 1000 },
];

const VARIABLES: SequenceVariable[] = [
  { token: 'firstName', label: 'First name', sample: 'Sam' },
  { token: 'product.name', label: 'Product name', sample: 'Atlas' },
  { token: 'product.url', label: 'Product link', sample: 'https://example.com' },
];

function SequenceDemo() {
  const [steps, setSteps] = useState<SequenceStep[]>([
    {
      id: '1',
      channel: 'email',
      delayMinutes: 0,
      subject: 'A quick intro to {{product.name}}',
      body: 'Hi {{firstName}},\n\nHere is {{product.name}}: {{product.url}}.\n\nWorth a short look?',
    },
    {
      id: '2',
      channel: 'chat',
      delayMinutes: 2880,
      body: 'Hi {{firstName}}, a gentle nudge about {{product.name}}.',
    },
  ]);
  return (
    <SequenceBuilder
      style={{ width: '100%', maxWidth: 720 }}
      value={steps}
      onChange={setSteps}
      channels={CHANNELS}
      variables={VARIABLES}
    />
  );
}

export default {
  id: 'sequence-builder',
  title: 'SequenceBuilder',
  summary:
    'Builds a multi-step message sequence: per-step wait, channel, subject and message, with template variables and a sample preview.',
  sections: {
    purpose: {
      summary:
        'Lets people design an outreach or follow-up sequence: wait, send, wait, send, across channels, with personalised variables. Needed by: GTM (campaign builder).',
      useWhen: [
        'Messages are sent in order with delays between them.',
        'Messages use variables such as the recipient’s first name.',
      ],
      avoidWhen: [
        'Actions run at once when a rule matches: use ActionEditor or RuleBuilder.',
        'People are alerted in levels when nobody responds: use EscalationPolicyEditor.',
      ],
    },
    designPrinciples: {
      items: [
        'Each step reads as a sentence: "Sent 2d after the previous step".',
        'Waits are durations people can type naturally ("2d", "4h"), reusing DurationInput.',
        'Variables are inserted where the caret is, checked against the known list and previewed with sample values.',
        'Channels are data: the product decides which have a subject line and a length limit.',
        'The builder never sends anything; the product schedules and sends.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: `value` + `onChange`. A step is `{ id, channel, delayMinutes, subject?, body }`.',
        'Helpers: `findTemplateVariables`, `renderSequenceTemplate`, `validateSequence`.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (GTM `CampaignBuilder`): `steps` → `value` (add `id`, drop `order`: the array order is the order), `delayHours` → `delayMinutes` (× 60), `channel` EMAIL / WHATSAPP → `channels` entries (email with `hasSubject`), the hard-coded variable hints → `variables`, `canCreate` → `validateSequence`. Keep the product picker, name, notes, space selector, modal and save in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'SequenceChannel.hasSubject',
          values: ['true', 'false'],
          description: 'Show a subject line for the channel (email).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid step',
          attribute: 'data-invalid',
          description: 'With `showErrors`, missing subject or message, or a too-long message.',
        },
        {
          name: 'Unknown variables',
          description: 'A warning lists tokens that are not in `variables`.',
        },
        { name: 'Empty', description: 'EmptyState with add buttons below.' },
        { name: 'Full', description: 'At `maxSteps` the add buttons become a note.' },
      ],
    },
    interaction: {
      items: [
        'Add buttons append a step on that channel; the first step is sent straight away, later ones wait `defaultDelayMinutes`.',
        'Variable buttons insert `{{token}}` at the caret of the field last focused in that step and return focus there.',
        'The preview expands to show the message with sample values.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through each step’s handle, remove, wait, subject, message, variables, preview and move buttons.',
        },
        { keys: 'Arrow Up / Down in the wait', action: 'Steps the wait by an hour.' },
        { keys: 'Enter / Space on the preview', action: 'Opens or closes it.' },
        {
          keys: 'Space / Enter on a handle',
          action: 'Picks the step up or drops it; arrows move it.',
        },
      ],
    },
    accessibility: {
      role: 'list; each step is a group named "Step n, channel"',
      name: '`messages.label`, "Sequence steps" by default.',
      items: [
        'Every input has a visible label; errors are linked with FieldError.',
        'Variable buttons are a group; each is named "Insert <variable>".',
        'The preview is a native disclosure (details / summary).',
      ],
    },
    responsive: {
      items: [
        'Single column; the wait input takes the full width on phones. Works at 320px.',
        'On coarse pointers, buttons, handles, inputs and the preview toggle are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only; channel icons sit on `--mn-color-accent-subtle`; warnings use the warning tokens.',
        'Forced colours outline icons, warnings and previews.',
        'Nothing animates here; reordering honours reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Two-step outreach', stack: true, render: () => <SequenceDemo /> },
        {
          title: 'Empty',
          render: () => <SequenceBuilder value={[]} onChange={() => {}} channels={CHANNELS} />,
        },
      ],
    },
    doDont: {
      do: [
        'Give every variable a sample so the preview reads naturally.',
        'Validate with `validateSequence` before saving.',
        'Explain in the product whether edits affect messages already scheduled.',
      ],
      dont: [
        'Do not put sending or scheduling logic in the builder.',
        'Do not allow variables the sender cannot fill.',
      ],
    },
    related: {
      items: [
        { id: 'action-editor', reason: 'Actions that run at once.' },
        { id: 'escalation-policy-editor', reason: 'Levels of alerts when nobody responds.' },
        { id: 'duration-input', reason: 'The wait input.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [
        'button',
        'duration-input',
        'empty-state',
        'form-field',
        'reorderable-list',
        'text-field',
      ],
    },
    testing: {
      unit: [
        'Steps render as named groups with wait, subject and message; axe passes.',
        'A variable is inserted at the caret and focus returns.',
        'Add per channel, remove and reorder; the wait is edited as a duration.',
        'Problems and unknown variables; sample preview.',
        'Helpers; empty state; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
