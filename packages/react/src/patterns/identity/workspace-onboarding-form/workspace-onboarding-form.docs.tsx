import { WorkspaceOnboardingForm } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));
const TAKEN = new Set(['acme', 'design', 'studio']);

async function checkUsername(username: string) {
  await wait(500);
  return TAKEN.has(username.toLowerCase())
    ? { valid: false, message: 'That username is taken' }
    : { valid: true };
}

function Onboarding() {
  return (
    <div style={{ maxWidth: 420 }}>
      <WorkspaceOnboardingForm
        checkUsername={checkUsername}
        onSubmit={async () => {
          await wait(800);
          return { id: 'w-1' };
        }}
      />
    </div>
  );
}

function Failing() {
  return (
    <div style={{ maxWidth: 420 }}>
      <WorkspaceOnboardingForm
        types={[]}
        defaultValues={{ name: 'Northwind', username: 'northwind' }}
        onSubmit={async () => {
          await wait(600);
          throw new Error('You have reached the workspace limit for this account.');
        }}
      />
    </div>
  );
}

export default {
  id: 'workspace-onboarding-form',
  title: 'WorkspaceOnboardingForm',
  summary: 'Creates a workspace: name, a unique username with a live check, and a type.',
  sections: {
    purpose: {
      summary:
        'Every Rook app asks a new person to create a workspace before they start, and offers the same form again from the workspace switcher. WorkspaceOnboardingForm collects a name, a unique username with a debounced availability check, and a workspace type, validates them inline and shows the server’s error if creation fails. Needed by: App, Atlas, Base, Calendar, Chat, Developers, Dossier, Dots, Friends, GTM, Intellect, People, Pulse, Ranker, Recruit, Signal, Stride and Tickets (onboarding page and workspace switcher).',
      useWhen: [
        'A first-run onboarding page that creates the person’s first workspace.',
        'Inside CreateWorkspaceDialog, for creating another workspace.',
      ],
      avoidWhen: [
        'Editing an existing workspace’s settings: use a settings form with SettingsSection.',
        'A multi-step setup with more than these fields: use WizardDialog or Stepper.',
      ],
    },
    designPrinciples: {
      items: [
        'Errors appear after a field is left or the form is submitted, never while someone is still typing their first characters.',
        'The availability check runs after the local rules pass, so the server is not asked about names that cannot be valid.',
        'If creation fails, the form keeps every value and says why, in the server’s words when it gives them.',
        'Product logic stays in the product: it passes `checkUsername` and `onSubmit`.',
      ],
    },
    apiReference: { source: 'typescript' },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onSubmit(values)` gets `{ name, username, type? }` with the name trimmed; `type` is left out when `types` is empty.',
        '`checkUsername(username, signal)` returns `{ valid, message? }`, or `null` to skip.',
        'Defaults follow the apps’ shared rules: name 4–25 characters, username 5–30 letters and digits.',
        'Migrating from the app copy (`components/workspace/WorkspaceOnboardingForm.tsx` in each app): replace `useWorkspaces().createWorkspace.mutateAsync` with `onSubmit`, and `setActiveWorkspace` plus `onSuccess()` with `onSuccess(result)`. `checkUsernameAvailability` becomes `checkUsername` returning `{ valid: data.available, message: data.error }`. `isModal` becomes `submitLabel="Create workspace"` (or use CreateWorkspaceDialog). The backend error parsing (Recruit’s `details.fields`) moves into `errorMessage`. Translations from `useTranslations("workspace")` go into `messages`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'types',
          values: ['defaultWorkspaceTypes (Creator, Business)', 'custom list', '[]'],
          description: 'The type choice; an empty list hides it.',
        },
        {
          prop: 'checkUsername',
          values: ['omitted', 'function'],
          description: 'Local rules only, or with a live availability check.',
        },
        {
          prop: 'secondaryAction',
          values: ['omitted', 'element'],
          description:
            'Without it the submit button fills the width; with it they sit side by side.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Invalid field',
          attribute: 'aria-invalid (inputs), data-invalid (type group)',
          description: 'With the error text below the field.',
        },
        {
          name: 'Checking',
          attribute: 'data-status (username)',
          description: '`checking`, `valid`, `invalid` or `error` from the availability check.',
        },
        {
          name: 'Selected type',
          attribute: 'data-selected',
          description: 'Accent border and background on the card.',
        },
        {
          name: 'Pending',
          attribute: 'data-pending, aria-busy',
          description: 'The submit button shows a spinner and fields are read-only.',
        },
        {
          name: 'Failed',
          attribute: 'role="alert"',
          description: 'The rejection message above the button.',
        },
      ],
    },
    interaction: {
      items: [
        'Submitting with errors shows every error and focuses the first invalid field.',
        'A taken username, or one still being checked, blocks submit and focuses the username field.',
        'A failed availability lookup does not block submit; it offers "Try again".',
        'After `onSubmit` resolves, `onSuccess(result, values)` runs.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves through the fields, the type choice and the buttons.',
        },
        { keys: 'Arrow keys', action: 'Move between workspace types (native radios).' },
        { keys: 'Enter (in a text field)', action: 'Submits the form.' },
      ],
    },
    accessibility: {
      role: 'form',
      name: 'Each field has a visible label; the type choice is a group named "Type".',
      items: [
        'Errors are linked with `aria-describedby` and announced politely.',
        'Availability results are announced once they settle.',
        'Each type is a native radio named by its label and described by its description.',
        'A failed submit is announced with `role="alert"`.',
      ],
    },
    responsive: {
      items: [
        'The type cards sit side by side and stack when there is no room for two 9rem cards.',
        'Buttons share the row on narrow screens and are at least 44px tall on touch screens.',
      ],
    },
    theme: {
      items: [
        'Type cards use the surface and default border tokens; selected cards use the accent tokens.',
        'Errors use the danger tokens.',
        'In forced-colors mode the selected card has a thick Highlight border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Onboarding with availability check (try "acme")',
          stack: true,
          render: () => <Onboarding />,
        },
        { title: 'Server error, no type choice', stack: true, render: () => <Failing /> },
      ],
    },
    doDont: {
      do: [
        'Pass the signal from `checkUsername` to `fetch` so stale checks are cancelled.',
        'Return a useful message from `checkUsername` when the server gives one.',
        'Keep the rules in `rules` in step with the server’s validation.',
      ],
      dont: [
        'Do not create the workspace in `onSuccess`; do it in `onSubmit` so failures show in the form.',
        'Do not add product-specific fields here; wrap the form or use a WizardDialog.',
      ],
    },
    related: {
      items: [
        { id: 'create-workspace-dialog', reason: 'This form in a dialog.' },
        { id: 'username-availability-input', reason: 'The username field it uses.' },
        { id: 'workspace-switcher', reason: 'Where "Create workspace" usually starts.' },
        { id: 'form-field', reason: 'Labels, help and errors.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'alert', 'form-field', 'text-field', 'username-availability-input'],
    },
    testing: {
      unit: [
        'Labelled fields, described username and a named type group; axe passes.',
        'Rule errors on submit focus the first invalid field; invalid characters are reported.',
        'A taken username blocks submit and focuses the field.',
        'Submits trimmed values with a pending button, then calls onSuccess with the result.',
        'A rejection shows its message and keeps the values; types={[]} hides the group.',
        'Custom types, rules, labels and secondary action; arrow keys move between types.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
