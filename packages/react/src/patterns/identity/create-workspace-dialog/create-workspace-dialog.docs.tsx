import { Button } from '@rookcom/monarch-react';
import { CreateWorkspaceDialog } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function FromSwitcher() {
  return (
    <CreateWorkspaceDialog
      trigger={<Button variant="primary">Create workspace</Button>}
      checkUsername={async (username) => {
        await wait(400);
        return username.toLowerCase() === 'acme'
          ? { valid: false, message: 'That username is taken' }
          : { valid: true };
      }}
      onSubmit={async () => {
        await wait(800);
        return { id: 'w-2' };
      }}
    />
  );
}

export default {
  id: 'create-workspace-dialog',
  title: 'CreateWorkspaceDialog',
  summary: 'A dialog that creates another workspace with WorkspaceOnboardingForm.',
  sections: {
    purpose: {
      summary:
        'People who already have a workspace create another from the workspace switcher without leaving the page. CreateWorkspaceDialog puts WorkspaceOnboardingForm in a dialog with Cancel, keeps it open while the workspace is created and closes it when that succeeds. Needed by: App, Atlas, Base, Calendar, Chat, Developers, Dossier, Dots, Friends, GTM, Intellect, People, Pulse, Ranker, Recruit, Signal, Stride and Tickets (workspace switcher).',
      useWhen: ['"Create workspace" from a workspace switcher or account menu.'],
      avoidWhen: [
        'The first workspace during onboarding: put WorkspaceOnboardingForm on the page.',
        'Setup with several steps: use WizardDialog.',
      ],
    },
    designPrinciples: {
      items: [
        'Typed values are not lost to a stray click: outside clicks do not close it.',
        'Nothing dismisses it while the workspace is being created.',
        'One form, two homes: the page and the dialog share WorkspaceOnboardingForm.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Takes the form’s `onSubmit`, `onSuccess`, `checkUsername`, `types`, `defaultValues`, `rules`, `usernamePrefix` and `errorMessage`; form strings go in `formMessages`, dialog strings in `messages`.',
        'Migrating from the app copy (`components/workspace/CreateWorkspaceModal.tsx` in each app): `open` and `onOpenChange` are unchanged. Pass `onSubmit` (the old form’s `createWorkspace.mutateAsync`) and set the active workspace in `onSuccess`. The title and close label from `useTranslations` go into `messages`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['sm', 'md', 'lg', 'xl'],
          description: 'Dialog width. Default `md`.',
        },
        {
          prop: 'closeOnSuccess',
          values: ['true', 'false'],
          description: 'Close after `onSubmit` resolves.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Open', attribute: 'data-state="open"', description: 'From Radix Dialog.' },
        {
          name: 'Busy',
          attribute: 'aria-busy, data-busy',
          description: 'Creating: Escape, the close button and Cancel do nothing.',
        },
        {
          name: 'Failed',
          attribute: 'role="alert"',
          description: 'The form shows the error and the dialog stays open.',
        },
      ],
    },
    interaction: {
      items: [
        'The trigger (or `open`) opens it with focus on the first field.',
        'Cancel, Escape and the close button close it, except while creating.',
        'On success `onSuccess` runs, the dialog closes and focus returns to the trigger.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves through the form; focus stays in the dialog.' },
        { keys: 'Enter (in a text field)', action: 'Submits.' },
        { keys: 'Escape', action: 'Closes, except while creating.' },
      ],
    },
    accessibility: {
      role: 'dialog',
      name: 'The title ("Create new workspace"); `description` is the accessible description.',
      items: [
        'Focus is trapped inside and returns to the trigger on close.',
        'The form’s labels, errors and announcements apply unchanged.',
      ],
    },
    responsive: {
      items: [
        'Follows Dialog: capped at the viewport width minus 2rem; the body scrolls.',
        'The type cards stack when the dialog is narrow.',
      ],
    },
    theme: {
      items: [
        'Dialog surface and overlay tokens; the form’s tokens inside.',
        'In forced-colors mode the dialog gets a system-colour border.',
      ],
    },
    examples: {
      items: [{ title: 'From a workspace switcher', render: () => <FromSwitcher /> }],
    },
    doDont: {
      do: [
        'Make the new workspace active in `onSuccess`.',
        'Return the create request’s promise from `onSubmit`.',
      ],
      dont: [
        'Do not close the dialog yourself in `onSubmit`; it closes on success.',
        'Do not use it for the very first workspace; put the form on the page.',
      ],
    },
    related: {
      items: [
        { id: 'workspace-onboarding-form', reason: 'The form inside, for pages.' },
        { id: 'workspace-switcher', reason: 'Where the dialog usually opens from.' },
        { id: 'dialog', reason: 'The dialog it is built on.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'dialog', 'workspace-onboarding-form'],
    },
    testing: {
      unit: [
        'Opens from its trigger as a named, described dialog; axe passes.',
        'Cannot be dismissed while creating; closes on success, calls onSuccess and returns focus.',
        'Stays open with the error when creation fails.',
        'Controlled; Cancel calls onOpenChange(false).',
        'Translated dialog and form messages.',
        'Renders its trigger on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
