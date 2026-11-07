'use client';

import {
  forwardRef,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type KeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, ChevronRightGlyph, DashGlyph } from '../../../internal/icons.js';

/** What a knowledge source is. */
export type AiKnowledgeSourceKind = 'space' | 'doc' | 'app';

/** One source the AI can ground its answers in. A source with `children` is a folder-like group. */
export interface AiKnowledgeSource {
  /** Stable id, returned in `value` when the source is selected. */
  id: string;
  /** Name shown in the tree, e.g. "Handbook". */
  label: string;
  kind: AiKnowledgeSourceKind;
  /** Short secondary text, e.g. "24 documents". */
  description?: string | undefined;
  /** Sources inside this one, e.g. the documents in a space. */
  children?: readonly AiKnowledgeSource[] | undefined;
}

/** The check state of one source in the tree. */
export type AiKnowledgeCheckState = 'checked' | 'unchecked' | 'indeterminate';

/** The source's children, or undefined when it has none. */
function kids(source: AiKnowledgeSource): readonly AiKnowledgeSource[] | undefined {
  return source.children && source.children.length > 0 ? source.children : undefined;
}

/**
 * The check state of every source for a `value`. A source is checked when its
 * id is in `value`, when an ancestor's id is, or when all of its children are
 * checked; it is indeterminate when only some of its descendants are.
 */
export function aiKnowledgeScopeState(
  sources: readonly AiKnowledgeSource[],
  value: readonly string[],
): Map<string, AiKnowledgeCheckState> {
  const selected = new Set(value);
  const out = new Map<string, AiKnowledgeCheckState>();
  const visit = (node: AiKnowledgeSource, inherited: boolean): AiKnowledgeCheckState => {
    const self = inherited || selected.has(node.id);
    let state: AiKnowledgeCheckState;
    const children = kids(node);
    if (!children) {
      state = self ? 'checked' : 'unchecked';
    } else {
      const childStates = children.map((child) => visit(child, self));
      if (self || childStates.every((s) => s === 'checked')) state = 'checked';
      else if (childStates.every((s) => s === 'unchecked')) state = 'unchecked';
      else state = 'indeterminate';
    }
    out.set(node.id, state);
    return state;
  };
  for (const source of sources) visit(source, false);
  return out;
}

/**
 * The new `value` after a person toggles source `id`: a checked (or
 * indeterminate) source becomes unchecked with all its descendants, an
 * unchecked one becomes checked with all its descendants. The result lists
 * every fully checked source, groups included, in tree order; ids in `value`
 * that are not in the tree are kept at the end.
 */
export function toggleAiKnowledgeSource(
  sources: readonly AiKnowledgeSource[],
  value: readonly string[],
  id: string,
): string[] {
  const state = aiKnowledgeScopeState(sources, value);
  const checked = new Set<string>();
  for (const [key, s] of state) if (s === 'checked') checked.add(key);
  const known = new Set(state.keys());

  const find = (nodes: readonly AiKnowledgeSource[]): AiKnowledgeSource | undefined => {
    for (const node of nodes) {
      if (node.id === id) return node;
      const hit = node.children ? find(node.children) : undefined;
      if (hit) return hit;
    }
    return undefined;
  };
  const target = find(sources);
  if (!target) return [...value];

  const turnOn = state.get(id) === 'unchecked';
  const mark = (node: AiKnowledgeSource) => {
    if (turnOn) checked.add(node.id);
    else checked.delete(node.id);
    node.children?.forEach(mark);
  };
  mark(target);

  const result: string[] = [];
  // Recompute groups bottom-up, then list in tree order.
  const settle = (node: AiKnowledgeSource): boolean => {
    const children = kids(node);
    if (!children) return checked.has(node.id);
    const all = children.map(settle).every(Boolean);
    if (all) checked.add(node.id);
    else checked.delete(node.id);
    return all;
  };
  sources.forEach(settle);
  const collect = (node: AiKnowledgeSource) => {
    if (checked.has(node.id)) result.push(node.id);
    node.children?.forEach(collect);
  };
  sources.forEach(collect);
  for (const v of value) if (!known.has(v) && !result.includes(v)) result.push(v);
  return result;
}

/** Every string AiKnowledgeScope shows or announces. English defaults. */
export interface AiKnowledgeScopeMessages {
  /** Accessible name of the tree. */
  label: string;
  /** Visible name of each kind. */
  kinds: Record<AiKnowledgeSourceKind, string>;
  /** Accessible name of one source, e.g. "Handbook, Space". */
  itemLabel: (label: string, kind: string) => string;
  /** Summary under the tree, e.g. "3 sources selected". */
  summary: (count: number) => string;
  /** Shown when there are no sources. */
  empty: string;
}

const DEFAULT_MESSAGES: AiKnowledgeScopeMessages = {
  label: 'Knowledge sources',
  kinds: { space: 'Space', doc: 'Document', app: 'App' },
  itemLabel: (label, kind) => `${label}, ${kind}`,
  summary: (count) =>
    count === 0 ? 'No sources selected' : `${count} ${count === 1 ? 'source' : 'sources'} selected`,
  empty: 'No sources available',
};

export interface AiKnowledgeScopeProps extends Omit<
  HTMLAttributes<HTMLDivElement>,
  'onChange' | 'defaultValue'
> {
  /** The sources to choose from, as a tree (spaces containing documents, apps…). */
  sources: readonly AiKnowledgeSource[];
  /**
   * Ids of the selected sources (controlled). A group's id stands for
   * everything in it; `onChange` lists every fully selected source, groups
   * included.
   */
  value: readonly string[];
  /** Called with the new selection after a source is checked or unchecked. */
  onChange: (value: string[]) => void;
  /** Ids of the groups that start expanded. Default: none. */
  defaultExpanded?: readonly string[] | undefined;
  /** Expanded groups (controlled). */
  expanded?: readonly string[] | undefined;
  /** Called when a group is expanded or collapsed. */
  onExpandedChange?: ((expanded: string[]) => void) | undefined;
  /** Shows "3 sources selected" under the tree. Default true. */
  showSummary?: boolean | undefined;
  /** Disables every source. */
  disabled?: boolean | undefined;
  /** Strings, for localisation. `kinds` merges per kind. */
  messages?:
    | (Partial<Omit<AiKnowledgeScopeMessages, 'kinds'>> & {
        kinds?: Partial<Record<AiKnowledgeSourceKind, string>>;
      })
    | undefined;
}

interface Row {
  node: AiKnowledgeSource;
  parent: string | null;
  level: number;
}

/** Counts the sources that are checked, counting a fully checked group as one. */
function countSelected(
  sources: readonly AiKnowledgeSource[],
  state: Map<string, AiKnowledgeCheckState>,
): number {
  let n = 0;
  const walk = (node: AiKnowledgeSource) => {
    const s = state.get(node.id);
    if (s === 'checked') n += 1;
    else if (s === 'indeterminate') node.children?.forEach(walk);
  };
  sources.forEach(walk);
  return n;
}

/**
 * Lets a person pick what grounds the AI's answers: a tree of spaces,
 * documents and apps with tri-state checkboxes. Checking a group selects
 * everything in it; a partly selected group shows a dash. Built as an ARIA
 * tree with `aria-checked`, arrow-key navigation and Space to toggle.
 */
export const AiKnowledgeScope = forwardRef<HTMLDivElement, AiKnowledgeScopeProps>(
  function AiKnowledgeScope(
    {
      sources,
      value,
      onChange,
      defaultExpanded,
      expanded: expandedProp,
      onExpandedChange,
      showSummary = true,
      disabled = false,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...DEFAULT_MESSAGES,
      ...messagesProp,
      kinds: { ...DEFAULT_MESSAGES.kinds, ...messagesProp?.kinds },
    };
    const baseId = useId().replace(/:/g, '');
    const [expandedInner, setExpandedInner] = useState<string[]>(() => [
      ...(defaultExpanded ?? []),
    ]);
    const expanded = useMemo(
      () => new Set(expandedProp ?? expandedInner),
      [expandedProp, expandedInner],
    );
    const setExpanded = (id: string, open: boolean) => {
      const next = new Set(expanded);
      if (open) next.add(id);
      else next.delete(id);
      const list = [...next];
      if (expandedProp === undefined) setExpandedInner(list);
      onExpandedChange?.(list);
    };

    const state = useMemo(() => aiKnowledgeScopeState(sources, value), [sources, value]);

    const visible: Row[] = [];
    const walk = (nodes: readonly AiKnowledgeSource[], parent: string | null, level: number) => {
      for (const node of nodes) {
        visible.push({ node, parent, level });
        const children = kids(node);
        if (children && expanded.has(node.id)) walk(children, node.id, level + 1);
      }
    };
    walk(sources, null, 1);

    const [activeId, setActiveId] = useState<string | null>(null);
    const current = visible.find((r) => r.node.id === activeId) ?? visible[0];
    const items = useRef(new Map<string, HTMLDivElement>());

    const focusRow = (id: string | undefined) => {
      if (!id) return;
      setActiveId(id);
      items.current.get(id)?.focus();
    };

    const toggle = (id: string) => {
      if (disabled) return;
      onChange(toggleAiKnowledgeSource(sources, value, id));
    };

    const handleKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
      if (!current) return;
      const index = visible.indexOf(current);
      const node = current.node;
      const children = kids(node);
      const isGroup = !!children;
      const open = isGroup && expanded.has(node.id);
      switch (e.key) {
        case 'ArrowDown':
          focusRow(visible[index + 1]?.node.id);
          break;
        case 'ArrowUp':
          focusRow(visible[index - 1]?.node.id);
          break;
        case 'Home':
          focusRow(visible[0]?.node.id);
          break;
        case 'End':
          focusRow(visible[visible.length - 1]?.node.id);
          break;
        case 'ArrowRight':
          if (isGroup && !open) setExpanded(node.id, true);
          else if (open) focusRow(children?.[0]?.id);
          break;
        case 'ArrowLeft':
          if (open) setExpanded(node.id, false);
          else focusRow(current.parent ?? undefined);
          break;
        case ' ':
        case 'Enter':
          toggle(node.id);
          break;
        default:
          return;
      }
      e.preventDefault();
    };

    const renderNodes = (nodes: readonly AiKnowledgeSource[], level: number): ReactNode[] =>
      nodes.map((node, i) => {
        const children = kids(node);
        const isGroup = !!children;
        const open = isGroup && expanded.has(node.id);
        const check = state.get(node.id) ?? 'unchecked';
        const descId = node.description ? `${baseId}-${node.id}-desc` : undefined;
        return (
          <div
            key={node.id}
            ref={(el) => {
              if (el) items.current.set(node.id, el);
              else items.current.delete(node.id);
            }}
            // aria-checked carries the state; aria-selected is not used in a checkbox tree.
            // eslint-disable-next-line jsx-a11y/role-has-required-aria-props
            role="treeitem"
            aria-level={level}
            aria-setsize={nodes.length}
            aria-posinset={i + 1}
            aria-checked={check === 'indeterminate' ? 'mixed' : check === 'checked'}
            aria-expanded={isGroup ? open : undefined}
            aria-label={m.itemLabel(node.label, m.kinds[node.kind])}
            aria-describedby={descId}
            aria-disabled={disabled || undefined}
            tabIndex={current?.node.id === node.id ? 0 : -1}
            className="mn-ai-knowledge-scope__item"
            data-state={check}
            data-kind={node.kind}
            onFocus={(e) => {
              if (e.target === e.currentTarget) setActiveId(node.id);
            }}
          >
            {/* Keyboard toggling is handled by the tree (Space / Enter on the focused item). */}
            {/* eslint-disable-next-line jsx-a11y/click-events-have-key-events, jsx-a11y/no-static-element-interactions */}
            <div
              className="mn-ai-knowledge-scope__row"
              style={{ paddingInlineStart: `calc(${level - 1} * var(--mn-space-5))` }}
              onClick={() => {
                setActiveId(node.id);
                toggle(node.id);
              }}
            >
              <span
                className="mn-ai-knowledge-scope__toggle"
                aria-hidden
                data-open={open || undefined}
                data-hidden={!isGroup || undefined}
                onClick={(e) => {
                  if (!isGroup) return;
                  e.stopPropagation();
                  setActiveId(node.id);
                  setExpanded(node.id, !open);
                }}
              >
                {isGroup ? <ChevronRightGlyph /> : null}
              </span>
              <span className="mn-ai-knowledge-scope__check" aria-hidden data-state={check}>
                {check === 'checked' ? (
                  <CheckGlyph />
                ) : check === 'indeterminate' ? (
                  <DashGlyph />
                ) : null}
              </span>
              <span className="mn-ai-knowledge-scope__text">
                <span className="mn-ai-knowledge-scope__label">{node.label}</span>
                {node.description ? (
                  <span id={descId} className="mn-ai-knowledge-scope__description">
                    {node.description}
                  </span>
                ) : null}
              </span>
              <span className="mn-ai-knowledge-scope__kind" aria-hidden>
                {m.kinds[node.kind]}
              </span>
            </div>
            {open && children ? (
              <div role="group" className="mn-ai-knowledge-scope__group">
                {renderNodes(children, level + 1)}
              </div>
            ) : null}
          </div>
        );
      });

    const count = countSelected(sources, state);

    return (
      <div
        ref={ref}
        {...rest}
        className={cx('mn-ai-knowledge-scope', className)}
        data-disabled={disabled || undefined}
      >
        {sources.length === 0 ? (
          <p className="mn-ai-knowledge-scope__empty">{m.empty}</p>
        ) : (
          // Focus lives on the items (roving tabindex), not on the tree itself.
          // eslint-disable-next-line jsx-a11y/interactive-supports-focus
          <div
            role="tree"
            aria-label={m.label}
            aria-multiselectable="true"
            aria-disabled={disabled || undefined}
            className="mn-ai-knowledge-scope__tree"
            onKeyDown={handleKeyDown}
          >
            {renderNodes(sources, 1)}
          </div>
        )}
        {showSummary && sources.length > 0 ? (
          <p className="mn-ai-knowledge-scope__summary" aria-live="polite">
            {m.summary(count)}
          </p>
        ) : null}
      </div>
    );
  },
);
