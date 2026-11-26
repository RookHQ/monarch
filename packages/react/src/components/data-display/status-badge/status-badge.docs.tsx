import { StatusBadge, StatusRegistryProvider, defineStatuses } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { ClockIcon } from '../../../internal/docs-demo.js';

const health = ['operational', 'degraded', 'partial', 'down', 'maintenance', 'unknown'] as const;
const generic = ['success', 'warning', 'danger', 'info', 'neutral'] as const;

// Built on first render: docs files are also imported by Node scripts that do not compile JSX at module scope.
let requestCache: ReturnType<typeof buildRequestStatuses> | undefined;
const requestStatuses = () => (requestCache ??= buildRequestStatuses());
const buildRequestStatuses = () =>
  defineStatuses({
    open: { label: 'Open', tone: 'info' },
    waiting: { label: 'Waiting on requester', tone: 'warning', icon: <ClockIcon /> },
    resolved: { label: 'Resolved', tone: 'success' },
  });

const workspaceStatuses = defineStatuses({
  triage: { label: 'Triage', tone: 'neutral', color: '#7c3aed' },
  review: { label: 'In review', tone: 'neutral', color: '#facc15' },
  shipped: { label: 'Shipped', tone: 'neutral', color: '#0d9488' },
});

export default {
  id: 'status-badge',
  title: 'StatusBadge',
  summary:
    'Typed status registry: label, tone and a distinct icon or dot, so colour is never the only signal.',
  sections: {
    purpose: {
      summary:
        'Shows the state of a thing (a service, a request, a sync) from a typed registry that maps each status value to a human label, a semantic tone and an icon. Products add their own statuses, including workspace-defined colours, which pass through a contrast guard.',
      useWhen: [
        'A value comes from an enum (`partial`, `pending_delete`) and needs a consistent label, tone and icon everywhere it appears.',
        'Service health: operational, degraded, partial outage, down, maintenance, unknown.',
        'Users or workspaces define their own statuses with colours.',
      ],
      avoidWhen: [
        'A free-form tag or count with no status meaning: use Badge.',
        'The status must be changed in place: put a Select or DropdownMenu next to it (an editable picker is not part of StatusBadge).',
      ],
    },
    designPrinciples: {
      items: [
        'Colour is never the only signal: every badge has text, and the built-in statuses have distinct icon shapes (check, triangle, half-filled circle, cross, wrench, dashed question).',
        'One registry, one meaning: define a status once with `defineStatuses` and every screen shows it the same way.',
        'Never a raw enum. Unknown values render in the neutral look with a readable label ("Pending delete").',
        'Custom colours mark, text reads: a custom colour tints only the dot, icon and border, and is adjusted per theme until it reaches the non-text contrast target.',
        'Calm by default: `subtle` and `dot` for tables and lists; `solid` only for the one status that matters most on a screen.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`builtInStatuses` is the shipped registry. `defineStatuses(defs, { extend? })` returns a typed registry and rejects colours that are not `#rrggbb`.',
        '`StatusRegistryProvider` makes a registry available below it; nested providers add up. Lookup order: the `statuses` prop, the nearest provider, then the built-in set.',
        '`resolveStatus(status, ...registries)` and `formatStatusLabel(status)` are the same lookup and label formatting, for legends and tooltips.',
        '`checkStatusColor(hex)` is the contrast guard; it returns the per-scheme colours, the readable text colour for solid fills, and the original ratios.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`status` autocompletes the built-in keys and accepts any string from your own registry.',
        '`StatusDefinition.description` is data for the product’s tooltip or legend; StatusBadge does not render it, because a tooltip on a non-focusable badge is not reachable by keyboard.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'variant',
          values: ['subtle', 'outline', 'solid', 'dot'],
          description:
            'Tinted pill (default), bordered pill, filled pill, or a coloured dot with plain text. Exposed as `data-variant`.',
        },
        { prop: 'size', values: ['sm', 'md'], description: 'Height and text size. Default `md`.' },
        {
          prop: 'tone (registry)',
          values: ['neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description: 'Set per status in the registry, exposed as `data-tone`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Known status',
          attribute: 'data-status, data-tone',
          description: 'Label, tone and icon from the registry.',
        },
        {
          name: 'Unknown status',
          attribute: 'data-known="false"',
          description: 'Neutral tone, dashed question icon, readable label from the raw value.',
        },
        {
          name: 'Custom colour',
          attribute: 'data-custom-color',
          description:
            'Guarded `--mn-status-light` and `--mn-status-dark` colours mark the dot, icon and border. A custom status without an icon gets a dot, so the colour always shows.',
        },
      ],
      notes: ['Not interactive: no hover, focus or disabled states.'],
    },
    interaction: {
      items: [],
      notes: ['Not interactive. Wrap it in a button or link when it should do something.'],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable; the text is read in the normal reading order.'],
    },
    accessibility: {
      role: 'none (inline text)',
      name: 'Its visible text.',
      items: [
        'Icons and dots are `aria-hidden`; the label alone carries the meaning.',
        'Distinct icon shapes keep built-in statuses apart for people who cannot tell the colours apart (WCAG 1.4.1).',
        'Custom colours are guarded to 3:1 against the surface in light and dark, and to the more-contrast target in those themes (WCAG 1.4.11). Solid fills pick a text colour of at least 4.5:1.',
        'In forced-colours mode the border and dot use system colours.',
      ],
    },
    responsive: {
      items: [
        'Never wraps; long labels truncate with an ellipsis inside a narrow cell (`max-width: 100%`).',
        'Same size at every viewport width.',
      ],
    },
    theme: {
      items: [
        'Tones use the Monarch intent tokens (`--mn-color-<tone>-subtle`, `-strong`, `-border`, `-solid`), so all themes work.',
        'Custom colours carry a light and a dark value; the dark one applies under `.dark` or `[data-mn-theme="dark"]`.',
        'The `accent` tone follows the product accent; the status tones do not.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Service health',
          render: () => (
            <>
              {health.map((s) => (
                <StatusBadge key={s} status={s} />
              ))}
            </>
          ),
        },
        {
          title: 'Variants',
          stack: true,
          render: () => (
            <>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--mn-space-2)' }}>
                {generic.map((s) => (
                  <StatusBadge key={s} status={s} variant="outline" />
                ))}
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--mn-space-3)' }}>
                {health.map((s) => (
                  <StatusBadge key={s} status={s} variant="dot" />
                ))}
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--mn-space-2)' }}>
                <StatusBadge status="down" variant="solid" />
                <StatusBadge status="operational" variant="solid" size="sm" />
              </div>
            </>
          ),
        },
        {
          title: 'Your own statuses',
          render: () => (
            <StatusRegistryProvider statuses={requestStatuses()}>
              <StatusBadge status="open" />
              <StatusBadge status="waiting" />
              <StatusBadge status="resolved" />
              <StatusBadge status="pending_delete" />
            </StatusRegistryProvider>
          ),
        },
        {
          title: 'Custom colours, contrast-guarded',
          render: () => (
            <StatusRegistryProvider statuses={workspaceStatuses}>
              <StatusBadge status="triage" variant="dot" />
              <StatusBadge status="review" variant="dot" />
              <StatusBadge status="shipped" />
              <StatusBadge status="review" variant="outline" />
              <StatusBadge status="triage" variant="solid" />
            </StatusRegistryProvider>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Define each product vocabulary once with `defineStatuses` and share it through a provider.',
        'Translate with the registry label or the `label` prop, never by showing the enum.',
        'Use `dot` in dense tables and `subtle` elsewhere.',
      ],
      dont: [
        'Do not hide the label to save space; colour alone fails people who cannot see it.',
        'Do not hand-pick raw palette colours for statuses; use a tone, or a guarded custom colour.',
        'Do not put explanations only in a `title` attribute; show them in text or a focusable tooltip trigger.',
      ],
    },
    related: {
      items: [
        { id: 'badge', reason: 'Free-form labels and counts without a status meaning.' },
        { id: 'tooltip', reason: 'Explain a status from a focusable trigger next to it.' },
        { id: 'alert', reason: 'A status that needs a message and an action.' },
      ],
    },
    dependencies: {
      packages: ['@rookcom/monarch-tokens', 'react'],
      components: [],
      notes: [
        'Uses the tokens package’s colour maths (`contrast`, `ensureContrast`) and theme surfaces for the contrast guard; it is already a peer dependency.',
      ],
    },
    testing: {
      unit: [
        'Renders label, tone and a decorative icon for a built-in status.',
        'Every built-in status has a label and an icon.',
        'Unknown and null values render a readable label in the neutral look.',
        'Dot variant shows a dot and text without the icon.',
        'Icon slot replaces or hides the icon; `label` overrides the registry label.',
        'Products extend the registry through a provider and the `statuses` prop; `defineStatuses` extends the built-in set and rejects malformed colours.',
        'Lookup order and label formatting.',
        'Contrast guard: keeps passing colours, adjusts failing ones to the target in all four themes, solid text at 4.5:1 or more, rejects non-hex input.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/status-badge'],
    },
  },
} satisfies ComponentDocs;
