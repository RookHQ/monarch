import { Provenance } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const NOW = '2026-03-10T12:00:00Z';

function SourceIcon() {
  return (
    <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
      <ellipse cx="8" cy="4" rx="5" ry="2" />
      <path d="M3 4v8c0 1.1 2.2 2 5 2s5-.9 5-2V4M3 8c0 1.1 2.2 2 5 2s5-.9 5-2" />
    </svg>
  );
}

export default {
  id: 'provenance',
  title: 'Provenance',
  summary: 'Where a value came from and how fresh it is: “Registry · 2 hours ago”.',
  sections: {
    purpose: {
      summary:
        'Labels a value with its source and the time it was last checked, so readers know whether to trust it and when it might be out of date. Needed by: Atlas (domain fields from registries, DNS, imports and manual entry).',
      useWhen: [
        'A field was filled from an external system, an import or a computation rather than typed by the user.',
        'Several sources can supply the same field and the reader needs to know which one won.',
      ],
      avoidWhen: [
        'The freshness of a whole data set or sync: use FreshnessIndicator.',
        'Marking AI-written content: use AiGeneratedBadge.',
        'A full change history: use AuditLogViewer or VersionHistoryPanel.',
      ],
    },
    designPrinciples: {
      items: [
        'Quiet: small muted text, never louder than the value it describes.',
        'Words, not codes: pass a readable source label; map internal source keys to labels and icons in the product.',
        'The age is relative (“2 hours ago”) with the exact time on hover and focus, from RelativeTime.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<span>` (ref); nothing when `source` is empty. Other span attributes pass through.',
        'The time is a RelativeTime, so `now` and `locale` make server and client text identical.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Atlas `Provenance`): `source` (a key such as `rdap`) → map it to a label and icon in Atlas and pass `source={label}` and `icon={<Icon />}`; the `label` override becomes `source`; `checkedAt` is unchanged. The `title` tooltip is replaced by RelativeTime’s absolute-time tooltip.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['chip', 'inline'],
          description:
            '`chip` (default) is a small bordered tag next to a value; `inline` is plain muted text inside a sentence. Exposed as `data-variant`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Source and time',
          description: 'Source label, a middle dot and the relative time.',
        },
        {
          name: 'Source only',
          description: 'When `checkedAt` is missing or invalid, only the source shows.',
        },
        { name: 'Linked', description: 'With a safe `href`, the source label is a link.' },
      ],
    },
    interaction: {
      items: [
        'Hovering or focusing the time shows the absolute date and time; Escape hides it.',
        'With `href`, the source label is an ordinary link.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action: 'Moves to the source link (if any), then to the time when `timeTooltip` is on.',
        },
        { keys: 'Escape', action: 'Hides the time tooltip.' },
      ],
    },
    accessibility: {
      role: 'none (text), with an optional link and a `<time>` element',
      name: 'Read as “Source: Registry, checked 2 hours ago” through visually hidden words (`messages`).',
      items: [
        'The icon and the middle dot are hidden from assistive technology.',
        'The `<time datetime>` carries the exact moment.',
        'Unsafe `href` values are ignored, so the source renders as text.',
      ],
    },
    responsive: {
      items: ['Wraps inside its container; long source names break instead of overflowing.'],
    },
    theme: {
      items: [
        'Muted text on `--mn-color-bg-subtle` with a `--mn-color-border-subtle` border for the chip; links use `--mn-color-text-link`.',
        'Forced colours: CanvasText border and text, LinkText for the link.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Next to values',
          stack: true,
          render: () => (
            <dl
              style={{
                display: 'grid',
                gridTemplateColumns: 'auto 1fr',
                gap: '8px 16px',
                margin: 0,
              }}
            >
              <dt>Expires</dt>
              <dd
                style={{
                  margin: 0,
                  display: 'flex',
                  gap: 8,
                  flexWrap: 'wrap',
                  alignItems: 'center',
                }}
              >
                12 Aug 2027
                <Provenance
                  source="Registry"
                  icon={<SourceIcon />}
                  checkedAt="2026-03-10T10:00:00Z"
                  now={NOW}
                />
              </dd>
              <dt>Owner</dt>
              <dd
                style={{
                  margin: 0,
                  display: 'flex',
                  gap: 8,
                  flexWrap: 'wrap',
                  alignItems: 'center',
                }}
              >
                Example team
                <Provenance source="Entered by hand" />
              </dd>
            </dl>
          ),
        },
        {
          title: 'Inline and linked',
          render: () => (
            <p style={{ margin: 0 }}>
              Last checked by{' '}
              <Provenance
                variant="inline"
                source="the import"
                href="#imports"
                checkedAt="2026-03-08T09:00:00Z"
                now={NOW}
              />
            </p>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use one wording per source across the product.',
        'Pass `now` when rendering on the server.',
      ],
      dont: [
        'Do not show internal source keys (“rdap”, “calc_v2”).',
        'Do not add provenance to values the user just typed.',
      ],
    },
    related: {
      items: [
        { id: 'freshness-indicator', reason: 'The freshness of a whole data set or sync.' },
        { id: 'relative-time', reason: 'The time inside it.' },
        { id: 'ai-generated-badge', reason: 'Marks AI-written content.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['relative-time'] },
    testing: {
      unit: [
        'Reads as source and age, with a `<time datetime>`.',
        'Shows the absolute time on keyboard focus.',
        'Leaves out invalid times; renders nothing without a source; inline variant.',
        'Links only to safe URLs.',
        'Custom messages.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
