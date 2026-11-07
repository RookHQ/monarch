import { useState } from 'react';
import { Button, Checkbox, WizardDialog } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function CloseProject() {
  const [exported, setExported] = useState(false);
  return (
    <WizardDialog
      trigger={<Button variant="danger">Close project</Button>}
      title="Close project"
      description="Closing removes the project for everyone."
      steps={[
        {
          id: 'review',
          title: 'What happens',
          content: (
            <ul>
              <li>48 tasks are archived</li>
              <li>6 people lose access</li>
            </ul>
          ),
        },
        {
          id: 'export',
          title: 'Keep a copy',
          description: 'Download the tasks and comments before you continue.',
          content: (
            <Checkbox
              label="I have downloaded a copy, or I do not need one"
              checked={exported}
              onCheckedChange={(next) => setExported(next === true)}
            />
          ),
          canContinue: exported,
          blockedReason: 'Tick the box to continue.',
        },
        {
          id: 'confirm',
          title: 'Close the project',
          content: <p>You can reopen it from Archive for 30 days.</p>,
          tone: 'danger',
          continueLabel: 'Close project',
          onContinue: () => wait(1200),
        },
        { id: 'done', kind: 'done', title: 'Project closed', content: <p>It is in Archive.</p> },
      ]}
    />
  );
}

function FailingStep() {
  return (
    <WizardDialog
      trigger={<Button>Import contacts</Button>}
      title="Import contacts"
      steps={[
        { id: 'check', title: 'Check the file', content: <p>contacts.csv · 120 rows</p> },
        {
          id: 'run',
          title: 'Import',
          continueLabel: 'Import 120 contacts',
          onContinue: async () => {
            await wait(800);
            throw new Error('The import service did not respond. Try again.');
          },
        },
      ]}
    />
  );
}

export default {
  id: 'wizard-dialog',
  title: 'WizardDialog',
  summary:
    'A dialog that walks through a short sequence of steps with Back, Continue and progress.',
  sections: {
    purpose: {
      summary:
        'Runs a short, ordered task inside a dialog: review, choose, confirm, done. It owns navigation, progress, async steps with a pending lock and inline errors, and focus. Account and destructive flows (delete workspace, transfer ownership, GDPR erase) are built on it.',
      useWhen: [
        'A task needs two to five steps that must happen in order, and the person should not leave the page.',
        'A destructive action needs a review or export step before the confirmation.',
      ],
      avoidWhen: [
        'One question is enough: use ConfirmDialog.',
        'The task is long or can be saved halfway: use a full page with a Stepper.',
      ],
    },
    designPrinciples: {
      items: [
        'One decision per step, with a heading that says what the step is for.',
        'The destructive step is last and names its action ("Delete workspace"), never "Continue".',
        'Nothing can dismiss the dialog while a step is running.',
        'Progress counts only real steps; the closing "done" screen is not one.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`steps` is an array of `{ id, title, description?, content?, canContinue?, blockedReason?, continueLabel?, tone?, onContinue?, hideBack?, kind? }`.',
        '`onContinue` may return a promise (pending until it settles; a rejection shows its message) or `false` (stay on the step).',
        'Every visible string is in `messages` (`back`, `cancel`, `continue`, `done`, `stepOf`, `error`) for translation.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'steps[].tone',
          values: ['default', 'danger'],
          description: '`danger` gives the step’s continue button the danger style.',
        },
        {
          prop: 'steps[].kind',
          values: ['step', 'done'],
          description:
            '`done` is a closing screen: not counted in progress, no Back or Cancel, one Done button.',
        },
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl'],
          description: 'Dialog width; default `md`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Blocked',
          attribute: 'disabled (continue button)',
          description:
            '`canContinue` is false. `blockedReason` is shown in the footer and describes the button.',
        },
        {
          name: 'Pending',
          attribute: 'aria-busy, data-busy',
          description:
            'The continue button shows a spinner; Back and Cancel are disabled; Escape and outside clicks are ignored.',
        },
        {
          name: 'Error',
          attribute: 'role="alert"',
          description:
            'A danger Alert under the step, linked to the continue button. The person can retry, go back or cancel.',
        },
        {
          name: 'Current step',
          attribute: 'data-step, data-kind',
          description: 'The current step id and whether it is a `done` screen.',
        },
      ],
    },
    interaction: {
      items: [
        'Continue runs the step’s `onContinue`, then moves to the next step. After the last step it calls `onComplete` and closes.',
        'Back returns to the previous step and clears any error.',
        'Cancel, Escape and the close button call `onCancel` and close, except while a step is running and on a `done` step.',
        'Every opening starts at the first step (or `defaultStep`).',
        'Pressing Enter in a field submits the step, like a form.',
        'Outside clicks do not close it by default, so typed values are not lost.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the step’s content and the footer buttons; focus stays inside.',
        },
        { keys: 'Enter (in a field)', action: 'Continues, when the step allows it.' },
        { keys: 'Enter / Space (button)', action: 'Activates Back, Cancel or Continue.' },
        { keys: 'Escape', action: 'Cancels, except while a step is running.' },
      ],
    },
    accessibility: {
      role: 'dialog (aria-modal)',
      name: '`title`; `description` becomes the accessible description.',
      items: [
        'When the step changes, focus moves to the new step heading, so screen readers announce it and keyboard users start at the top.',
        'Progress is text ("Step 2 of 4"); the bar beside it is decorative.',
        'A disabled continue button is described by `blockedReason`, so the reason is heard, not only seen.',
        'Errors are announced assertively and linked to the continue button.',
      ],
    },
    responsive: {
      items: [
        'Default width 32rem (`md`), never wider than the screen minus 2rem.',
        'Below 640px the footer buttons fill the row and Back takes its own row.',
        'Long step content scrolls inside the body while the footer stays visible.',
      ],
    },
    theme: {
      items: [
        'Dialog surface tokens; the progress bar uses the accent for done and current steps and the muted surface for the rest.',
        'In forced-colors mode the progress segments are outlined and current steps use `Highlight`.',
        'Light, dark and both high-contrast themes apply automatically.',
      ],
    },
    examples: {
      items: [
        { title: 'Review, export, confirm, done', render: () => <CloseProject /> },
        { title: 'A failing step', render: () => <FailingStep /> },
      ],
    },
    doDont: {
      do: [
        'Keep it to five steps or fewer.',
        'Put the destructive action on the last step and name it.',
        'Return the request’s promise from `onContinue` so pending and failure show in the dialog.',
        'Explain a blocked step with `blockedReason`.',
      ],
      dont: [
        'Do not use it for a single question.',
        'Do not run the destructive request before the last step.',
        'Do not hide a required step behind a disabled button with no reason.',
      ],
    },
    related: {
      items: [
        { id: 'confirm-dialog', reason: 'One question, one action.' },
        { id: 'dialog', reason: 'The base it is built on, for free-form content.' },
        { id: 'stepper', reason: 'Long, page-level processes.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'dialog'],
    },
    testing: {
      unit: [
        'Walks forward and back, shows progress, focuses each step heading; axe passes.',
        'The last step calls onComplete and closes; its button uses the danger variant.',
        'A blocked step disables Continue and describes it with the reason.',
        'An async step locks the dialog (aria-busy, Cancel disabled, Escape ignored); a rejection shows the error and stays.',
        'Returning false stays without an error.',
        'A done step has one Done button, is not counted in progress, and does not call onCancel.',
        'Escape cancels; every opening starts at the first step.',
        'All strings come from messages.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
