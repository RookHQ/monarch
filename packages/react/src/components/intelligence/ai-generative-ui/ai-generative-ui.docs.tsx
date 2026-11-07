import { Badge, Card, CardContent, CardHeader, CardTitle } from '@rookcom/monarch-react';
import { AiGenerativeUi, createAiUiRegistry, type AiUiPart } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function MetricCard({ label, value, change }: { label?: string; value?: string; change?: string }) {
  return (
    <Card style={{ maxWidth: '18rem' }}>
      <CardHeader>
        <CardTitle>{label}</CardTitle>
      </CardHeader>
      <CardContent>
        <p
          style={{
            margin: 0,
            fontSize: 'var(--mn-text-2xl)',
            fontWeight: 'var(--mn-weight-semibold)',
          }}
        >
          {value}
        </p>
        {change ? <Badge tone="success">{change}</Badge> : null}
      </CardContent>
    </Card>
  );
}

function StatusBadge({ label, tone }: { label?: string; tone?: 'success' | 'warning' | 'danger' }) {
  return <Badge tone={tone ?? 'neutral'}>{label}</Badge>;
}

// The allow-list: only these names can ever render.
const registry = createAiUiRegistry({ MetricCard, StatusBadge });

const validate = {
  MetricCard: (p: Record<string, unknown>) =>
    typeof p.label === 'string' && typeof p.value === 'string',
  StatusBadge: (p: Record<string, unknown>) =>
    typeof p.label === 'string' &&
    ['success', 'warning', 'danger', undefined].includes(p.tone as string),
};

const metric: AiUiPart = {
  type: 'ui',
  component: 'MetricCard',
  props: { label: 'Active projects', value: '42', change: '+6 this month' },
};
const unknown: AiUiPart = {
  type: 'ui',
  component: 'iframe',
  props: { srcDoc: '<script>alert(1)</script>' },
};
const hostileProps: AiUiPart = {
  type: 'ui',
  component: 'StatusBadge',
  props: {
    label: '<b>On track</b>',
    tone: 'success',
    dangerouslySetInnerHTML: { __html: '<img src=x onerror=alert(1)>' },
    onClick: 'alert(1)',
    style: { position: 'fixed', inset: 0 },
  },
};
const invalid: AiUiPart = { type: 'ui', component: 'MetricCard', props: { label: 7 } };

export default {
  id: 'ai-generative-ui',
  title: 'AiGenerativeUi',
  summary:
    'Renders a component the model asked for, only from an allow-list, with props as validated data.',
  sections: {
    purpose: {
      summary:
        'Lets an AI answer with real interface (a metric card, a status, a small chart) instead of text, without ever letting the model inject markup. The model names a component and gives data; only components you allow-listed can render.',
      useWhen: [
        'Rendering a `ui` part of an AI message.',
        'An agent returns structured results that a Monarch or product component displays better than prose.',
      ],
      avoidWhen: [
        'The model returns HTML: show it as code, or in AiArtifactCanvas’s sandboxed preview.',
        'The UI needs to perform actions on the model’s say-so: use tool calls with AiToolApproval.',
      ],
    },
    designPrinciples: {
      items: [
        'Allow-list only: names are looked up as own properties of a frozen, prototype-free registry, so `constructor` or `__proto__` resolve to nothing, and raw element names like `"iframe"` are refused.',
        'Props are data, never code or markup: `sanitizeAiUiProps` keeps only strings, finite numbers, booleans, null, arrays and plain objects, and drops `dangerouslySetInnerHTML`, `style`, `className`, `id`, `ref`, `key`, `srcDoc`, every `on…` name and unsafe URLs.',
        'Validate every component’s props; anything unexpected becomes a calm notice, not a broken thread.',
        'Failures are contained: a component that throws is caught and replaced by the notice.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`createAiUiRegistry(components)` returns the allow-list; it throws if a value is not a React component.',
        '`sanitizeAiUiProps(props)` is exported for apps that need the same rules elsewhere; it returns `null` for non-object props.',
        '`onReject(reason, part, error?)` reports `unknown-component`, `invalid-props` and `render-error` for logging.',
        '`children` survives only as text (React escapes it). Native `<div>` attributes pass through to the wrapper and the ref is forwarded to it.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: ['No variants: the look comes from the registered component, or the notice.'],
    },
    states: {
      items: [
        {
          name: 'Rendered',
          attribute: 'data-state="rendered"',
          description: 'The allow-listed component with sanitised, validated props.',
        },
        {
          name: 'Rejected',
          attribute: 'data-state="rejected"',
          description: 'Unknown name or invalid props: a neutral notice.',
        },
        {
          name: 'Failed',
          attribute: 'data-reason="render-error"',
          description: 'The component threw; the notice replaces it.',
        },
      ],
      notes: ['`data-component` carries the requested name; the notice carries `data-reason`.'],
    },
    interaction: {
      items: [
        'None of its own: interaction belongs to the registered component.',
        'Registered components receive data only; give them callbacks from your app, not from the model.',
      ],
    },
    keyboard: {
      items: [],
      notes: ['Not interactive itself; keyboard support is the registered component’s.'],
    },
    accessibility: {
      role: 'none (a wrapper); the notice is a paragraph',
      name: 'From the registered component',
      items: [
        'The surrounding AiMessageView identifies the content as AI-generated.',
        'The notice is text with a decorative icon, readable by everyone.',
      ],
    },
    responsive: {
      items: ['The wrapper never exceeds its container; the notice wraps long text.'],
    },
    theme: {
      items: [
        'The notice uses `--mn-color-bg-subtle`, `--mn-color-text-muted` and a dashed `--mn-color-border-default` border.',
        'In forced colours it is CanvasText on Canvas.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Allow-listed component',
          render: () => <AiGenerativeUi part={metric} registry={registry} validate={validate} />,
        },
        {
          title: 'Unknown component',
          render: () => <AiGenerativeUi part={unknown} registry={registry} validate={validate} />,
        },
        {
          title: 'Hostile props are stripped',
          render: () => (
            <AiGenerativeUi part={hostileProps} registry={registry} validate={validate} />
          ),
        },
        {
          title: 'Invalid props',
          render: () => <AiGenerativeUi part={invalid} registry={registry} validate={validate} />,
        },
      ],
    },
    doDont: {
      do: [
        'Register a few purpose-built components with narrow, typed props.',
        'Give every registered component a validator.',
        'Log `onReject` so you can see what the model asked for.',
      ],
      dont: [
        'Do not register components that render HTML strings, take URLs you have not checked, or act without confirmation.',
        'Do not build the registry from model output.',
        'Do not pass the part’s props anywhere else without `sanitizeAiUiProps`.',
      ],
    },
    related: {
      items: [
        { id: 'ai-message', reason: 'Renders `ui` parts with this component.' },
        { id: 'ai-artifact-canvas', reason: 'Sandboxed preview for AI-made HTML.' },
        { id: 'ai-tool-approval', reason: 'For actions the model wants to take.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'The registry is frozen, prototype-free, accepts memo / forwardRef and refuses strings and non-components.',
        'Unknown names (including `script`, `iframe`, `constructor`, `__proto__`, `toString`, empty) render the notice, call nothing and report `unknown-component`.',
        'A string in a hand-built registry never renders a raw element; text containing HTML is rendered as text.',
        'Props lose `dangerouslySetInnerHTML`, handlers, `style`, `className`, `id`, `ref`, `srcDoc`, unsafe URLs, functions, symbols, elements and class instances, at any depth up to a limit.',
        'Non-object props and failed or throwing validators render the invalid notice; a throwing component is contained.',
        'Custom fallback and messages; server rendering.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
