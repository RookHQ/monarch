'use client';

import {
  Component,
  forwardRef,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  type ComponentType,
  type ErrorInfo,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { InfoGlyph } from '../../../internal/icons.js';
import { isSafeUrl } from '../../../internal/sanitize.js';
import type { AiUiPart } from '../ai-conversation/index.js';
import {
  schemaSummary,
  validateAiUiProps,
  type AiUiJsonSchema,
} from './ai-generative-ui.schema.js';
import { parsePartialJson } from './ai-generative-ui.partial.js';

export {
  validateAiUiProps,
  schemaSummary,
  type AiUiJsonSchema,
  type AiUiJsonType,
  type AiUiSchemaOptions,
  type AiUiSchemaResult,
} from './ai-generative-ui.schema.js';
export { parsePartialJson } from './ai-generative-ui.partial.js';

/* eslint-disable @typescript-eslint/no-explicit-any -- a registry holds components with any props; props reach them only after sanitising and validation. */

/** Checks the (already sanitised) props of one component. Return false to refuse to render it. */
export type AiUiValidator = (props: Record<string, unknown>) => boolean;

/**
 * The allow-list of components generative UI may render, by the name the
 * model uses. Build it with `createAiUiRegistry` so that names such as
 * `constructor` or `__proto__` can never resolve to anything.
 */
export type AiUiRegistry = Readonly<Record<string, ComponentType<any>>>;

/** Why a part was not rendered. */
export type AiUiRejection = 'unknown-component' | 'invalid-props' | 'render-error';

/**
 * A registry entry with a contract: the component, the JSON Schema its props
 * must match (unknown props are stripped), and a description for the model.
 */
export interface AiUiRegistryEntry<C extends ComponentType<any> = ComponentType<any>> {
  component: C;
  /** What the component accepts. Props that do not match are refused; unlisted props are stripped. */
  schema?: AiUiJsonSchema;
  /** One sentence for the model: when to use this component. */
  description?: string;
  /**
   * While props stream: `progressive` (default) renders what has arrived,
   * checked against the schema without `required`; `on-complete` waits.
   */
  streaming?: 'progressive' | 'on-complete';
}

type EntryMeta = Omit<AiUiRegistryEntry, 'component'>;

/** Schemas and descriptions per registry, kept beside the frozen allow-list. */
const registryMeta = new WeakMap<object, Readonly<Record<string, EntryMeta>>>();

/** The component type of a registry input value. */
export type AiUiRegistryComponents<T> = {
  readonly [K in keyof T]: T[K] extends AiUiRegistryEntry<infer C> ? C : T[K];
};

const isEntry = (v: unknown): v is AiUiRegistryEntry =>
  typeof v === 'object' &&
  v !== null &&
  !('$$typeof' in v) &&
  'component' in v &&
  isComponent((v as { component: unknown }).component);

/**
 * Builds a generative UI allow-list: a frozen copy of `components` with no
 * prototype, so only the names you list resolve. Values must be React
 * components (functions, classes, `memo` or `forwardRef`) or entries
 * `{ component, schema, description }`; a string such as `"div"` or
 * `"iframe"` is refused, so the model can never pick a raw HTML element.
 */
export function createAiUiRegistry<
  T extends Record<string, ComponentType<any> | AiUiRegistryEntry>,
>(components: T): AiUiRegistryComponents<T> {
  const registry = Object.create(null) as Record<string, ComponentType<any>>;
  const meta = Object.create(null) as Record<string, EntryMeta>;
  for (const name of Object.keys(components)) {
    const value = components[name];
    if (isEntry(value)) {
      const { component, ...rest } = value;
      registry[name] = component;
      meta[name] = rest;
      continue;
    }
    if (!isComponent(value)) {
      throw new TypeError(`createAiUiRegistry: "${name}" is not a React component.`);
    }
    registry[name] = value;
  }
  const frozen = Object.freeze(registry);
  registryMeta.set(frozen, Object.freeze(meta));
  return frozen as AiUiRegistryComponents<T>;
}

function metaOf(registry: AiUiRegistry, name: string): EntryMeta | undefined {
  const meta = registryMeta.get(registry);
  return meta && Object.prototype.hasOwnProperty.call(meta, name) ? meta[name] : undefined;
}

export interface DescribeAiUiRegistryOptions {
  /** `text` (default): one line per component for a system prompt; `json`: name, description and schema. */
  format?: 'text' | 'json';
}

/**
 * A compact manifest of what generative UI may render, to put in the
 * model's system prompt: each component's name, description and props
 * (`- Chart: Bar chart of values. props: {title: string, data: array<…>}`).
 */
export function describeAiUiRegistry(
  registry: AiUiRegistry,
  options: DescribeAiUiRegistryOptions = {},
): string {
  const names = Object.keys(registry);
  if (options.format === 'json')
    return JSON.stringify(
      names.map((name) => {
        const m = metaOf(registry, name);
        return {
          name,
          ...(m?.description ? { description: m.description } : {}),
          ...(m?.schema ? { props: m.schema } : {}),
        };
      }),
    );
  return names
    .map((name) => {
      const m = metaOf(registry, name);
      const parts = [`- ${name}`];
      if (m?.description) parts.push(`: ${m.description}`);
      if (m?.schema) parts.push(`${m.description ? '' : ':'} props: ${schemaSummary(m.schema)}`);
      return parts.join('');
    })
    .join('\n');
}

/** A function, class, `memo` or `forwardRef` component. Strings (intrinsic HTML elements) are not. */
function isComponent(value: unknown): value is ComponentType<any> {
  if (typeof value === 'function') return true;
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { $$typeof?: unknown }).$$typeof === 'symbol'
  );
}

/** Looks a name up as an own property only, so inherited names never resolve. */
function lookup(registry: AiUiRegistry, name: unknown): ComponentType<any> | undefined {
  if (typeof name !== 'string' || name === '') return undefined;
  if (!Object.prototype.hasOwnProperty.call(registry, name)) return undefined;
  const value = registry[name];
  return isComponent(value) ? value : undefined;
}

// ---------------------------------------------------------------- props

/** Prop names that are never passed through, whatever the component. */
const BLOCKED_KEYS = new Set([
  'dangerouslySetInnerHTML',
  'ref',
  'key',
  'style',
  'className',
  'id',
  'srcDoc',
  'srcdoc',
  '__proto__',
  'constructor',
  'prototype',
]);
/** Props that hold a URL: kept only when `isSafeUrl` allows them. */
const URL_KEYS = new Set([
  'href',
  'src',
  'srcSet',
  'action',
  'formAction',
  'poster',
  'xlinkHref',
  'data',
  'url',
  'imageUrl',
  'avatar',
  'favicon',
  'cite',
  'background',
  'ping',
]);
const MAX_DEPTH = 8;
const MAX_ITEMS = 500;
const MAX_STRING = 20_000;

/** Event-handler-shaped names (`onClick`, `onerror`): data never becomes a handler. */
function isHandlerName(key: string) {
  return /^on[A-Za-z]/.test(key);
}

/** Schemes that are never data a component should receive, whatever the prop is called. */
const UNSAFE_SCHEMES = new Set([
  'javascript',
  'vbscript',
  'livescript',
  'data',
  'file',
  'blob',
  'filesystem',
  'about',
  'jar',
  'view-source',
  'intent',
  'chrome',
  'chrome-extension',
  'moz-extension',
  'ms-browser-extension',
]);

/**
 * Whether a string is shaped like a URL, whatever its prop is called: it
 * starts with a scheme (ignoring the whitespace and control characters
 * browsers ignore) and either has no spaces or names a scheme that runs code.
 */
export function isUrlShaped(value: string): boolean {
  // eslint-disable-next-line no-control-regex -- control characters are exactly what is removed.
  const compact = value.trim().replace(/[\u0000- \u007f-\u009f]/g, '');
  const scheme = /^([a-z][a-z0-9+.-]*):/i.exec(compact)?.[1]?.toLowerCase();
  if (!scheme) return false;
  return UNSAFE_SCHEMES.has(scheme) || !/\s/.test(value.trim());
}

function cleanValue(value: unknown, key: string, depth: number): unknown {
  if (value === null) return null;
  switch (typeof value) {
    case 'string': {
      // By name (`href`, `src`…) and by shape: a URL hidden in `items[3].link`
      // or `config.target` is held to the same rule.
      if ((URL_KEYS.has(key) || isUrlShaped(value)) && !isSafeUrl(value)) return undefined;
      return value.length > MAX_STRING ? value.slice(0, MAX_STRING) : value;
    }
    case 'number':
      return Number.isFinite(value) ? value : undefined;
    case 'boolean':
      return value;
    case 'object': {
      if (depth >= MAX_DEPTH) return undefined;
      if (Array.isArray(value)) {
        const out: unknown[] = [];
        for (const item of value.slice(0, MAX_ITEMS)) {
          const cleaned = cleanValue(item, key, depth + 1);
          if (cleaned !== undefined) out.push(cleaned);
        }
        return out;
      }
      return cleanObject(value, depth + 1);
    }
    default:
      // Functions, symbols, bigints and undefined are not data.
      return undefined;
  }
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) return false;
  const proto = Object.getPrototypeOf(value) as unknown;
  return proto === Object.prototype || proto === null;
}

function cleanObject(value: unknown, depth: number): Record<string, unknown> | undefined {
  // Only plain data objects: React elements, Dates, class instances and
  // anything with a `$$typeof` brand are dropped.
  if (!isPlainObject(value) || '$$typeof' in value) return undefined;
  const out: Record<string, unknown> = {};
  for (const key of Object.keys(value).slice(0, MAX_ITEMS)) {
    if (BLOCKED_KEYS.has(key) || isHandlerName(key)) continue;
    // Children may only be text; React escapes it.
    if (key === 'children' && typeof value[key] !== 'string' && typeof value[key] !== 'number') {
      continue;
    }
    const cleaned = cleanValue(value[key], key, depth);
    if (cleaned !== undefined) out[key] = cleaned;
  }
  return out;
}

/**
 * Turns model-supplied props into plain data a component can receive safely:
 * only strings, finite numbers, booleans, null, arrays and plain objects
 * survive, and `children` only as text. Drops `dangerouslySetInnerHTML`, `style`, `className`,
 * `id`, `ref`, `key`, `srcDoc`, every `on…` handler name, prototype keys,
 * functions, symbols and React elements, and every string at any depth that
 * is a URL by name (`href`, `src`, `action`…) or by shape (`javascript:…`,
 * `data:…`) and that `isSafeUrl` rejects: only http(s), mailto and relative
 * URLs pass. Nesting stops at 8 levels. Returns `null` when `props` is not a
 * plain object.
 */
export function sanitizeAiUiProps(props: unknown): Record<string, unknown> | null {
  if (props === undefined || props === null) return {};
  return cleanObject(props, 0) ?? null;
}

// ---------------------------------------------------------------- render

/** Every string AiGenerativeUi shows. English defaults. */
export interface AiGenerativeUiMessages {
  /** Shown instead of a component that is not in the registry. */
  unknown: (name: string) => string;
  /** Shown when the props are not plain data or fail validation. */
  invalid: (name: string) => string;
  /** Shown when the component throws while rendering. */
  failed: (name: string) => string;
}

export const AI_GENERATIVE_UI_MESSAGES: AiGenerativeUiMessages = {
  unknown: () => 'This part of the answer can’t be shown here.',
  invalid: () => 'This part of the answer had data that couldn’t be shown.',
  failed: () => 'This part of the answer couldn’t be displayed.',
};

/** What a generated component sends through its `emit` prop. */
export interface AiUiEmitted {
  /** What happened, e.g. `submit`, `select`, `confirm`. */
  type: string;
  /** Plain data; sanitised like props before it reaches the host. */
  payload?: unknown;
}

/** An action from a generated component, as the host's `onAction` receives it. */
export interface AiUiAction extends AiUiEmitted {
  /** The registered name of the component that emitted it. */
  component: string;
  /** `part.id`, when the part has one. */
  partId?: string;
  /** `part.toolCallId`: send the action back as that tool call's result. */
  toolCallId?: string;
}

/** The prop a generated component receives when the host listens for actions. */
export type AiUiEmit = (action: AiUiEmitted) => void;

export interface AiGenerativeUiProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'children' | 'part'
> {
  /** The `ui` part from the message: a component name and its props. */
  part: AiUiPart;
  /** The allow-list, from `createAiUiRegistry`. Only these names render. */
  registry: AiUiRegistry;
  /**
   * Validators by component name, run on the sanitised props. A component
   * without one renders with any sanitised props; give every component one.
   */
  validate?: Readonly<Record<string, AiUiValidator>> | undefined;
  /** Called when a part is refused or fails, for logging. */
  onReject?: ((reason: AiUiRejection, part: AiUiPart, error?: unknown) => void) | undefined;
  /**
   * The action channel: when set, the component receives an `emit(action)`
   * prop, and every action arrives here with its payload sanitised. Send it
   * back to the agent, e.g. `submitToolResult(action.toolCallId, { output: action })`
   * or as a new message.
   */
  onAction?: ((action: AiUiAction) => void) | undefined;
  /** Replaces the built-in notice. Never render the part's props as HTML. */
  renderFallback?: ((reason: AiUiRejection, part: AiUiPart) => ReactNode) | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiGenerativeUiMessages> | undefined;
}

interface BoundaryProps {
  fallback: ReactNode;
  onError: (error: unknown) => void;
  children: ReactNode;
}

/** Catches a registered component that throws, so one bad part never breaks the thread. */
class AiUiBoundary extends Component<BoundaryProps, { failed: boolean }> {
  override state = { failed: false };
  static getDerivedStateFromError() {
    return { failed: true };
  }
  override componentDidCatch(error: unknown, _info: ErrorInfo) {
    this.props.onError(error);
  }
  override render() {
    return this.state.failed ? this.props.fallback : this.props.children;
  }
}

function Notice({ reason, text }: { reason: AiUiRejection; text: string }) {
  return (
    <p className="mn-ai-generative-ui__notice" data-reason={reason}>
      <InfoGlyph className="mn-ai-generative-ui__notice-icon" aria-hidden />
      <span>{text}</span>
    </p>
  );
}

/**
 * Generative UI: renders a component the model asked for, but only when its
 * name is in the allow-listed `registry`. Props are treated as untrusted
 * data: sanitised (`sanitizeAiUiProps`) and then checked by `validate`.
 * Unknown names, invalid props and components that throw all render a short
 * neutral notice instead. Model output is never rendered as HTML.
 */
export const AiGenerativeUi = forwardRef<HTMLDivElement, AiGenerativeUiProps>(
  function AiGenerativeUi(
    {
      part,
      registry,
      validate,
      onReject,
      onAction,
      renderFallback,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...AI_GENERATIVE_UI_MESSAGES, ...messagesProp };
    const name = typeof part.component === 'string' ? part.component : '';
    const target = lookup(registry, name);
    const meta = target ? metaOf(registry, name) : undefined;
    const streaming = part.state === 'streaming';
    const waiting = streaming && meta?.streaming === 'on-complete';
    // Streaming props may be JSON text still arriving; finished ones must be data.
    const raw = useMemo(
      () =>
        typeof part.props === 'string' && streaming
          ? (parsePartialJson(part.props) ?? {})
          : part.props,
      [part.props, streaming],
    );
    const props = useMemo(() => {
      const clean = sanitizeAiUiProps(raw);
      if (!clean || !meta?.schema) return clean;
      const checked = validateAiUiProps(meta.schema, clean, { partial: streaming });
      return checked.ok ? (checked.value as Record<string, unknown>) : null;
    }, [raw, meta, streaming]);

    // Stable across renders: reads the latest handler and part through a ref.
    const latest = useRef({ onAction, part, name });
    latest.current = { onAction, part, name };
    const emit = useCallback<AiUiEmit>((action) => {
      const { onAction: handler, part: current, name: component } = latest.current;
      if (!handler || !action || typeof action.type !== 'string' || !action.type) return;
      const payload =
        action.payload === undefined ? undefined : cleanValue(action.payload, 'payload', 0);
      handler({
        type: action.type,
        component,
        ...(payload !== undefined ? { payload } : {}),
        ...(current.id ? { partId: current.id } : {}),
        ...(current.toolCallId ? { toolCallId: current.toolCallId } : {}),
      });
    }, []);

    let reason: AiUiRejection | null = null;
    if (!target) reason = 'unknown-component';
    else if (props === null) reason = 'invalid-props';
    else {
      const check =
        validate && Object.prototype.hasOwnProperty.call(validate, name)
          ? validate[name]
          : undefined;
      if (check) {
        let ok = false;
        try {
          ok = check(props) === true;
        } catch {
          ok = false;
        }
        if (!ok) reason = 'invalid-props';
      }
    }

    const fallback = (why: AiUiRejection) =>
      renderFallback ? (
        renderFallback(why, part)
      ) : (
        <Notice
          reason={why}
          text={
            why === 'unknown-component'
              ? m.unknown(name)
              : why === 'invalid-props'
                ? m.invalid(name)
                : m.failed(name)
          }
        />
      );

    // A part that is still streaming is not refused yet: it may still become valid.
    const pending = streaming && reason === 'invalid-props';

    // Report a refusal after render, once per outcome and part.
    useEffect(() => {
      if (reason && !pending) onReject?.(reason, part);
      // eslint-disable-next-line react-hooks/exhaustive-deps -- report on outcome changes only.
    }, [reason, part, pending]);

    const Target = target;
    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-generative-ui', className)}
        data-component={name || undefined}
        data-state={pending || waiting ? 'streaming' : reason ? 'rejected' : 'rendered'}
        aria-busy={streaming || undefined}
      >
        {pending || waiting ? null : reason || !Target || !props ? (
          fallback(reason ?? 'unknown-component')
        ) : (
          <AiUiBoundary
            key={name}
            fallback={fallback('render-error')}
            onError={(error) => onReject?.('render-error', part, error)}
          >
            <Target {...props} {...(onAction ? { emit } : {})} />
          </AiUiBoundary>
        )}
      </div>
    );
  },
);
