import { useState } from 'react';
import { AiMessageBranch } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const VERSIONS = [
  'The quarterly plan has three goals: hire two engineers, ship the reporting module and cut support response time in half.',
  'In short: grow the team by two, launch reporting, and halve support response time.',
  'Three goals this quarter. 1) Two engineering hires. 2) Reporting module launch. 3) Support response time down 50%.',
];

function Demo() {
  const [index, setIndex] = useState(0);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', maxWidth: '32rem' }}>
      <p style={{ margin: 0 }}>{VERSIONS[index]}</p>
      <AiMessageBranch index={index} count={VERSIONS.length} onChange={setIndex} />
    </div>
  );
}

export default {
  id: 'ai-message-branch',
  title: 'AiMessageBranch',
  summary: 'The "‹ 2 / 3 ›" switcher between regenerated or edited versions of a message.',
  sections: {
    purpose: {
      summary:
        'Lets people move between the alternative versions of a message that regenerating a reply or editing a prompt creates, without losing any of them.',
      useWhen: [
        'A message has more than one version (`message.branch` in the conversation model).',
        'Building a custom message layout that needs the same switcher AiMessageView shows.',
      ],
      avoidWhen: [
        'Moving through pages of a list: use Pagination.',
        'Comparing two versions side by side: use AiArtifactVersions or AiInlineDiff.',
      ],
    },
    designPrinciples: {
      items: [
        'Small and quiet: muted text sized to sit in a message footer next to the actions.',
        'Every version is kept; switching never deletes anything.',
        'The position is visible text and is announced after each switch, never on first render.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`index` is 0-based; the visible position and the announcement are 1-based.',
        'Renders nothing when `count` is below 2, unless `showSingle` is set.',
        'Native `<div>` attributes pass through and the ref is forwarded to the root.',
        'With `useAiConversation`, call `branchTo(messageId, index)` from `onChange`.',
        '`AI_MESSAGE_BRANCH_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: { items: [], notes: ['There are no visual variants.'] },
    states: {
      items: [
        { name: 'First version', description: 'Previous is disabled.' },
        { name: 'Last version', description: 'Next is disabled.' },
        {
          name: 'Disabled',
          description: 'Both buttons are disabled, for example while a reply streams.',
        },
        { name: 'Single version', description: 'Nothing is rendered unless `showSingle` is set.' },
      ],
    },
    interaction: {
      items: [
        'Previous and Next call `onChange` with the neighbouring 0-based index.',
        'The parent updates `index`; the new position is then announced politely.',
        'When a button becomes disabled at either end, focus moves to the other button so it is never lost.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between Previous and Next.' },
        { keys: 'Enter / Space', action: 'Switches to the previous or next version.' },
      ],
    },
    accessibility: {
      role: 'group',
      name: '"Versions" (aria-label, from `messages.label`)',
      items: [
        'Icon buttons are named "Previous version" and "Next version".',
        'A polite status region announces "Version 2 of 3" after a switch.',
        'The chevrons are decorative and mirror in right-to-left layouts.',
      ],
    },
    responsive: {
      items: [
        'Compact and inline; it never needs more than about 8rem.',
        'On coarse pointers both buttons are at least 44px.',
      ],
    },
    theme: {
      items: [
        'Text uses `--mn-color-text-muted`; buttons are ghost IconButtons.',
        'In forced colours the text uses CanvasText and disabled buttons GrayText.',
      ],
    },
    examples: {
      items: [
        { title: 'Switch between versions', stack: true, render: () => <Demo /> },
        {
          title: 'Disabled while streaming',
          render: () => <AiMessageBranch index={1} count={3} onChange={() => {}} disabled />,
        },
      ],
    },
    doDont: {
      do: [
        'Place it in the message footer, next to the message actions.',
        'Disable it while the message streams.',
      ],
      dont: [
        'Do not show it for a message with one version.',
        'Do not delete versions when the person switches away.',
      ],
    },
    related: {
      items: [
        { id: 'ai-message', reason: 'Shows a version switcher in its footer.' },
        { id: 'ai-user-message-edit', reason: 'Editing a prompt creates a new version.' },
        { id: 'ai-message-actions', reason: 'Regenerate creates a new version.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['button'] },
    testing: {
      unit: [
        'Named group, labelled buttons, visible position; props and ref forwarded.',
        'Nothing rendered for one version; `showSingle`.',
        '0-based `onChange`; ends disabled.',
        'Polite announcement after a switch only; focus moves when a button becomes disabled.',
        'Disabled state and messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
