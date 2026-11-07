import { useEffect, useState } from 'react';
import {
  AiMessageView,
  AiPlan,
  type AiMessage,
  type AiPlanDecision,
  type AiPlanPart,
  type AiPlanStep,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PLAN: AiPlanPart = {
  type: 'plan',
  steps: [
    { id: 's1', label: 'Collect last quarter’s figures', state: 'todo' },
    { id: 's2', label: 'Compare them with the forecast', state: 'todo' },
    { id: 's3', label: 'Draft a one-page summary', state: 'todo' },
  ],
};

function message(plan: AiPlanPart): AiMessage {
  return {
    id: 'm1',
    role: 'assistant',
    createdAt: '2026-03-02T10:15:00.000Z',
    agent: { name: 'Assistant' },
    parts: [{ type: 'text', text: 'Here is how I would approach it:', state: 'done' }, plan],
  };
}

/** Approve, then watch the steps run (simulated with a timer). */
function InMessageDemo() {
  const [plan, setPlan] = useState(PLAN);
  const [decision, setDecision] = useState<AiPlanDecision>('pending');

  useEffect(() => {
    if (decision !== 'approved') return;
    const next = plan.steps.findIndex((s) => s.state !== 'done');
    if (next < 0) return;
    const t = setTimeout(() => {
      setPlan({
        type: 'plan',
        steps: plan.steps.map((s, i) => ({
          ...s,
          state:
            i < next ? 'done' : i === next ? (s.state === 'active' ? 'done' : 'active') : s.state,
        })),
      });
    }, 1000);
    return () => clearTimeout(t);
  }, [decision, plan]);

  return (
    <AiMessageView
      message={message(plan)}
      showActions={false}
      renderPart={(part) =>
        part.type === 'plan' ? (
          <AiPlan
            part={part}
            decision={decision}
            onChange={(steps) => setPlan({ type: 'plan', steps })}
            onApprove={(steps: AiPlanStep[]) => {
              setPlan({ type: 'plan', steps });
              setDecision('approved');
            }}
            onReject={() => setDecision('rejected')}
          />
        ) : undefined
      }
    />
  );
}

export default {
  id: 'ai-plan',
  title: 'AiPlan',
  summary:
    'An agent’s proposed plan that a person edits, then approves or rejects, before it acts.',
  sections: {
    purpose: {
      summary:
        'Puts a person in the loop before an agent acts: it shows the steps the agent proposes, lets them reorder, edit, add or remove steps, and nothing runs until they approve. After approval it shows the same steps live as the agent works.',
      useWhen: [
        'A message has an `AiPlanPart` and the agent waits for approval before doing several things.',
        'Through `AiMessageView renderPart` for `part.type === "plan"` (AiMessageView has no default for plans).',
      ],
      avoidWhen: [
        'Approving one tool call: use AiToolApproval.',
        'Showing progress with nothing to approve: use AiChainOfSteps.',
        'A person’s own task list: use OnboardingChecklist or ReorderableList.',
      ],
    },
    designPrinciples: {
      items: [
        'Human in the loop: the copy says plainly that nothing runs until the person approves.',
        'The person can change the plan, not only accept or refuse it.',
        'Reordering never depends on dragging: keyboard pick-up and move buttons do the same.',
        'Once decided, the plan is read-only and becomes a live progress list, so the record of what was approved stays visible.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>` (role `group`); native `<div>` attributes pass through.',
        'The draft starts from `part.steps`. A new `part.steps` array from the model replaces the draft; the array you pass back after `onChange` is recognised and does not reset it.',
        '`onApprove` receives the steps with labels trimmed and empty steps dropped.',
        'Without `decision` the component remembers Approve or Reject itself; pass `decision` to drive it from conversation state.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'editable',
          values: ['true', 'false'],
          description:
            'True (default): reorder, edit, add and remove before deciding. False: the proposed steps are read-only, with Approve and Reject only.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Pending',
          attribute: 'data-decision="pending"',
          description: 'Waiting for the person. With `editable`, also `data-editable`.',
        },
        {
          name: 'Approved',
          attribute: 'data-decision="approved"',
          description:
            '"Plan approved." and a read-only AiChainOfSteps showing each step’s live state (`todo` shows as not started).',
        },
        {
          name: 'Rejected',
          attribute: 'data-decision="rejected"',
          description: '"Plan rejected." with the steps muted.',
        },
        {
          name: 'Empty',
          description:
            'Every step was removed or left blank: Approve is disabled and a line says to add a step.',
        },
      ],
    },
    interaction: {
      items: [
        'Edit a step by typing in its field; every edit calls `onChange` with the draft.',
        'Reorder by dragging the handle, by keyboard on the handle, or with the move up / down buttons.',
        'Add step appends an empty step and focuses it; Enter in a step inserts a new one after it.',
        'Remove (×) deletes a step and moves focus to the previous step, or to Add step when none are left; Backspace in an empty field does the same.',
        'Approve calls `onApprove(steps)`; Reject calls `onReject()`. Both move focus to the plan, since the buttons disappear.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through handles, step fields, move and remove buttons, Add step, Reject and Approve.',
        },
        { keys: 'Enter (in a step)', action: 'Inserts a new step after it and focuses it.' },
        {
          keys: 'Backspace (in an empty step)',
          action: 'Removes it and focuses the previous step.',
        },
        { keys: 'Space / Enter (on a handle)', action: 'Picks the step up or drops it.' },
        { keys: 'Arrow Up / Down (while picked up)', action: 'Moves the step.' },
        { keys: 'Escape (while picked up)', action: 'Cancels the move.' },
      ],
    },
    accessibility: {
      role: 'group',
      name: 'The title, default "Proposed plan"; described by the human-in-the-loop copy',
      items: [
        'Step fields are named "Step 1", "Step 2" …; remove buttons "Remove <step>"; handles "Reorder <step>".',
        'Moves, removals and the decision are announced politely; the reorder announcements come from ReorderableList.',
        'Focus never gets lost: it follows added and removed steps and lands on the plan after a decision.',
        'After approval each step’s state is read as text ("In progress", "Done") by AiChainOfSteps, never by colour alone.',
      ],
    },
    responsive: {
      items: [
        'Step rows shrink their field first; handle, move and remove buttons keep their size.',
        'Below 480px the padding tightens and Reject / Approve share the row equally.',
        'RTL: logical properties throughout.',
      ],
    },
    theme: {
      items: [
        'Card border in `--mn-color-ai-border` on `--mn-color-bg-surface`; rejected plans use `--mn-color-bg-subtle`.',
        'Buttons, fields and the reorder list use their own tokens. Forced colours use Canvas / CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Editable proposal',
          stack: true,
          render: () => <AiPlan part={PLAN} />,
        },
        {
          title: 'Read-only proposal',
          stack: true,
          render: () => <AiPlan part={PLAN} editable={false} />,
        },
        {
          title: 'Approved and running',
          stack: true,
          render: () => (
            <AiPlan
              decision="approved"
              part={{
                type: 'plan',
                steps: [
                  { id: 's1', label: 'Collect last quarter’s figures', state: 'done' },
                  { id: 's2', label: 'Compare them with the forecast', state: 'active' },
                  { id: 's3', label: 'Draft a one-page summary', state: 'todo' },
                ],
              }}
            />
          ),
        },
        {
          title: 'Plugged into AiMessageView with renderPart',
          stack: true,
          render: () => <InMessageDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Send the approved steps (from `onApprove`) back to the agent, not the original proposal.',
        'Keep updating `part.steps[].state` after approval so the list shows progress.',
      ],
      dont: [
        'Do not start acting before `onApprove`.',
        'Do not use it for a single action; AiToolApproval states the consequence of one call more clearly.',
      ],
    },
    related: {
      items: [
        { id: 'ai-chain-of-steps', reason: 'The read-only, live view it becomes once approved.' },
        { id: 'ai-tool-approval', reason: 'Approving one tool call.' },
        {
          id: 'ai-message',
          reason: 'Renders a message’s parts; plans plug in through `renderPart`.',
        },
        { id: 'reorderable-list', reason: 'Provides the drag, keyboard and button reordering.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'text-field', 'reorderable-list', 'ai-chain-of-steps'],
    },
    testing: {
      unit: [
        'Renders a named group described by the human-in-the-loop copy, with one named field per step; ref and props forwarding.',
        'Inline edits report the draft through `onChange`.',
        'Reorders with move buttons and the keyboard handle.',
        'Adds a step with the button and with Enter, focusing it; removes with the button and Backspace, announcing it and moving focus.',
        'Approve passes cleaned steps, becomes a read-only list and takes focus; Reject; Approve disabled for an empty plan.',
        'Read-only when not editable; controlled decision shows live step states.',
        'A new plan from the model replaces the draft; its own echo does not. Translated strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
