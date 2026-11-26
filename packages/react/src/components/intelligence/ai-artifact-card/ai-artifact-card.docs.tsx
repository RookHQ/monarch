import { useState } from 'react';
import { AiArtifactCard, type AiArtifactPart } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const PARTS: AiArtifactPart[] = [
  { type: 'artifact', id: 'a1', kind: 'document', title: 'Q3 summary', version: 3 },
  { type: 'artifact', id: 'a2', kind: 'code', title: 'parse-invoice.ts', version: 1 },
  { type: 'artifact', id: 'a3', kind: 'html', title: 'Event landing page', version: 2 },
];

function ActiveDemo() {
  const [active, setActive] = useState<string | null>('a1');
  return (
    <div style={{ display: 'grid', gap: 'var(--mn-space-2)', maxWidth: '28rem' }}>
      {PARTS.map((part) => (
        <AiArtifactCard
          key={part.id}
          part={part}
          active={active === part.id}
          onOpen={(p) => setActive(p.id)}
        />
      ))}
    </div>
  );
}

export default {
  id: 'ai-artifact-card',
  title: 'AiArtifactCard',
  summary: 'The card a reply shows for an artifact it made: title, kind, version, and Open.',
  sections: {
    purpose: {
      summary:
        'When the AI produces a document, code, table, chart, image or web page, the reply shows this card instead of the whole thing. One click opens it in an AiArtifactCanvas.',
      useWhen: [
        'Rendering an `artifact` part of an AI message.',
        'Listing the artifacts a conversation produced.',
      ],
      avoidWhen: [
        'The content is short enough to read inline: render it in the message.',
        'Showing an uploaded file: use the attachment components.',
      ],
    },
    designPrinciples: {
      items: [
        'One control: the whole card is a single button, so there is one tab stop per artifact.',
        'The kind is shown as text and an icon, never as colour alone.',
        'The version is always visible, so people know which draft they are opening.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Native `<button>` attributes pass through and the ref is forwarded to it; `type` is always `button`.',
        '`AiArtifactKindGlyph` draws the kind icon for apps that show kinds elsewhere; `AI_ARTIFACT_CARD_MESSAGES` holds the English strings.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'part.kind',
          values: ['document', 'code', 'table', 'chart', 'image', 'html'],
          description: 'Picks the icon and the kind label. Exposed as `data-kind`.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Default', description: 'Shows "Open".' },
        {
          name: 'Active',
          attribute: 'data-active, aria-current="true"',
          description: 'The artifact is open in the canvas: AI-tinted background and "Showing".',
        },
        { name: 'Focus', attribute: ':focus-visible', description: 'Focus ring around the card.' },
      ],
    },
    interaction: {
      items: [
        'Click, Enter or Space calls `onOpen` with the part. The app opens the canvas.',
        'Focus stays on the card after opening; the canvas decides whether to move it.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the card.' },
        { keys: 'Enter / Space', action: 'Opens the artifact.' },
      ],
    },
    accessibility: {
      role: 'button',
      name: 'Its text: title, kind, "version 3" and the action, e.g. "Q3 summary Document, version 3, Open".',
      items: [
        'The short "v3" is hidden from assistive technology and "version 3" is read instead.',
        'The active card has `aria-current="true"`.',
        'Icons are decorative.',
      ],
    },
    responsive: {
      items: [
        'Fills its container up to 28rem; the title and kind truncate instead of wrapping, so it never scrolls sideways at 390px.',
        'At least 44px tall on coarse pointers.',
      ],
    },
    theme: {
      items: [
        'Surface, border and text tokens; the icon tile and active state use `--mn-color-ai-subtle`, `--mn-color-ai-strong` and `--mn-color-ai-border`.',
        'In forced colours it is a ButtonFace button with a ButtonText border; the active card has a 2px Highlight border.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Kinds',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 'var(--mn-space-2)', maxWidth: '28rem' }}>
              {PARTS.map((part) => (
                <AiArtifactCard key={part.id} part={part} onOpen={() => {}} />
              ))}
            </div>
          ),
        },
        { title: 'Active artifact', stack: true, render: () => <ActiveDemo /> },
        {
          title: 'With a description',
          stack: true,
          render: () => (
            <AiArtifactCard
              part={{
                type: 'artifact',
                id: 't',
                kind: 'table',
                title: 'Pipeline by stage',
                version: 4,
              }}
              description="12 rows · 5 columns"
              onOpen={() => {}}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Open the artifact in an AiArtifactCanvas next to the thread.',
        'Mark the artifact that is open with `active`.',
      ],
      dont: [
        'Do not put more buttons inside the card; put actions in the canvas.',
        'Do not hide the version.',
      ],
    },
    related: {
      items: [
        { id: 'ai-artifact-canvas', reason: 'Where the card opens the artifact.' },
        { id: 'ai-artifact-versions', reason: 'Steps between versions inside the canvas.' },
        { id: 'ai-message', reason: 'Renders artifact parts with this card.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Shows title, kind and version; the button name includes "version 3"; ref and props forwarded.',
        'Click, Enter and Space call `onOpen` with the part.',
        'Active state sets `aria-current` and `data-active`; strings are localisable.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
