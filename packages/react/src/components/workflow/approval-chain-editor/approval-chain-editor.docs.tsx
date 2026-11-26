import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import {
  ApprovalChainEditor,
  summarizeApprovalChain,
  validateApprovalChain,
  type ApprovalApproverType,
  type ApprovalChainStep,
} from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const TYPES: ApprovalApproverType[] = [
  { value: 'manager', label: 'Reporting manager' },
  { value: 'manager-2', label: 'Manager’s manager' },
  { value: 'head', label: 'Department head' },
  {
    value: 'role',
    label: 'Anyone with a role',
    targetLabel: 'Role',
    targets: [
      { value: 'finance', label: 'Finance team' },
      { value: 'people', label: 'People team' },
    ],
  },
  {
    value: 'person',
    label: 'A named person',
    targetLabel: 'Person',
    targets: [
      { value: 'p1', label: 'Jo Park' },
      { value: 'p2', label: 'Ana Ruiz' },
    ],
  },
];

function ChainDemo() {
  const [steps, setSteps] = useState<ApprovalChainStep[]>([
    { id: '1', approver: 'manager' },
    { id: '2', approver: 'role', label: 'Finance check' },
  ]);
  const [tried, setTried] = useState(false);
  const [saved, setSaved] = useState('');
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%', maxWidth: 640 }}>
      <ApprovalChainEditor
        value={steps}
        onChange={setSteps}
        approverTypes={TYPES}
        showErrors={tried}
        description="Each step is decided by one of the people it names. A step whose approver is the requester is skipped."
      />
      <div>
        <Button
          variant="primary"
          onClick={() => {
            setTried(true);
            if (Object.keys(validateApprovalChain(steps, TYPES)).length === 0)
              setSaved(summarizeApprovalChain(steps, TYPES));
          }}
        >
          Save chain
        </Button>
      </div>
      <p role="status" style={{ margin: 0 }}>
        {saved ? `Saved: ${saved}` : ''}
      </p>
    </div>
  );
}

export default {
  id: 'approval-chain-editor',
  title: 'ApprovalChainEditor',
  summary:
    'Edits the ordered steps of an approval chain: who approves each step, an optional label, and the order.',
  sections: {
    purpose: {
      summary:
        'Lets an administrator define who approves a kind of request, step by step, with add, remove and accessible reordering. Needed by: People (approval chains for leave, expenses, timesheets and compensation).',
      useWhen: [
        'An admin sets up multi-step approval for requests (leave, expenses, purchases, access).',
        'Each step names a kind of approver, sometimes with a role or person to choose.',
      ],
      avoidWhen: [
        'You are showing how one request moved through its chain: use ApprovalTimeline.',
        'Rules decide what happens based on conditions: use RuleBuilder.',
      ],
    },
    designPrinciples: {
      items: [
        'Approver kinds are data (`approverTypes`): the product defines them, and which need a role or person.',
        'Order is explicit and editable three ways: drag the handle, the keyboard, or the move buttons.',
        'Errors appear when the product asks (`showErrors`, usually after a save attempt), not while the person is still choosing.',
        'The editor owns only the steps. Name, conditions, priority and saving stay in the product’s dialog or page.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled only: `value` + `onChange`. Steps need a stable `id`; new steps get a generated one unless you pass `createStep`.',
        'Changing a step’s approver drops a `target` the new kind does not offer.',
        '`validateApprovalChain(steps, types)` returns the steps missing a target; `summarizeApprovalChain(steps, types)` writes "A → B" text for list rows.',
        'The ref is the root `<fieldset>`; `disabled` disables every control through it.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copies (People `ApprovalChainsView` ChainDialog, `TimesheetChains` ChainDialog, expenses `ChainDialog`): the steps fieldset becomes this component. `StepDef.kind` → `approver`; `roleKey` / `userId` → `target` (declare `targets` on the ROLE and USER approver types); `label` stays `label`. `KINDS` + `STEP_KIND_LABEL` → `approverTypes`. The up / down / remove buttons and the 10-step limit are built in (`maxSteps`). Keep the name, conditions, priority, deadline and active fields in the product dialog.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showLabels',
          values: ['true', 'false'],
          description: 'Show the optional label input on each step. Default true.',
        },
        {
          prop: 'minSteps / maxSteps',
          values: ['number'],
          description: 'Limits for remove and add. Defaults 1 and 10.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid step',
          attribute: 'data-invalid (step), aria-invalid (target picker)',
          description:
            'With `showErrors`, a step missing its target shows an error under the picker.',
        },
        {
          name: 'Moving',
          attribute: 'data-active (ReorderableList item)',
          description: 'A step being dragged or moved from the keyboard is raised.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled fieldset',
          description: 'Every control is disabled.',
        },
        {
          name: 'Full',
          description: 'At `maxSteps` the add button is replaced by a note.',
        },
      ],
    },
    interaction: {
      items: [
        'Add step appends a step with the first approver kind.',
        'Choosing a kind that needs a target shows the target picker next to it.',
        'Remove is disabled at `minSteps`. Removing a step from an unsaved draft needs no confirmation; confirm when the product saves a chain that requests already use.',
        'Every edit calls `onChange` with the full new list.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through each step’s handle, fields, remove and move buttons, then Add step.',
        },
        { keys: 'Space / Enter on a handle', action: 'Picks the step up or drops it.' },
        { keys: 'Arrow Up / Arrow Down', action: 'Moves a picked-up step.' },
        { keys: 'Escape', action: 'Cancels a move.' },
      ],
    },
    accessibility: {
      role: 'group (fieldset) with a legend; each step is a group named "Step n"',
      name: '`messages.legend`, "Steps, in order" by default.',
      items: [
        'Every picker and input sits in a Field with a visible label.',
        'Handles and move buttons are named with the step position and its summary; moves are announced by ReorderableList.',
        'Remove buttons are named "Remove step n".',
        'Target errors use FieldError, so the picker is `aria-invalid` and described by the message.',
      ],
    },
    responsive: {
      items: [
        'Fields sit side by side when there is room and stack at narrow widths; works at 320px.',
        'On coarse pointers, handles, move buttons, fields and buttons are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Semantic tokens only; the error title uses `--mn-color-danger-strong`.',
        'Forced colours underline an invalid step’s title.',
        'Motion comes from ReorderableList, which honours reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'Edit a chain', stack: true, render: () => <ChainDemo /> },
        {
          title: 'Without labels, at most three steps',
          stack: true,
          render: () => (
            <ApprovalChainEditor
              style={{ width: '100%', maxWidth: 640 }}
              value={[
                { id: '1', approver: 'manager' },
                { id: '2', approver: 'head' },
                { id: '3', approver: 'person', target: 'p2' },
              ]}
              onChange={() => {}}
              approverTypes={TYPES}
              showLabels={false}
              maxSteps={3}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Explain in `description` what happens when an approver is missing or is the requester.',
        'Validate on save with `validateApprovalChain` and pass `showErrors`.',
        'Show the summary (`summarizeApprovalChain`) in the list of chains.',
      ],
      dont: [
        'Do not put chain conditions in step labels; keep conditions in their own fields.',
        'Do not allow a chain with no steps.',
      ],
    },
    related: {
      items: [
        { id: 'approval-timeline', reason: 'How one request moved through its chain.' },
        { id: 'approval-queue', reason: 'Requests waiting on the viewer.' },
        { id: 'reorderable-list', reason: 'The reordering it is built on.' },
        { id: 'rule-builder', reason: 'Condition-and-action rules rather than approval steps.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'form-field', 'text-field', 'reorderable-list'],
    },
    testing: {
      unit: [
        'Steps render as named groups with labelled pickers; axe passes.',
        'Changing the approver calls `onChange` and drops an unused target.',
        'Add and remove respect `minSteps` and `maxSteps`.',
        'Reordering with move buttons and the keyboard handle.',
        'Target errors only with `showErrors`; validate and summarise helpers; disabled; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
