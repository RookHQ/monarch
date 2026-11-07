import { useState } from 'react';
import {
  AiRelatedEntities,
  type AiEntityRelation,
  type AiRelatedEntity,
} from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const ENTITIES: AiRelatedEntity[] = [
  { id: 'northwind', label: 'Northwind', kind: 'org', weight: 0.92 },
  { id: 'jordan', label: 'Jordan Lee', kind: 'person', weight: 0.74 },
  { id: 'sam', label: 'Sam Rivera', kind: 'person', weight: 0.58 },
  { id: 'q3-plan', label: 'Q3 plan', kind: 'document', weight: 0.51 },
  { id: 'harbor', label: 'Harbor office', kind: 'place', weight: 0.33 },
];

const RELATIONS: AiEntityRelation[] = [
  { from: 'jordan', to: 'northwind', label: 'works at' },
  { from: 'sam', to: 'northwind', label: 'works at' },
  { from: 'q3-plan', to: 'northwind', label: 'mentions' },
  { from: 'jordan', to: 'q3-plan', label: 'wrote' },
  { from: 'northwind', to: 'harbor', label: 'based at' },
];

function SelectDemo({ variant }: { variant: 'list' | 'graph' }) {
  const [selected, setSelected] = useState<string | null>(null);
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', maxWidth: '24rem', width: '100%' }}>
      <AiRelatedEntities
        variant={variant}
        entities={ENTITIES}
        relations={RELATIONS}
        onSelect={(entity) => setSelected(entity.label)}
      />
      <p style={{ margin: 0, fontSize: 'var(--mn-text-xs)' }}>Selected: {selected ?? 'none'}</p>
    </div>
  );
}

export default {
  id: 'ai-related-entities',
  title: 'AiRelatedEntities',
  summary:
    'People, organisations and records the AI found related, as a ranked list or a radial graph.',
  sections: {
    purpose: {
      summary:
        'Shows the entities an AI extracted or linked from the current content, ranked by relevance and with how they relate, so people can jump to them or check the AI’s reading.',
      useWhen: [
        'An AI summary or analysis surfaces people, organisations, projects or records connected to what the person is viewing.',
        'People should be able to open a related record from an AI answer.',
        'A small set of connections (up to about a dozen) is easier to grasp as a picture (`variant="graph"`).',
      ],
      avoidWhen: [
        'Large or deep networks: use a dedicated graph view; this component draws one ring.',
        'Sources the answer cites: use AiSourceList.',
        'Choosing an entity to act on in a form: use EntityPicker.',
      ],
    },
    designPrinciples: {
      items: [
        'A list first: it reads, wraps and scales; the graph is an optional view of the same data.',
        'No physics: the graph is a deterministic radial layout, so it renders on the server and never moves.',
        'Every node carries its full meaning in its name (kind, relevance and relations), so the graph is not visual-only.',
        'Relevance is shown as a bar with text, never colour alone.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`entities` is sorted by `weight` (0–1, clamped), highest first; entities without a weight keep their order after weighted ones. `sortAiRelatedEntities(entities)` is exported.',
        '`relations` link entities by id; relations to unknown ids are ignored.',
        'In the graph the centre is `centerId` or the highest weight; the others sit on one ring, clockwise from the top in weight order. Node size follows weight.',
        'With more than `maxGraphNodes` entities (default 12) the graph variant renders the list instead; `data-variant` reports what was rendered.',
        '`kind` accepts any string; known kinds (person, org, project, document, record, place, other) get English labels through `messages.kind`.',
        'Native `<div>` attributes pass through to the root and the ref is forwarded to it.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['list', 'graph'],
          description:
            '`list` is a ranked list with kind, relations and a relevance bar. `graph` is an SVG radial diagram with lines for relations. Exposed as `data-variant`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Interactive',
          attribute: 'data-interactive',
          description:
            'With `onSelect`, list entries are buttons and graph nodes are focusable buttons.',
        },
        {
          name: 'Graph fallback',
          attribute: 'data-variant="list"',
          description:
            'The graph was asked for but there are too many entities, so the list is shown.',
        },
        {
          name: 'Centre',
          attribute: 'data-center',
          description: 'The graph node in the middle, drawn larger in the solid AI colour.',
        },
        { name: 'Empty', description: 'Shows "No related entities".' },
      ],
    },
    interaction: {
      items: [
        'With `onSelect`, clicking a list entry or graph node calls it with the entity.',
        'Without `onSelect` the component is display only.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab / Shift+Tab', action: 'Moves between entities when they are selectable.' },
        {
          keys: 'Enter / Space',
          action: 'Selects the focused entity (list button or graph node).',
        },
      ],
      notes: ['Without `onSelect` nothing is focusable.'],
    },
    accessibility: {
      role: 'list (list variant); group of img or button nodes (graph variant)',
      name: '`messages.label` ("Related entities")',
      items: [
        'Graph nodes are named with the label, kind, relevance and relations, e.g. "Northwind. Organization. Relevance 92%. Related to: works at Jordan Lee". Edges and node text are hidden from assistive technology because the names already say it.',
        'Relevance bars are decorative; a visually hidden "Relevance 74%" is read with each list entry.',
        'Graph nodes show a focus ring on the circle when focused from the keyboard.',
      ],
    },
    responsive: {
      items: [
        'The list fills its container and wraps names and relations, so there is no horizontal scroll at 390px.',
        'The graph scales with its viewBox up to 22rem wide; long labels are shortened in the drawing (the full name stays accessible).',
        'On coarse pointers list buttons are at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Nodes use `--mn-color-ai-subtle` with an `--mn-color-ai-border` stroke; the centre and relevance bars use `--mn-color-ai-solid`.',
        'Edges use `--mn-color-border-strong`; list entries sit on `--mn-color-bg-surface`.',
        'In forced colours nodes are Canvas with CanvasText strokes, edges are CanvasText and focus is Highlight.',
      ],
    },
    examples: {
      items: [
        {
          title: 'List',
          stack: true,
          render: () => (
            <div style={{ maxWidth: '24rem', width: '100%' }}>
              <AiRelatedEntities entities={ENTITIES} relations={RELATIONS} />
            </div>
          ),
        },
        { title: 'Selectable list', stack: true, render: () => <SelectDemo variant="list" /> },
        { title: 'Graph', stack: true, render: () => <SelectDemo variant="graph" /> },
      ],
    },
    doDont: {
      do: [
        'Use real weights from the model or your ranking; leave `weight` out when there is none.',
        'Name relations with short verbs ("works at", "mentions").',
        'Open the entity’s own page from `onSelect`.',
      ],
      dont: [
        'Do not use the graph for more than about a dozen entities.',
        'Do not present AI-inferred relations as confirmed facts; label the panel as AI-generated nearby.',
        'Do not rely on node colour or size alone to carry meaning.',
      ],
    },
    related: {
      items: [
        { id: 'ai-source-list', reason: 'The sources an answer cites.' },
        { id: 'entity-picker', reason: 'Choosing an entity in a form.' },
        { id: 'ai-memory-panel', reason: 'What the AI remembers about entities.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Sorts by weight with unweighted entities last.',
        'Named, ranked list with kinds, relevance text and relations; unknown kinds shown as given; ref and prop forwarding.',
        'Selectable list entries are buttons that work with click and Enter.',
        'Graph: named group, nodes named with kind, relevance and relations, centre placement and one line per relation.',
        '`centerId`, and graph nodes select with Enter, Space and click.',
        'Falls back to the list above `maxGraphNodes`; empty state and localised strings.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
