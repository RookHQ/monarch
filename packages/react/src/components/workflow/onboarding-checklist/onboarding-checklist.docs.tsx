import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { OnboardingChecklist, type OnboardingStep } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function ChecklistDemo() {
  const [done, setDone] = useState<Record<string, boolean>>({ profile: true });
  const [collapsed, setCollapsed] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const finish = (id: string) => setDone((d) => ({ ...d, [id]: true }));
  const steps: OnboardingStep[] = [
    { id: 'profile', title: 'Complete your profile', done: !!done.profile },
    {
      id: 'invite',
      title: 'Invite your team',
      description: 'Work is easier to share with the people you work with.',
      done: !!done.invite,
      action: (
        <Button size="sm" onClick={() => finish('invite')}>
          Invite people
        </Button>
      ),
    },
    {
      id: 'project',
      title: 'Create your first project',
      description: 'Start from a template or a blank project.',
      done: !!done.project,
      action: (
        <Button size="sm" variant="secondary" onClick={() => finish('project')}>
          New project
        </Button>
      ),
    },
    {
      id: 'notifications',
      title: 'Choose your notifications',
      done: !!done.notifications,
      action: (
        <Button size="sm" variant="ghost" onClick={() => finish('notifications')}>
          Open settings
        </Button>
      ),
    },
  ];
  if (dismissed)
    return (
      <Button variant="secondary" onClick={() => setDismissed(false)}>
        Show the checklist again
      </Button>
    );
  return (
    <OnboardingChecklist
      style={{ width: '100%', maxWidth: 560 }}
      title="Get started"
      description="A few steps to set up your workspace."
      steps={steps}
      collapsed={collapsed}
      onCollapsedChange={setCollapsed}
      onDismiss={() => setDismissed(true)}
    />
  );
}

export default {
  id: 'onboarding-checklist',
  title: 'OnboardingChecklist',
  summary:
    'A first-run checklist with done steps, a progress summary, per-step actions, collapse and dismiss.',
  sections: {
    purpose: {
      summary:
        'Guides people through setting up a product: a short list of steps, each with a done state and an optional call to action, and a summary of how far they are.',
      useWhen: [
        'A new workspace or account has a handful of setup steps that unlock the product.',
        'Steps can be done in any order and are completed by doing the thing itself (the product ticks them from its data).',
        'People should be able to put the checklist away (collapse) or remove it for good (dismiss).',
      ],
      avoidWhen: [
        'The steps must happen in order inside one flow: use Stepper.',
        'There is one thing to do: use a callout or an empty state with an action.',
      ],
    },
    designPrinciples: {
      items: [
        'The product owns the truth: it defines the steps, decides when each is done and persists collapse and dismiss.',
        'Done is shown three ways: a filled check marker, muted title, and hidden "Done" text for screen readers.',
        'The next step stands out (`aria-current="step"`, accent marker, tinted row) so people know where to go.',
        'An action belongs only to a step that is not done; done steps lose their description and action.',
        'Progress reuses Progress, and turns success when everything is done, with a completion message.',
        'Unlike Stepper, the order is a suggestion, not a sequence the component enforces.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`steps` are `{ id, title, done, description?, action? }`. `action` is any node, typically a small Button or a link.',
        'Collapse is controlled with `collapsed` + `onCollapsedChange`, or uncontrolled with `defaultCollapsed`; `collapsible={false}` removes the toggle.',
        'The dismiss button shows only when `onDismiss` is given; the product hides the checklist and remembers it.',
        'The ref is the root `<section>`, labelled by the title; other attributes pass through.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'collapsible',
          values: ['true', 'false'],
          description:
            'Show the toggle that hides the steps (default). The title and progress stay visible.',
        },
        {
          prop: 'headingLevel',
          values: ['2', '3', '4', '5', '6'],
          description: 'Heading level of the title, to fit the page outline. Default 2.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Step done',
          attribute: 'data-done',
          description: 'Filled success marker with a check; muted title.',
        },
        {
          name: 'Current step',
          attribute: 'data-current, aria-current="step"',
          description: 'The first step not done: accent marker and tinted row.',
        },
        {
          name: 'Collapsed',
          attribute: 'data-collapsed, aria-expanded="false"',
          description: 'Only the header and progress show.',
        },
        {
          name: 'Complete',
          attribute: 'data-complete',
          description:
            'Every step is done: success progress and the completion message (`completeContent`).',
        },
      ],
    },
    interaction: {
      items: [
        'The toggle shows and hides the steps; the chevron turns to show the state.',
        'Dismiss calls `onDismiss`; nothing is hidden until the product stops rendering the checklist.',
        'Step actions are the product’s own buttons or links and behave as they normally do.',
        'When the product marks a step done, its row updates and the next step becomes current.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the toggle, the dismiss button, then each step’s action.',
        },
        { keys: 'Enter / Space', action: 'Activates the toggle, dismiss or a step action.' },
      ],
    },
    accessibility: {
      role: 'region (section) labelled by the title; an ordered list of steps',
      name: 'The `title`.',
      items: [
        'The toggle is a button named by the title with `aria-expanded` and `aria-controls`.',
        'The progress bar is named "Setup progress" and reads "2 of 4 done".',
        'Each step title is followed by hidden ", Done" or ", Not done" text; the current step has `aria-current="step"`.',
        'The dismiss button is named "Dismiss checklist".',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/onboarding-checklist.',
      ],
    },
    responsive: {
      items: [
        'Step actions sit at the end of the row on wider screens and wrap under the text on narrow ones, aligned with the title.',
        'The header wraps; the progress bar always takes the full width. Works at 320px.',
      ],
    },
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
        'Done markers use `--mn-color-success-solid` and `--mn-color-success-on-solid`; the current step uses `--mn-color-accent-strong`.',
        'Reduced motion removes the toggle transition. Forced colours outline done markers with `Highlight`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Get started',
          stack: true,
          render: () => <ChecklistDemo />,
        },
        {
          title: 'Complete',
          stack: true,
          render: () => (
            <OnboardingChecklist
              style={{ width: '100%', maxWidth: 560 }}
              title="Set up billing"
              headingLevel={3}
              collapsible={false}
              steps={[
                { id: 'plan', title: 'Choose a plan', done: true },
                { id: 'payment', title: 'Add a payment method', done: true },
                { id: 'invoice', title: 'Set invoice details', done: true },
              ]}
              completeContent="Billing is ready. You can change it any time in Settings."
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep it short: three to six steps that matter for getting value.',
        'Tick steps from real data (the thing was done), not from clicking the action.',
        'Persist collapse and dismiss per person, and offer a way back (for example in Help).',
      ],
      dont: [
        'Do not use it for a strict sequence or a form split into pages: use Stepper.',
        'Do not show it to people who have finished setup; hide it once dismissed or complete.',
        'Do not put several actions in one step.',
      ],
    },
    related: {
      items: [
        { id: 'stepper', reason: 'Steps in a fixed order inside one flow.' },
        { id: 'progress-bar', reason: 'The progress summary, on its own.' },
        { id: 'empty-state', reason: 'A single first action on an empty screen.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'progress-bar'],
    },
    testing: {
      unit: [
        'Steps, done state, progress value text and the current step render; actions are the product’s nodes; axe passes.',
        'Done steps hide their description and action.',
        'Collapse uncontrolled (click and keyboard) and controlled; `collapsible={false}` removes the toggle; progress stays visible.',
        'Dismiss shows only with `onDismiss` and calls it.',
        'Complete state with success progress and custom content; empty list; heading level; translated messages; server rendering.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery/onboarding-checklist',
        'Cross-browser axe: /components/onboarding-checklist',
      ],
    },
  },
} satisfies ComponentDocs;
