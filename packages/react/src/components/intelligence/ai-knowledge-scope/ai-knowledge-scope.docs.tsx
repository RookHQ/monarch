import { useState } from 'react';
import { AiKnowledgeScope, type AiKnowledgeSource } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const SOURCES: AiKnowledgeSource[] = [
  {
    id: 'handbook',
    label: 'Team handbook',
    kind: 'space',
    description: '3 documents',
    children: [
      { id: 'policies', label: 'Policies', kind: 'doc' },
      { id: 'onboarding', label: 'Onboarding guide', kind: 'doc' },
      { id: 'travel', label: 'Travel guidelines', kind: 'doc' },
    ],
  },
  {
    id: 'projects',
    label: 'Projects',
    kind: 'space',
    children: [
      { id: 'roadmap', label: 'Roadmap', kind: 'doc' },
      { id: 'launch-notes', label: 'Launch notes', kind: 'doc' },
    ],
  },
  { id: 'calendar', label: 'Calendar', kind: 'app', description: 'Events you can see' },
];

function ScopeDemo() {
  const [value, setValue] = useState<string[]>(['policies', 'calendar']);
  return (
    <div style={{ maxWidth: '24rem', width: '100%' }}>
      <AiKnowledgeScope
        sources={SOURCES}
        value={value}
        onChange={setValue}
        defaultExpanded={['handbook']}
      />
    </div>
  );
}

function DisabledDemo() {
  return (
    <div style={{ maxWidth: '24rem', width: '100%' }}>
      <AiKnowledgeScope sources={SOURCES} value={['projects']} onChange={() => {}} disabled />
    </div>
  );
}

export default {
  id: 'ai-knowledge-scope',
  title: 'AiKnowledgeScope',
  summary: 'Pick which spaces, documents and apps ground the AI’s answers, as a checkbox tree.',
  sections: {
    purpose: {
      summary:
        'Lets people decide what the AI may read before it answers: a tree of spaces, documents and apps with tri-state checkboxes. Checking a space selects everything in it; a partly selected space shows a dash.',
      useWhen: [
        'An assistant or agent can be grounded in a chosen subset of workspace content.',
        'Setting up an agent’s knowledge in an agent builder.',
        'People need to see, at a glance, which sources are in and out of scope.',
      ],
      avoidWhen: [
        'Deciding what the AI may change, not read: use AiPermissionScope.',
        'Attaching one-off context to a single prompt: use AiContextChips or AiMentionContext.',
        'A flat list of a handful of options: use a group of Checkbox.',
      ],
    },
    designPrinciples: {
      items: [
        'Groups stand for everything inside them: selecting a space keeps new documents in scope.',
        'Partial selection is visible (a dash) and announced ("mixed"), so nothing is selected by surprise.',
        'A live summary ("3 sources selected") confirms the result of every change.',
        'Pure helpers (`aiKnowledgeScopeState`, `toggleAiKnowledgeSource`) keep the selection logic testable outside the UI.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: pass `value` and update it from `onChange`. The new value lists every fully selected source, groups included, in tree order; ids not found in the tree are kept.',
        'An id in `value` selects that source and all its descendants. A group whose children are all selected counts as selected.',
        '`aiKnowledgeScopeState(sources, value)` returns a `Map` of id to `checked`, `unchecked` or `indeterminate`; `toggleAiKnowledgeSource(sources, value, id)` returns the value after toggling one source.',
        'Expansion is uncontrolled with `defaultExpanded`, or controlled with `expanded` and `onExpandedChange`.',
        'Native `<div>` attributes pass through to the root and the ref is forwarded to it.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['There are no visual variants. `showSummary={false}` hides the summary line.'],
    },
    states: {
      items: [
        {
          name: 'Checked',
          attribute: 'data-state="checked"',
          description: 'The source, or every source in the group, is selected.',
        },
        {
          name: 'Indeterminate',
          attribute: 'data-state="indeterminate"',
          description: 'Some but not all sources in the group are selected; shows a dash.',
        },
        {
          name: 'Unchecked',
          attribute: 'data-state="unchecked"',
          description: 'Nothing in the source is selected.',
        },
        {
          name: 'Expanded',
          attribute: 'aria-expanded="true"',
          description: 'A group shows its children; the chevron points down.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled',
          description: 'The tree can be browsed but nothing can be toggled.',
        },
        {
          name: 'Empty',
          description: 'With no sources, shows "No sources available" instead of the tree.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a row toggles its checkbox. Toggling a checked or partly checked group clears it and everything in it; toggling an unchecked one selects everything in it.',
        'Clicking the chevron expands or collapses a group without changing the selection.',
        'The summary under the tree updates politely after each change.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus into the tree (one tab stop) and out of it.' },
        { keys: 'ArrowDown / ArrowUp', action: 'Moves to the next or previous visible source.' },
        {
          keys: 'ArrowRight',
          action: 'Expands a collapsed group; on an expanded group, moves to its first child.',
        },
        {
          keys: 'ArrowLeft',
          action: 'Collapses an expanded group; otherwise moves to the parent group.',
        },
        { keys: 'Home / End', action: 'Moves to the first or last visible source.' },
        { keys: 'Space / Enter', action: 'Toggles the focused source.' },
      ],
    },
    accessibility: {
      role: 'tree with treeitem children in nested groups',
      name: '`messages.label` ("Knowledge sources") for the tree; each item reads "Label, Kind"',
      items: [
        'Each item has `aria-checked` (`true`, `false` or `mixed`), `aria-level`, `aria-setsize`, `aria-posinset` and, for groups, `aria-expanded`.',
        'Roving tabindex: only the active item is in the tab order.',
        'A source’s description is its accessible description.',
        'The check boxes and chevrons are drawn, not native controls, and are hidden from assistive technology; the state is on the tree item.',
        'The selection summary is a polite live region.',
      ],
    },
    responsive: {
      items: [
        'Fills its container; long labels truncate and descriptions wrap, so there is no horizontal scroll at 390px.',
        'Below 480px the kind label on the right is hidden (it stays in the accessible name).',
        'On coarse pointers rows are at least 44px tall and the chevron target grows.',
      ],
    },
    theme: {
      items: [
        'Checked and partly checked boxes use `--mn-color-ai-solid` with `--mn-color-ai-on-solid` marks.',
        'The tree sits on `--mn-color-bg-surface` with a `--mn-color-border-subtle` border; rows hover with `--mn-color-bg-muted`.',
        'In forced colours checked boxes use Highlight and the focus ring is Highlight.',
        'Logical properties throughout, and the chevron mirrors in right-to-left layouts.',
      ],
    },
    examples: {
      items: [
        { title: 'Choose sources', stack: true, render: () => <ScopeDemo /> },
        { title: 'Disabled', stack: true, render: () => <DisabledDemo /> },
      ],
    },
    doDont: {
      do: [
        'Store group ids as returned, so new content in a selected space stays in scope.',
        'Describe each space ("24 documents") so people know what they are including.',
        'Pair it with AiPermissionScope when the AI can also act on data.',
      ],
      dont: [
        'Do not list sources the person cannot open themselves.',
        'Do not save the selection without the person’s action in settings flows.',
        'Do not use it to grant write access.',
      ],
    },
    related: {
      items: [
        { id: 'ai-permission-scope', reason: 'What the AI may read and write in each app.' },
        { id: 'ai-context-chips', reason: 'Context attached to a single prompt.' },
        { id: 'ai-agent-builder', reason: 'Uses this tree for an agent’s knowledge.' },
        { id: 'checkbox', reason: 'A flat list of options.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        '`aiKnowledgeScopeState` derives checked, indeterminate and inherited states.',
        '`toggleAiKnowledgeSource` selects and clears groups, splits a group when a child is cleared, promotes a full group and keeps unknown ids.',
        'Named tree, item names, `aria-checked`, `aria-expanded` and descriptions; ref and prop forwarding.',
        'Arrow keys, Home and End move focus; ArrowRight and ArrowLeft expand, collapse and move; one tab stop.',
        'Space, Enter and click toggle and report the tri-state value; the chevron only expands.',
        'Disabled, controlled expansion, localised strings and the empty state.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
