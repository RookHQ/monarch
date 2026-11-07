import {
  Button,
  ConfirmDialog,
  DangerZone,
  DangerZoneItem,
  SettingsRow,
  SettingsSection,
  Input,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

function WorkspaceDangerZone() {
  return (
    <DangerZone description="These actions affect everyone in the workspace and cannot be undone.">
      <DangerZoneItem
        title="Transfer ownership"
        description="Another member becomes the owner. You stay on as an admin."
        action={
          <ConfirmDialog
            trigger={<Button>Transfer ownership</Button>}
            title="Transfer ownership to Alex Morgan?"
            description="Alex can then remove you or delete the workspace."
            confirmLabel="Transfer ownership"
            onConfirm={() => wait(800)}
          />
        }
      />
      <DangerZoneItem
        title="Delete this workspace"
        description="Projects, tasks and files are deleted for everyone."
        action={
          <ConfirmDialog
            trigger={<Button variant="danger">Delete workspace</Button>}
            tone="danger"
            title="Delete this workspace?"
            description="This cannot be undone."
            consequences={[
              '14 projects and their tasks are deleted',
              '9 members lose access',
              'Invoices are kept for 7 years',
            ]}
            confirmPhrase="northfield"
            confirmLabel="Delete workspace"
            onConfirm={() => wait(1200)}
          />
        }
      />
    </DangerZone>
  );
}

export default {
  id: 'danger-zone',
  title: 'DangerZone',
  summary:
    'Settings section for irreversible actions (delete, transfer, leave) with a danger edge and rows that explain each one.',
  sections: {
    purpose: {
      summary:
        'Collects the irreversible actions of a settings page (delete a workspace, transfer ownership, leave, delete an account) in one clearly marked place, each with a title, its consequence and the button that starts it. It keeps destructive actions apart from everyday settings so they are found on purpose, not by accident.',
      useWhen: [
        'The end of a workspace, project or account settings page.',
        'Any action that removes data or access for good and is started from settings.',
        'An action that is sometimes unavailable and needs to say why ("You are the only owner").',
      ],
      avoidWhen: [
        'Reversible actions such as archive with restore: put them in a normal SettingsSection.',
        'A single destructive action on a record page: use a danger Button with a ConfirmDialog in the page actions.',
      ],
    },
    designPrinciples: {
      items: [
        'Separate and last: one danger zone per page, at the bottom, so it is never next to the Save button.',
        'Each row says what happens before the button does it.',
        'The zone only marks the place; the confirmation (ConfirmDialog, type-to-confirm, flows) is the action’s own job.',
        'Unavailable actions explain themselves in text instead of silently disappearing.',
        'Same header and card layout as SettingsSection, so the page reads as one system.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`DangerZone` renders a `<section>` (ref forwarded) labelled by its heading, with the rows in a card. Native attributes pass through.',
        '`DangerZoneItem` renders a `role="group"` `<div>` (ref forwarded) named by its title and described by its description and `disabledReason`.',
        'Inside a DangerZone, item titles are headings one level below the zone heading; outside one they are paragraphs.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`heading` defaults to "Danger zone"; pass a translated string to localise it.',
        '`disabledReason` does not disable the action for you: pass the action with `disabled` too.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'headingLevel',
          values: ['h2', 'h3', 'h4'],
          description: 'Zone heading element; item titles use the next level. Default `h2`.',
        },
      ],
      notes: ['There is one tone: the danger edge is the point of the component.'],
    },
    states: {
      items: [
        {
          name: 'Available',
          description: 'Title, description and an enabled action.',
        },
        {
          name: 'Unavailable',
          attribute: 'data-disabled (item)',
          description:
            '`disabledReason` is shown under the description with a lock icon and added to the group description.',
        },
      ],
      notes: ['Pending and error states belong to the action’s ConfirmDialog or flow.'],
    },
    interaction: {
      items: [
        'The zone itself does nothing; each item’s action does what it was given (usually opening a ConfirmDialog).',
        'Focus returns to the action button when its ConfirmDialog closes.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between the item actions in reading order.' },
        { keys: 'Enter / Space', action: 'Activates the focused action.' },
      ],
    },
    accessibility: {
      role: 'region (section with a heading); each item is a group',
      name: 'The zone by `heading`; each item by `title`, described by `description` and `disabledReason`.',
      items: [
        'Heading structure is kept: the zone heading and item titles appear in the heading outline.',
        'The danger edge is not the only signal: the heading says "Danger zone" and each title says what happens.',
        'The lock icon beside a disabled reason is decorative; the reason is plain text.',
        'In more-contrast themes and forced colours the card has a solid 2px border.',
      ],
    },
    responsive: {
      items: [
        'Rows put the action beside the text when the card is at least 36rem wide (container query) and below it when narrower, so it works at 390px.',
        'At 1024px and wider the heading sits beside the card, as in SettingsSection.',
        'Long titles and descriptions wrap; actions wrap onto several lines if needed.',
      ],
    },
    theme: {
      items: [
        'Card border `--mn-color-danger-border` with a 3px inline-start edge in `--mn-color-danger-solid`; heading in `--mn-color-danger-strong`.',
        'Rows are separated by `--mn-color-border-subtle` on `--mn-color-bg-surface`.',
        'More-contrast (`data-mn-contrast="more"` or `prefers-contrast: more`): 2px `--mn-color-danger-strong` border. Forced colours: 2px CanvasText border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Workspace danger zone',
          stack: true,
          render: () => <WorkspaceDangerZone />,
        },
        {
          title: 'Unavailable action with a reason',
          stack: true,
          render: () => (
            <DangerZone heading="Leave or delete" headingLevel="h3">
              <DangerZoneItem
                title="Leave this workspace"
                description="You lose access to its projects and messages."
                disabledReason="You are the only owner. Transfer ownership before leaving."
                action={
                  <Button variant="danger" disabled>
                    Leave workspace
                  </Button>
                }
              />
            </DangerZone>
          ),
        },
        {
          title: 'At the end of a settings page',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 'var(--mn-space-8)' }}>
              <SettingsSection title="Workspace" description="Shown to everyone in the workspace.">
                <SettingsRow label="Workspace name">
                  <Input defaultValue="Northfield" />
                </SettingsRow>
              </SettingsSection>
              <DangerZone>
                <DangerZoneItem
                  title="Delete this workspace"
                  description="Projects, tasks and files are deleted for everyone."
                  action={
                    <ConfirmDialog
                      trigger={<Button variant="danger">Delete workspace</Button>}
                      tone="danger"
                      title="Delete this workspace?"
                      description="This cannot be undone."
                      confirmPhrase="northfield"
                      confirmLabel="Delete workspace"
                      onConfirm={() => wait(1200)}
                    />
                  }
                />
              </DangerZone>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Put it last on the page, once.',
        'Name each title and button after the action ("Delete workspace"), never "Proceed".',
        'Confirm every action with a ConfirmDialog; add `confirmPhrase` for the most destructive ones.',
        'Explain unavailable actions with `disabledReason` and disable the button.',
      ],
      dont: [
        'Do not put everyday settings or Save buttons in it.',
        'Do not hide unavailable actions without saying why.',
        'Do not run the action straight from the button without a confirmation.',
      ],
    },
    related: {
      items: [
        { id: 'settings-section', reason: 'The ordinary settings sections it sits under.' },
        { id: 'confirm-dialog', reason: 'What each action usually opens.' },
        { id: 'consequence-list', reason: 'Lists the effects inside the confirmation.' },
        { id: 'delete-workspace-flow', reason: 'A full deletion flow started from an item.' },
        { id: 'delete-account-flow', reason: 'A full account deletion flow started from an item.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: [],
      notes: ['Uses the internal lock glyph for disabled reasons.'],
    },
    testing: {
      unit: [
        'Is a region named by the default heading; forwards ref and props; axe passes.',
        'Custom heading and level; item titles are one level down.',
        'Each item is a group named by its title and described by its description; axe passes.',
        'Tab moves through the actions; a ConfirmDialog opens from an item and Escape returns focus to it.',
        'A disabled reason is shown, sets data-disabled and joins the group description; axe passes.',
        'Outside a DangerZone, item titles are paragraphs.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
