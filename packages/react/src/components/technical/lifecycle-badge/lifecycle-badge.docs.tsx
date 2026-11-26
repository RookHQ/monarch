import { LifecycleBadge } from '@rookcom/monarch-react/patterns';
import type { ComponentDocs } from '../../../internal/component-docs.js';

const apis = [
  { name: 'Projects API', lifecycle: 'stable' },
  { name: 'Reports API', lifecycle: 'preview' },
  { name: 'Exports API', lifecycle: 'planned' },
  { name: 'Legacy search API', lifecycle: 'deprecated' },
] as const;

export default {
  id: 'lifecycle-badge',
  title: 'LifecycleBadge',
  summary: 'The release stage of an API or feature (planned, preview, stable, deprecated).',
  sections: {
    purpose: {
      summary:
        'Shows where something is in its lifecycle: an API that is planned, in preview, stable or deprecated, or a custom stage that the product defines. Needed by: Developers (API catalogue and API pages), Dots (account lifecycle stages).',
      useWhen: [
        'A catalogue lists APIs, SDKs or features at different release stages.',
        'A record moves through stages the workspace configures, such as account lifecycle stages.',
      ],
      avoidWhen: [
        'The value is a live status (running, failed, down): use StatusBadge.',
        'The value is an HTTP method: use MethodBadge.',
        'The stages form a journey the user should see as steps: use a stepper.',
      ],
    },
    designPrinciples: {
      items: [
        'Quiet: a surface chip with a coloured dot and border, so a catalogue full of them stays calm.',
        'Text first: the label always says the stage; the dot only reinforces it.',
        'Built-in stages for the common API lifecycle, open to any custom stage key.',
        'A stage that no longer exists still renders its key, so old records never show a blank.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders a `<span>` and forwards its ref and HTML attributes to it.',
        '`formatLifecycleLabel(key, messages)` returns the text shown for a key without a `label`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Built-in tones: planned neutral, preview info, stable success, deprecated warning.',
        'Migrating from the app copy (Developers `LifecycleBadge`): `lifecycle` and `className` are unchanged; the app showed "Live" for `stable`, so pass `messages={{ stable: "Live" }}`.',
        'Migrating from the app copy (Dots `LifecycleBadge`): `stageKey` becomes `lifecycle`; look the stage up in the product and pass `label={stage.label}` and `tone` mapped from the stage colour. With no `label`, the key is shown humanised, as before.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'tone',
          values: ['neutral', 'accent', 'info', 'success', 'warning', 'danger'],
          description:
            'Dot and border colour. Defaults to the stage’s built-in tone, else neutral.',
        },
        {
          prop: 'size',
          values: ['sm', 'md'],
          description: '`md` (default) or `sm` for dense rows.',
        },
        {
          prop: 'dot',
          values: ['true', 'false'],
          description: 'Show the leading dot. Default true.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Stage',
          attribute: 'data-lifecycle',
          description: 'The stage key as given, for styling or tests.',
        },
        { name: 'Tone', attribute: 'data-tone', description: 'The resolved tone.' },
      ],
    },
    interaction: {
      items: [],
      notes: [
        'Not interactive. A product that lets people change the stage puts a Select or menu next to it, or replaces it with one when the user can edit.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not focusable.'],
    },
    accessibility: {
      role: 'none (inline text)',
      name: 'Its label text.',
      items: [
        'The dot is `aria-hidden`; the stage is always in the text.',
        'Text uses the default text colour on the surface, so contrast does not depend on the tone.',
        'In forced-colours mode the border and dot use system colours.',
      ],
    },
    responsive: {
      items: [
        'Never wider than its container: a long custom label truncates with an ellipsis.',
        'Use `size="sm"` in dense table rows on small screens.',
      ],
    },
    theme: {
      items: [
        'Dot and border use the intent tokens for each tone, so every theme applies.',
        'The background is `--mn-color-bg-surface` and the text `--mn-color-text-default`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Built-in stages',
          render: () => (
            <>
              <LifecycleBadge lifecycle="planned" />
              <LifecycleBadge lifecycle="preview" />
              <LifecycleBadge lifecycle="stable" />
              <LifecycleBadge lifecycle="deprecated" />
            </>
          ),
        },
        {
          title: 'In a catalogue',
          stack: true,
          render: () => (
            <ul
              style={{
                listStyle: 'none',
                margin: 0,
                padding: 0,
                display: 'grid',
                gap: 'var(--mn-space-2)',
                maxWidth: '24rem',
              }}
            >
              {apis.map((api) => (
                <li
                  key={api.name}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 'var(--mn-space-3)',
                    fontSize: 'var(--mn-text-sm)',
                  }}
                >
                  <span>{api.name}</span>
                  <LifecycleBadge lifecycle={api.lifecycle} size="sm" />
                </li>
              ))}
            </ul>
          ),
        },
        {
          title: 'Custom stages',
          render: () => (
            <>
              <LifecycleBadge lifecycle="lead" label="Lead" tone="info" />
              <LifecycleBadge lifecycle="customer" label="Customer" tone="success" />
              <LifecycleBadge lifecycle="former_customer" />
            </>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Rename built-in stages through `messages` when a product uses different words.',
        'Pass `label` and `tone` from the product’s own stage definitions for custom stages.',
      ],
      dont: [
        'Do not use it for live health or request status.',
        'Do not rely on the dot colour alone to tell stages apart.',
      ],
    },
    related: {
      items: [
        { id: 'status-badge', reason: 'Live statuses with icons.' },
        { id: 'badge', reason: 'Other short labels.' },
        { id: 'method-badge', reason: 'HTTP methods on API operations.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Labels and tones for the four built-in stages.',
        'Labels from `messages` and `label`.',
        'Custom stage keys with a tone, and humanised keys without a label.',
        'The dot is hidden from assistive technology and can be turned off.',
        'Ref, className and size.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
