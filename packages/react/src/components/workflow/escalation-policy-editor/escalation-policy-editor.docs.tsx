import { useState } from 'react';
import {
  EscalationPolicyEditor,
  type EscalationLevel,
  type EscalationTarget,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const TARGETS: EscalationTarget[] = [
  { value: 'oncall', label: 'On-call rota' },
  { value: 'chat', label: 'Team chat', description: 'Chat channel' },
  { value: 'email', label: 'Operations email', description: 'Email list' },
  { value: 'lead', label: 'Team lead', description: 'Person' },
];

function PolicyDemo() {
  const [levels, setLevels] = useState<EscalationLevel[]>([
    { id: '1', afterMinutes: 5, targets: ['oncall'] },
    { id: '2', afterMinutes: 20, targets: ['chat', 'lead'] },
    { id: '3', afterMinutes: 60, targets: [] },
  ]);
  return (
    <EscalationPolicyEditor
      style={{ width: '100%', maxWidth: 640 }}
      value={levels}
      onChange={setLevels}
      targets={TARGETS}
      showErrors
      description="Each level fires only if nobody has acknowledged yet. Times are measured from when the incident opened."
    />
  );
}

export default {
  id: 'escalation-policy-editor',
  title: 'EscalationPolicyEditor',
  summary: 'Edits an escalation ladder: after how long each level fires and whom it notifies.',
  sections: {
    purpose: {
      summary:
        'Makes sure an alert or request nobody acts on reaches someone else: level by level, after a time, notify these channels or people. Needed by: Pulse (incident escalation policies), People (approval deadline escalation, with `timing="interval"`).',
      useWhen: [
        'Unacknowledged incidents or overdue approvals must move to other people over time.',
        'Each level notifies one or more channels, rotas or people.',
      ],
      avoidWhen: [
        'Messages go to customers on a schedule: use SequenceBuilder.',
        'Steps are approvals in order: use ApprovalChainEditor.',
      ],
    },
    designPrinciples: {
      items: [
        'A level that notifies nobody is an error: escalation exists to prevent silence.',
        'Each level shows a plain summary ("After 20m, notify Team chat, Team lead").',
        'Times are durations typed naturally; with cumulative timing each level must be later than the previous one.',
        'When nothing can be notified yet the editor says so up front.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: `value` + `onChange`. A level is `{ id, afterMinutes, targets }`.',
        '`timing="cumulative"` (default) measures from the start; `interval` measures from the previous level.',
        '`validateEscalationPolicy(levels, timing)` returns problems per level id.',
        'The ref is the root `<div>`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copies: Pulse `PolicyModal` `steps` → `value` (add `id`), `channelLabels` → `targets` (use channel ids as values), `channels` → `targets` prop, the "pick at least one channel" check → `validateEscalationPolicy` with `showErrors`, the no-channels warning is built in. Keep the name, severity picker, modal and save in the product. People `EscalationSettings`: `maxEscalationLevels` + `levelIntervalHours` + `escalateTo` become levels with `timing="interval"` and targets such as "Next manager", "HR", "Workspace owner"; deadlines and reminders stay as product fields.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'timing',
          values: ['cumulative', 'interval'],
          description: 'Measure each time from the start (default) or from the previous level.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid level',
          attribute: 'data-invalid',
          description: 'With `showErrors`: no targets, or not later than the level before.',
        },
        { name: 'No targets available', description: 'A warning note above the ladder.' },
        { name: 'Full', description: 'At `maxLevels` the add button becomes a note.' },
        { name: 'Disabled', attribute: 'data-disabled', description: 'Every control is disabled.' },
      ],
    },
    interaction: {
      items: [
        'Add level appends one `defaultStepMinutes` after the last.',
        'Ticking a target adds it in the order of `targets`, so summaries read consistently.',
        'Remove is disabled at `minLevels`.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves through each level’s remove button, time and targets, then Add level.',
        },
        { keys: 'Arrow Up / Down in a time', action: 'Steps by 5 minutes.' },
        { keys: 'Space', action: 'Toggles a target.' },
      ],
    },
    accessibility: {
      role: 'ordered list; each level is a group named "Level n"',
      name: '`messages.label`, "Escalation levels" by default.',
      items: [
        'Targets are a fieldset with the legend "Notify".',
        'A level’s problem is linked to its group with `aria-describedby`.',
        'Level numbers are decorative; the group name carries them.',
      ],
    },
    responsive: {
      items: [
        'Targets flow into columns and stack on phones; the time input takes the full width. Works at 320px.',
        'On coarse pointers, targets, inputs and buttons are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only; markers use `--mn-color-bg-emphasis` with `--mn-color-text-inverse`.',
        'Forced colours draw borders, markers and the rail in system colours.',
        'Nothing animates.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Incident escalation with a problem shown',
          stack: true,
          render: () => <PolicyDemo />,
        },
        {
          title: 'Nothing to notify yet',
          stack: true,
          render: () => (
            <EscalationPolicyEditor
              style={{ width: '100%', maxWidth: 640 }}
              value={[{ id: '1', afterMinutes: 15, targets: [] }]}
              onChange={() => {}}
              targets={[]}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Say in `description` what acknowledges an incident and where times are measured from.',
        'Validate before saving with `validateEscalationPolicy`.',
      ],
      dont: ['Do not save levels without targets.', 'Do not fire every level at the same time.'],
    },
    related: {
      items: [
        { id: 'sla-indicator', reason: 'Shows how close something is to its deadline.' },
        { id: 'approval-chain-editor', reason: 'Approval steps in order.' },
        { id: 'sequence-builder', reason: 'Scheduled messages to customers.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'checkbox', 'duration-input', 'form-field'],
    },
    testing: {
      unit: [
        'Ladder, summaries and labelled controls render; axe passes.',
        'Targets toggle in target order (keyboard and pointer); time edits as a duration.',
        'Add and remove within limits.',
        'Problems with `showErrors`, linked to the level; warning without targets.',
        'Cumulative and interval validation; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
