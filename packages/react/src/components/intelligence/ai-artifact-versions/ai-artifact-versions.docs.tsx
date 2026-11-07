import { useState } from 'react';
import {
  AiArtifactDiff,
  AiArtifactVersions,
  type AiArtifactVersionInfo,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const VERSIONS: AiArtifactVersionInfo[] = [
  { version: 1, createdAt: '2026-03-12T09:00:00Z' },
  { version: 2, createdAt: '2026-03-12T09:20:00Z', label: 'Shorter intro' },
  { version: 3, createdAt: '2026-03-12T10:05:00Z', label: 'Added risks' },
];
const TEXT: Record<number, string> = {
  1: 'The team shipped the new onboarding flow in May. Sign-ups grew and support tickets went down.',
  2: 'The team shipped the onboarding flow in May. Sign-ups grew by a fifth.',
  3: 'The team shipped the onboarding flow in May. Sign-ups grew by a fifth. The main risk is the June migration.',
};

function VersionsDemo() {
  const [value, setValue] = useState(3);
  const [compare, setCompare] = useState<[number, number] | null>(null);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-3)', maxWidth: '36rem' }}>
      <AiArtifactVersions
        versions={VERSIONS}
        value={value}
        onChange={(v) => {
          setValue(v);
          setCompare(null);
        }}
        comparing={compare !== null}
        onCompare={(from, to) => setCompare(compare ? null : [from, to])}
      />
      {compare ? (
        <AiArtifactDiff
          before={TEXT[compare[0]] ?? ''}
          after={TEXT[compare[1]] ?? ''}
          from={compare[0]}
          to={compare[1]}
        />
      ) : (
        <p style={{ margin: 0 }}>{TEXT[value]}</p>
      )}
    </div>
  );
}

export default {
  id: 'ai-artifact-versions',
  title: 'AiArtifactVersions',
  summary: 'Steps through the versions of an AI artifact, with a diff between two of them.',
  sections: {
    purpose: {
      summary:
        'Every revision the AI makes to an artifact is a new version. This stepper lets people go back to an earlier one, jump to any version, and see what changed.',
      useWhen: [
        'An artifact has more than one version (inside AiArtifactCanvas, or a version history page).',
        'People need to compare a revision with the one before it (`onCompare` and `AiArtifactDiff`).',
      ],
      avoidWhen: [
        'Alternative replies to the same prompt: use the message branch switcher.',
        'A full audit history with authors and comments: use a version history or audit log.',
      ],
    },
    designPrinciples: {
      items: [
        'Position in words ("Version 2 of 3"), not just arrows.',
        'Previous / next for small steps, a select to jump; both are native controls.',
        'Changes are marked with strike-through and underline as well as colour.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Controlled: pass `value` and update it in `onChange`.',
        '`AiArtifactDiff` shows the changes between two texts: `mode="words"` (prose, via `diffWords`) or `mode="lines"` (an AiCodeBlock diff).',
        '`diffLines(before, after)` returns a unified line diff (" ", "-", "+" prefixes) for AiCodeBlock with `language="diff"`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'AiArtifactDiff mode',
          values: ['words', 'lines'],
          description:
            '`words` marks changed words inline; `lines` shows a line diff for code. Exposed as `data-mode`.',
        },
      ],
      notes: [
        'The stepper itself has one look; "Compare with previous" appears only with `onCompare`.',
      ],
    },
    states: {
      items: [
        {
          name: 'First / last version',
          attribute: 'disabled',
          description: 'Previous or next is disabled at the ends.',
        },
        {
          name: 'Comparing',
          attribute: 'aria-pressed="true"',
          description: 'The compare button is pressed while the diff shows.',
        },
        {
          name: 'No changes',
          description:
            'AiArtifactDiff says "No changes between these versions." when the texts match.',
        },
      ],
    },
    interaction: {
      items: [
        'Previous and next call `onChange` with the neighbouring version; the select calls it with the chosen one.',
        'Versions are sorted by number, whatever order they are given in.',
        'A change of `value` is announced politely ("Showing version 2 of 3"); the first render is not.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between previous, next, the select and compare.' },
        { keys: 'Enter / Space', action: 'Activates previous, next or compare.' },
        { keys: 'Arrow keys', action: 'Change the version in the select (native behaviour).' },
      ],
    },
    accessibility: {
      role: 'group',
      name: '"Versions" (`messages.label`)',
      items: [
        'Previous and next are icon buttons with names; the select is named "Choose a version".',
        'Version changes are announced through a polite status region.',
        'AiArtifactDiff is a named group; removed and added text starts with hidden "Removed:" and "Added:".',
      ],
    },
    responsive: {
      items: [
        'The controls wrap onto a second line on narrow screens and the select shrinks, so there is no sideways scroll at 390px.',
        'Diff text wraps anywhere; the line diff scrolls inside its code block.',
      ],
    },
    theme: {
      items: [
        'Diff colours use `--mn-color-ai-diff-add`, `-add-strong`, `-remove` and `-remove-strong`.',
        'Chevrons flip in right-to-left layouts.',
        'In forced colours changes keep their strike-through and underline; additions also get an outline.',
      ],
    },
    examples: {
      items: [
        { title: 'Versions with compare', stack: true, render: () => <VersionsDemo /> },
        {
          title: 'Code diff',
          stack: true,
          render: () => (
            <AiArtifactDiff
              mode="lines"
              from={1}
              to={2}
              before={'const total = items.length;\nreturn total;'}
              after={'const total = items.filter(Boolean).length;\nreturn total;'}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Keep every version the AI produced, so people can go back.',
        'Use `mode="lines"` for code and `words` for prose.',
      ],
      dont: [
        'Do not delete earlier versions when the AI revises an artifact.',
        'Do not use it for alternative replies to one prompt.',
      ],
    },
    related: {
      items: [
        { id: 'ai-artifact-canvas', reason: 'Shows this stepper in its toolbar.' },
        { id: 'ai-inline-diff', reason: 'Accept or reject suggested edits one by one.' },
        { id: 'ai-code-block', reason: 'Renders the line diff.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'form-field', 'ai-code-block', 'ai-edit-proposal'],
    },
    testing: {
      unit: [
        'Named group with position, previous / next, and a select listing sorted versions.',
        'Buttons step and disable at the ends; changes are announced; the select jumps.',
        'Compare calls `onCompare` with the previous and current versions.',
        '`diffLines` marks kept, removed and added lines; AiArtifactDiff marks words with hidden text, shows a code diff and a no-changes message.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
