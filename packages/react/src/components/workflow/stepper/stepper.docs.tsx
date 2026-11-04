import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { Stepper, type StepperStep } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SETUP: StepperStep[] = [
  { id: 'basics', label: 'Basics' },
  { id: 'access', label: 'Access' },
  { id: 'branding', label: 'Branding', optional: true },
  { id: 'review', label: 'Review' },
];

function WizardDemo() {
  const [current, setCurrent] = useState(1);
  const [furthest, setFurthest] = useState(1);
  const go = (i: number) => {
    setCurrent(i);
    setFurthest((f) => Math.max(f, i));
  };
  return (
    <div style={{ display: 'grid', gap: 16, width: '100%' }}>
      <Stepper
        aria-label="Setup progress"
        steps={SETUP}
        current={current}
        furthest={furthest}
        onStepSelect={(_, i) => go(i)}
      />
      <div style={{ display: 'flex', gap: 8 }}>
        <Button disabled={current === 0} onClick={() => go(current - 1)}>
          Back
        </Button>
        <Button
          variant="primary"
          disabled={current === SETUP.length - 1}
          onClick={() => go(current + 1)}
        >
          Continue
        </Button>
      </div>
    </div>
  );
}

export default {
  id: 'stepper',
  title: 'Stepper',
  summary:
    'Multi-step progress for wizards and onboarding: horizontal or vertical, current, complete, error and optional steps.',
  sections: {
    purpose: {
      summary:
        'Shows where the user is in a fixed sequence of steps, what is done and what is left, and optionally lets them jump back to a finished step. Import from `@rookcom/monarch-react/patterns`.',
      useWhen: [
        'A wizard or setup flow with three to seven named steps (create a community, import a file, connect a domain).',
        'A record that moves through ordered stages, shown read-only (a payroll run, a filing journey).',
      ],
      avoidWhen: [
        'Sections the user can visit in any order: use Tabs.',
        'Progress of one task with no named steps: use Progress.',
        'A to-do list of independent tasks: use a checklist.',
      ],
    },
    designPrinciples: {
      items: [
        'Status is never colour alone: a check for complete, “!” for error, the number otherwise, plus hidden text (“completed”, “needs attention”).',
        'The current step is the strongest element: accent ring and semibold label.',
        'Connectors fill with the accent as steps complete, so progress reads at a glance.',
        'Navigation is opt-in: only with `onStepSelect` do reachable steps become buttons; the current and future steps never are.',
        'When there is no room it says less, not smaller: “Step 2 of 5 · Access” with a bar.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `<nav aria-label="Progress">` (ref) with an `<ol>`; each step is an `<li>` with `data-status` and, for the current step, `aria-current="step"`.',
        '`steps` are `{ id, label, description?, optional?, status?, disabled? }`; `status` overrides the derived state with `error` or `complete`.',
        '`current` and `furthest` take a step id or a zero-based index. `stepStatus(index, currentIndex, step)` is exported to derive the same status elsewhere.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'orientation',
          values: ['horizontal', 'vertical'],
          description:
            '`horizontal` (default) spreads steps across with connectors; `vertical` stacks them for sidebars and checklists, and never collapses.',
        },
        {
          prop: 'compact',
          values: ['auto', 'always', 'never'],
          description:
            '`auto` (default) collapses a horizontal stepper to the “Step n of m” summary when its own width is under 36rem (a container query), so it adapts to 320px screens and narrow panels.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Current',
          attribute: 'data-status="current", aria-current="step"',
          description: 'Accent ring and number, semibold label.',
        },
        {
          name: 'Complete',
          attribute: 'data-status="complete"',
          description: 'Filled accent circle with a check; the following connector is accent.',
        },
        {
          name: 'Upcoming',
          attribute: 'data-status="upcoming"',
          description: 'Outlined circle with the number, muted label.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description: 'Filled danger circle with “!”, danger label.',
        },
        { name: 'Optional', description: '“Optional” under the label (`messages.optional`).' },
        {
          name: 'Clickable',
          attribute: 'button[data-clickable]',
          description: 'Hover fill and underline; focus ring.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'A step that would be clickable but is disabled: disabled label colour.',
        },
      ],
    },
    interaction: {
      items: [
        'Steps before `current` are complete. With `onStepSelect`, complete steps and steps up to `furthest` (after going back) are buttons that call it with the step and its index.',
        'The current step, future steps beyond `furthest` and `disabled` steps are plain text.',
        'The stepper does not move focus; on a step change, move focus to the new step’s heading in your page so screen-reader users hear where they are.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves between clickable steps (only reachable steps are buttons).',
        },
        { keys: 'Enter / Space', action: 'Goes to the focused step.' },
      ],
      notes: ['A read-only stepper has no tab stops.'],
    },
    accessibility: {
      role: 'navigation containing an ordered list',
      name: '`aria-label` on the nav, default “Progress”; name it after the flow (“Setup progress”).',
      items: [
        '`aria-current="step"` marks the current step.',
        'Each step includes hidden text “Step n of m:” and, when relevant, “(completed)” or “(needs attention)”, so buttons are fully named.',
        'Indicator numbers, checks and the compact bar are `aria-hidden`; the text carries the information.',
        'In compact mode the list is not displayed and the summary text (“Step 2 of 5 Access”) is read instead.',
        'Verified with axe in jsdom and in Chromium, Firefox and WebKit on /components/stepper.',
      ],
    },
    responsive: {
      items: [
        'Horizontal steps share the width; labels wrap rather than overflow.',
        'Under 36rem of its own width (`compact="auto"`) it shows “Step 2 of 5 · label” and a progress bar, so at 320px nothing is cramped.',
        'Vertical steppers keep the full list at every width.',
      ],
    },
    theme: {
      items: [
        'Monarch tokens only: `--mn-color-accent-solid` for complete and current, `--mn-color-danger-solid` for errors, `--mn-color-border-strong` for upcoming outlines.',
        'Follows the product accent; forced-colours mode draws indicators in Highlight.',
        'The hover transition is removed under `prefers-reduced-motion: reduce`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Wizard with clickable completed steps',
          stack: true,
          render: () => <WizardDemo />,
          code: `<Stepper
  aria-label="Setup progress"
  steps={[
    { id: 'basics', label: 'Basics' },
    { id: 'access', label: 'Access' },
    { id: 'branding', label: 'Branding', optional: true },
    { id: 'review', label: 'Review' },
  ]}
  current={step}
  furthest={furthest}
  onStepSelect={(_, i) => setStep(i)}
/>`,
        },
        {
          title: 'Error and descriptions',
          stack: true,
          render: () => (
            <Stepper
              aria-label="Import progress"
              current="map"
              steps={[
                { id: 'upload', label: 'Upload', description: 'contacts.csv' },
                {
                  id: 'map',
                  label: 'Map columns',
                  description: '2 columns need a field',
                  status: 'error',
                },
                { id: 'preview', label: 'Preview' },
                { id: 'import', label: 'Import' },
              ]}
            />
          ),
          code: `<Stepper current="map" steps={[…, { id: 'map', label: 'Map columns', status: 'error' }, …]} />`,
        },
        {
          title: 'Vertical',
          stack: true,
          render: () => (
            <Stepper
              orientation="vertical"
              aria-label="Domain setup"
              current={2}
              steps={[
                { id: 'add', label: 'Add domain', description: 'help.example.com' },
                { id: 'dns', label: 'Add DNS records' },
                { id: 'verify', label: 'Verify', description: 'Checking every few minutes' },
                { id: 'live', label: 'Live' },
              ]}
            />
          ),
          code: `<Stepper orientation="vertical" current={2} steps={steps} aria-label="Domain setup" />`,
        },
        {
          title: 'Compact',
          stack: true,
          render: () => (
            <Stepper aria-label="Checkout progress" compact="always" current={1} steps={SETUP} />
          ),
          code: `<Stepper compact="always" current={1} steps={steps} /> // what 320px shows with compact="auto"`,
        },
      ],
    },
    doDont: {
      do: [
        'Keep labels to one or two words, and keep the step count small (three to seven).',
        'Mark steps with problems `status: "error"` and jump the user to the first one on submit.',
        'Pair the stepper with Back / Continue buttons; the stepper is not the only way forward.',
      ],
      dont: [
        'Do not make future steps clickable when earlier steps validate what they need.',
        'Do not use a stepper for tabs or for free-order sections.',
        'Do not rely on the step colour to explain an error; say what is wrong in the step itself.',
      ],
    },
    related: {
      items: [
        {
          id: 'progress-bar',
          reason: 'Progress of one task without named steps; also the bar in compact mode.',
        },
        { id: 'tabs', reason: 'Sections in free order.' },
        {
          id: 'onboarding-checklist',
          reason: 'Independent tasks the user ticks off in any order.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['progress-bar'],
    },
    testing: {
      unit: [
        'Named nav and ordered list; `aria-current="step"`; complete/current/upcoming statuses; hidden “Step n of m” and status text; description and optional note; axe.',
        'Current by index (clamped) and by id.',
        'Error and out-of-order complete overrides; axe.',
        'No buttons without `onStepSelect`; with it only completed steps are buttons, clicked or activated with Enter; axe.',
        '`furthest` keeps later reached steps clickable after going back; disabled steps are not.',
        'Compact summary with an aria-hidden bar; vertical orientation.',
        'Translated messages; `stepStatus`; renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery/stepper', 'Cross-browser axe: /components/stepper'],
    },
  },
} satisfies ComponentDocs;
