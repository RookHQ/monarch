'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type FieldsetHTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';

/** One permission a user can grant. */
export interface ScopeOption {
  /** The scope string sent to the server, such as `projects:read`. */
  value: string;
  /** Visible label. Defaults to `value` in a monospace font. */
  label?: ReactNode | undefined;
  /** What the scope allows, shown under the label. */
  description?: ReactNode | undefined;
  /** The scope cannot be changed. "Select all" skips it. */
  disabled?: boolean | undefined;
}

/** The scopes of one resource, such as one API. */
export interface ScopeGroup {
  /** Stable id, used for the expanded state and auto-selection. */
  id: string;
  /** Group name, such as the API name. */
  label: ReactNode;
  /** Secondary text under the name, such as the API's owner. */
  description?: ReactNode | undefined;
  /** Leading icon or logo. Decorative: the label names the group. */
  icon?: ReactNode | undefined;
  /** The scopes the user may grant in this group (already filtered by the product). */
  scopes: readonly ScopeOption[];
  /** The scopes are still loading. */
  loading?: boolean | undefined;
  /** Shown instead of the count when the group has no scopes. */
  emptyMessage?: ReactNode | undefined;
}

export interface ScopeSelectorMessages {
  /** Legend for the whole selector. */
  label: string;
  /** Shown when there are no groups. */
  empty: string;
  /** Group summary while its scopes load. */
  loading: string;
  /** Group summary when it offers no scopes. */
  noScopes: string;
  /** Group summary: `selected` of `total` scopes. */
  count: (selected: number, total: number) => string;
  selectAll: string;
  clearAll: string;
  /** The paging button: show `next` more of `remaining` hidden groups. */
  showMore: (next: number, remaining: number) => string;
}

const DEFAULT_MESSAGES: ScopeSelectorMessages = {
  label: 'Scopes',
  empty: 'There are no scopes to choose from.',
  loading: 'Loading scopes…',
  noScopes: 'No scopes available.',
  count: (selected, total) => `${selected} of ${total} scopes`,
  selectAll: 'Select all',
  clearAll: 'Clear all',
  showMore: (next, remaining) => `Show ${next} more of ${remaining}`,
};

export interface ScopeSelectorProps extends Omit<
  FieldsetHTMLAttributes<HTMLFieldSetElement>,
  'defaultValue' | 'onChange'
> {
  /** The groups to choose from, in display order. */
  groups: readonly ScopeGroup[];
  /** Controlled: the selected scope values. */
  value?: readonly string[] | undefined;
  /** Uncontrolled initial selection. */
  defaultValue?: readonly string[] | undefined;
  /** Called with the full new selection. */
  onValueChange?: ((value: string[]) => void) | undefined;
  /** Disables every checkbox and button. Groups can still be expanded to review. */
  disabled?: boolean | undefined;
  /** Visually hide the legend (it stays available to screen readers). */
  hideLabel?: boolean | undefined;
  /** Groups expanded on first render: a list of ids, or `'all'`. Default none. */
  defaultExpanded?: readonly string[] | 'all' | undefined;
  /** How many groups to show before a "Show more" button. Default 10. */
  pageSize?: number | undefined;
  /**
   * Select every scope of a group that has none selected, once per group, as
   * soon as its scopes are loaded, so a grant never silently has no access.
   * `'all'` applies to every group; `'added'` only to groups added after the
   * first render (for editing an existing grant). Default off.
   */
  autoSelect?: 'all' | 'added' | undefined;
  /** Replaces the empty message when there are no groups. */
  emptyState?: ReactNode | undefined;
  /** Visible text. */
  messages?: Partial<ScopeSelectorMessages> | undefined;
}

const enabledValues = (group: ScopeGroup) =>
  group.scopes.filter((s) => !s.disabled).map((s) => s.value);

/**
 * Choose the scopes (permissions) to grant, grouped by resource, such as the
 * APIs an API key or OAuth app may call. Each group collapses, shows how many
 * of its scopes are chosen, and has "Select all" and "Clear all". Long lists
 * page in steps so a grant with many APIs stays manageable.
 */
export const ScopeSelector = forwardRef<HTMLFieldSetElement, ScopeSelectorProps>(
  function ScopeSelector(
    {
      groups,
      value: valueProp,
      defaultValue,
      onValueChange,
      disabled = false,
      hideLabel = false,
      defaultExpanded,
      pageSize = 10,
      autoSelect,
      emptyState,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const baseId = useId();
    const [inner, setInner] = useState<readonly string[]>(defaultValue ?? []);
    const value = valueProp ?? inner;
    const valueRef = useRef(value);
    valueRef.current = value;

    const setValue = useCallback(
      (next: string[]) => {
        valueRef.current = next;
        if (valueProp === undefined) setInner(next);
        onValueChange?.(next);
      },
      [valueProp, onValueChange],
    );

    const [expanded, setExpanded] = useState<ReadonlySet<string>>(
      () => new Set(defaultExpanded === 'all' ? groups.map((g) => g.id) : (defaultExpanded ?? [])),
    );
    const toggleExpanded = (id: string) =>
      setExpanded((prev) => {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return next;
      });

    const [limit, setLimit] = useState(pageSize);
    const visible = groups.slice(0, limit);
    const remaining = groups.length - visible.length;

    // After "Show more", move focus to the first newly shown group so it is not lost
    // when the button disappears.
    const listRef = useRef<HTMLUListElement>(null);
    const focusIndex = useRef<number | null>(null);
    useEffect(() => {
      if (focusIndex.current === null) return;
      const triggers = listRef.current?.querySelectorAll<HTMLButtonElement>(
        '.mn-scope-selector__trigger',
      );
      triggers?.[focusIndex.current]?.focus();
      focusIndex.current = null;
    }, [limit]);

    // Auto-selection: once per group id, after its scopes have loaded.
    const seen = useRef<Set<string> | null>(null);
    useEffect(() => {
      if (!autoSelect) return;
      if (seen.current === null) {
        seen.current = new Set(autoSelect === 'added' ? groups.map((g) => g.id) : []);
      }
      const current = valueRef.current;
      const add: string[] = [];
      for (const group of groups) {
        if (seen.current.has(group.id) || group.loading) continue;
        seen.current.add(group.id);
        const offered = enabledValues(group);
        if (!group.scopes.some((s) => current.includes(s.value))) add.push(...offered);
      }
      if (add.length) setValue([...new Set([...current, ...add])]);
    }, [autoSelect, groups, setValue]);

    const toggleScope = (scope: string, on: boolean) => {
      const current = valueRef.current;
      setValue(on ? [...current, scope] : current.filter((v) => v !== scope));
    };

    return (
      <fieldset
        ref={ref}
        {...rest}
        className={cx('mn-scope-selector', className)}
        data-disabled={disabled || undefined}
        data-empty={groups.length === 0 || undefined}
      >
        <legend
          className={cx('mn-scope-selector__legend', hideLabel ? 'mn-visually-hidden' : undefined)}
        >
          {messages.label}
        </legend>
        {groups.length === 0 ? (
          <div className="mn-scope-selector__empty">{emptyState ?? messages.empty}</div>
        ) : (
          <ul ref={listRef} className="mn-scope-selector__groups">
            {visible.map((group, index) => {
              const id = `${baseId}-${index}`;
              const labelId = `${id}-label`;
              const panelId = `${id}-panel`;
              const selectAllId = `${id}-all`;
              const isOpen = expanded.has(group.id);
              const total = group.scopes.length;
              const count = group.scopes.filter((s) => value.includes(s.value)).length;
              const offered = enabledValues(group);
              const allOn = offered.length > 0 && offered.every((v) => value.includes(v));
              const summary = group.loading
                ? messages.loading
                : total === 0
                  ? (group.emptyMessage ?? messages.noScopes)
                  : messages.count(count, total);
              return (
                <li
                  key={group.id}
                  className="mn-scope-selector__group"
                  data-expanded={isOpen || undefined}
                  data-loading={group.loading || undefined}
                  data-selected={count > 0 || undefined}
                >
                  <div className="mn-scope-selector__header">
                    <button
                      type="button"
                      className="mn-scope-selector__trigger"
                      aria-expanded={isOpen}
                      aria-controls={isOpen ? panelId : undefined}
                      onClick={() => toggleExpanded(group.id)}
                    >
                      <ChevronRightGlyph className="mn-scope-selector__chevron" aria-hidden />
                      {group.icon ? (
                        <span className="mn-scope-selector__icon" aria-hidden>
                          {group.icon}
                        </span>
                      ) : null}
                      <span className="mn-scope-selector__heading">
                        <span id={labelId} className="mn-scope-selector__label">
                          {group.label}
                        </span>
                        {group.description ? (
                          <span className="mn-scope-selector__description">
                            {group.description}
                          </span>
                        ) : null}
                        <span className="mn-scope-selector__count">{summary}</span>
                      </span>
                    </button>
                    {!disabled && !group.loading && offered.length > 0 ? (
                      <Button
                        id={selectAllId}
                        variant="ghost"
                        size="sm"
                        className="mn-scope-selector__all"
                        aria-labelledby={`${selectAllId} ${labelId}`}
                        onClick={() => {
                          const current = valueRef.current;
                          setValue(
                            allOn
                              ? current.filter((v) => !offered.includes(v))
                              : [...new Set([...current, ...offered])],
                          );
                        }}
                      >
                        {allOn ? messages.clearAll : messages.selectAll}
                      </Button>
                    ) : null}
                  </div>
                  {isOpen && !group.loading && total > 0 ? (
                    <div
                      id={panelId}
                      role="group"
                      aria-labelledby={labelId}
                      className="mn-scope-selector__panel"
                    >
                      {group.scopes.map((scope) => (
                        <div key={scope.value} className="mn-scope-selector__scope">
                          <Checkbox
                            checked={value.includes(scope.value)}
                            disabled={disabled || !!scope.disabled}
                            onCheckedChange={(state) => toggleScope(scope.value, state === true)}
                            label={
                              scope.label ?? (
                                <code className="mn-scope-selector__value">{scope.value}</code>
                              )
                            }
                            description={scope.description}
                          />
                        </div>
                      ))}
                    </div>
                  ) : null}
                </li>
              );
            })}
          </ul>
        )}
        {remaining > 0 ? (
          <button
            type="button"
            className="mn-scope-selector__more"
            onClick={() => {
              focusIndex.current = limit;
              setLimit(limit + pageSize);
            }}
          >
            {messages.showMore(Math.min(pageSize, remaining), remaining)}
          </button>
        ) : null}
      </fieldset>
    );
  },
);
