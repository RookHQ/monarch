import { useState } from 'react';
import { AiPermissionScope, type AiPermissionScopeItem } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SCOPES: AiPermissionScopeItem[] = [
  { app: 'Calendar', resources: 'Your events only', access: 'read' },
  { app: 'Documents', resources: 'Shared projects', access: 'write' },
  { app: 'Contacts', access: 'read' },
  { app: 'Billing', access: 'none' },
];

function EditableDemo() {
  const [scopes, setScopes] = useState(SCOPES);
  return (
    <AiPermissionScope editable scopes={scopes} onChange={setScopes} manageHref="#permissions" />
  );
}

export default {
  id: 'ai-permission-scope',
  title: 'AiPermissionScope',
  summary: 'What an AI agent may read or edit, app by app, read-only or editable.',
  sections: {
    purpose: {
      summary:
        'Tells people exactly what an AI agent can see and change before they rely on it, and lets an admin set that access per app (no access, read, or read and edit).',
      useWhen: [
        'An agent card, agent settings page or onboarding step needs to show which apps the AI can reach.',
        'An admin sets per-app access for an agent or for the workspace assistant.',
      ],
      avoidWhen: [
        'Granting a single tool call at run time: use AiToolApproval.',
        'Choosing which sources ground answers: use AiKnowledgeScope.',
        'Permissions for people or roles: use the product’s role and permission settings.',
      ],
    },
    designPrinciples: {
      items: [
        'Access is stated in words ("Can read and edit"), with an icon and tint as support, never colour alone.',
        'Edit access is tinted as a warning so it stands out from read access at a glance.',
        'Editing is opt-in (`editable`); the default is an honest read-only summary.',
        'Controlled: the component never changes access on its own; the app decides when to save.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<div>`; native `<div>` attributes pass through.',
        '`app` is the row key, so app names must be unique.',
        '`aiAccessAtLeast(access, needed)` compares two levels (`write` includes `read`).',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'editable',
          values: ['false', 'true'],
          description:
            'False (default) shows each level as text with an icon. True shows a None / Read / Write segmented control per app.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Access level',
          attribute: 'data-access (on each row)',
          description:
            '`none` "No access" (lock), `read` "Can read" (eye, AI tint), `write` "Can read and edit" (pencil, warning tint).',
        },
        {
          name: 'Editable',
          attribute: 'data-editable',
          description: 'Rows show a segmented control instead of read-only text.',
        },
        {
          name: 'Empty',
          description: 'No scopes: shows "The AI has no access to any app." instead of the list.',
        },
      ],
    },
    interaction: {
      items: [
        'Read-only by default; nothing to operate except the optional "Manage permissions" link.',
        'When editable, choosing a level calls `onChange(scopes, changed)` with the whole updated list; choosing the current level does nothing.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between each row’s control and the manage link.' },
        {
          keys: 'Arrow keys / Home / End',
          action: 'Choose a level inside a row’s control (SegmentedControl radio group).',
        },
      ],
      notes: ['In read-only mode only the manage link is focusable.'],
    },
    accessibility: {
      role: 'list (a `<ul>`); each editable row has a radiogroup',
      name: '`messages.label`, default "What the AI can access"; each control is named "Access to <app>"',
      items: [
        'Each read-only row reads as "Calendar, Your events only: Can read".',
        'Icons are decorative; the level is always in text.',
        '`manageHref` passes through `isSafeUrl`; `javascript:` and similar URLs are dropped.',
      ],
    },
    responsive: {
      items: [
        'Rows wrap: on a 390px screen the control or level drops under the app name, with no horizontal scroll.',
        'The segmented control and manage link grow to 44px touch targets on coarse pointers.',
        'RTL: logical properties throughout.',
      ],
    },
    theme: {
      items: [
        'Read access uses `--mn-color-ai-subtle` / `--mn-color-ai-strong`; edit access `--mn-color-warning-subtle` / `--mn-color-warning-strong`; no access is muted.',
        'Rows sit on `--mn-color-bg-surface` with `--mn-color-border-subtle` dividers.',
        'Forced colours: bordered CanvasText pills and a LinkText link.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Read-only',
          stack: true,
          render: () => <AiPermissionScope scopes={SCOPES} />,
        },
        { title: 'Editable with a manage link', stack: true, render: () => <EditableDemo /> },
        { title: 'No access', stack: true, render: () => <AiPermissionScope scopes={[]} /> },
      ],
    },
    doDont: {
      do: [
        'Say which part of an app the access covers in `resources` ("Shared projects").',
        'Save changes explicitly in the surrounding settings form.',
      ],
      dont: [
        'Do not grant write access by default; start at read or none.',
        'Do not hide apps with no access when people need to know the AI cannot reach them.',
      ],
    },
    related: {
      items: [
        { id: 'ai-agent-card', reason: 'Shows an agent’s tools and data access.' },
        { id: 'ai-tool-approval', reason: 'Approves one tool call at run time.' },
        { id: 'ai-knowledge-scope', reason: 'Picks the sources that ground answers.' },
        { id: 'segmented-control', reason: 'The control used for each editable row.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['segmented-control'] },
    testing: {
      unit: [
        'Lists each app with its access in text and `data-access`; ref and props forwarding.',
        'Editable rows are radio groups named "Access to <app>" and call `onChange` with the updated list, by click and by arrow keys.',
        'Shows a safe manage link and drops `javascript:` URLs.',
        'Empty state and translated strings; `aiAccessAtLeast`.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
