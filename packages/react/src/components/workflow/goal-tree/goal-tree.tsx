'use client';

import { forwardRef, useId, useMemo, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph, PlusGlyph } from '../../../internal/icons.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import { Progress, type ProgressTone } from '../../data-display/progress-bar/index.js';
import { Avatar } from '../../identity/avatar/index.js';

/** How a goal is doing. */
export type GoalStatus = 'not-started' | 'on-track' | 'at-risk' | 'off-track' | 'done';

/** One goal: an objective, key result or any nested target. */
export interface GoalTreeItem {
  /** Stable id. */
  id: string;
  /** Parent goal id; `null` or a missing parent makes it a top-level goal. */
  parentId?: string | null | undefined;
  /** What the goal is. */
  title: ReactNode;
  /** Plain-text title for accessible names when `title` is not a string. */
  titleText?: string | undefined;
  /** Progress from 0 to 100. */
  progress: number;
  status: GoalStatus;
  /** Owner shown with an avatar. */
  owner?: { name: string; avatarSrc?: string | null | undefined } | undefined;
  /** Short facts after the title, such as "weight 30%" or "due 31 Mar". */
  meta?: ReactNode;
  /** A goal it supports elsewhere, such as "Grow repeat customers (Sam Lee)". Shown as "Aligned to …". */
  alignedTo?: ReactNode;
}

/** Visible and announced strings. Override them to translate. */
export interface GoalTreeMessages {
  status: Record<GoalStatus, string>;
  /** Accessible name of the expand button. */
  expand: (title: string) => string;
  collapse: (title: string) => string;
  /** Accessible name of each progress bar. */
  progress: (title: string) => string;
  /** "Aligned to …" prefix. */
  alignedTo: string;
  /** Label of the add-child button (with `onAddChild`). */
  addChild: string;
  /** Accessible name of the add-child button. */
  addChildTo: (title: string) => string;
  /** "3 key results" on a collapsed goal. */
  childCount: (count: number) => string;
  empty: string;
}

export const DEFAULT_GOAL_TREE_MESSAGES: GoalTreeMessages = {
  status: {
    'not-started': 'Not started',
    'on-track': 'On track',
    'at-risk': 'At risk',
    'off-track': 'Off track',
    done: 'Done',
  },
  expand: (t) => `Expand ${t}`,
  collapse: (t) => `Collapse ${t}`,
  progress: (t) => `Progress of ${t}`,
  alignedTo: 'Aligned to',
  addChild: 'Add key result',
  addChildTo: (t) => `Add key result to ${t}`,
  childCount: (n) => `${n} ${n === 1 ? 'key result' : 'key results'}`,
  empty: 'No goals yet.',
};

const BADGE_TONE: Record<GoalStatus, Tone> = {
  'not-started': 'neutral',
  'on-track': 'info',
  'at-risk': 'warning',
  'off-track': 'danger',
  done: 'success',
};
const BAR_TONE: Record<GoalStatus, ProgressTone> = {
  'not-started': 'neutral',
  'on-track': 'accent',
  'at-risk': 'warning',
  'off-track': 'danger',
  done: 'success',
};

export interface GoalTreeProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** Every goal, flat; nesting comes from `parentId`. Order is kept among siblings. */
  goals: GoalTreeItem[];
  /** Ids of expanded goals (controlled). */
  expanded?: string[] | undefined;
  /** Ids expanded at first (uncontrolled). Default: every goal with children. */
  defaultExpanded?: string[] | undefined;
  /** Called with the new expanded ids when a goal is expanded or collapsed. */
  onExpandedChange?: ((expanded: string[]) => void) | undefined;
  /** Buttons for a goal, such as Check in, History or a menu. Rendered at the end of its row. */
  renderActions?: ((goal: GoalTreeItem) => ReactNode) | undefined;
  /** Adds an "Add key result" button under goals that `canAddChild` allows. */
  onAddChild?: ((goal: GoalTreeItem) => void) | undefined;
  /** Which goals get the add-child button. Default: top-level goals. */
  canAddChild?: ((goal: GoalTreeItem, depth: number) => boolean) | undefined;
  /** Hide the status badge on children that are on track or not started, as a quieter view. Default `true`. */
  quietChildren?: boolean;
  /** Shown when there are no goals. */
  emptyMessage?: ReactNode;
  /** Override visible and announced strings. */
  messages?: Partial<GoalTreeMessages>;
}

function textOf(goal: GoalTreeItem) {
  return goal.titleText ?? (typeof goal.title === 'string' ? goal.title : goal.id);
}

/**
 * Nested goals (objectives and key results) with progress, status, owner and
 * alignment. Branches expand and collapse; each row can carry its own actions.
 */
export const GoalTree = forwardRef<HTMLDivElement, GoalTreeProps>(function GoalTree(
  {
    goals,
    expanded: expandedProp,
    defaultExpanded,
    onExpandedChange,
    renderActions,
    onAddChild,
    canAddChild = (_goal, depth) => depth === 0,
    quietChildren = true,
    emptyMessage,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_GOAL_TREE_MESSAGES, ...messagesProp };
  const statusText = { ...DEFAULT_GOAL_TREE_MESSAGES.status, ...messagesProp?.status };
  const baseId = useId();

  const { roots, children } = useMemo(() => {
    const ids = new Set(goals.map((g) => g.id));
    const kids = new Map<string, GoalTreeItem[]>();
    const top: GoalTreeItem[] = [];
    for (const g of goals) {
      const parent = g.parentId && ids.has(g.parentId) && g.parentId !== g.id ? g.parentId : null;
      if (parent === null) top.push(g);
      else kids.set(parent, [...(kids.get(parent) ?? []), g]);
    }
    return { roots: top, children: kids };
  }, [goals]);

  const [expandedState, setExpandedState] = useState<string[]>(
    () => defaultExpanded ?? [...children.keys()],
  );
  const expanded = new Set(expandedProp ?? expandedState);

  function toggle(id: string) {
    const next = new Set(expanded);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    const list = [...next];
    if (expandedProp === undefined) setExpandedState(list);
    onExpandedChange?.(list);
  }

  if (goals.length === 0) {
    return (
      <div ref={ref} {...rest} className={cx('mn-goal-tree', className)} data-empty="">
        <p className="mn-goal-tree__empty">{emptyMessage ?? messages.empty}</p>
      </div>
    );
  }

  const renderGoal = (goal: GoalTreeItem, depth: number, trail: Set<string>): ReactNode => {
    const kids = trail.has(goal.id) ? [] : (children.get(goal.id) ?? []);
    const open = expanded.has(goal.id);
    const title = textOf(goal);
    const groupId = `${baseId}-${goal.id}`;
    const adds = onAddChild !== undefined && canAddChild(goal, depth);
    const showBadge =
      depth === 0 ||
      !quietChildren ||
      (goal.status !== 'on-track' && goal.status !== 'not-started');
    const nextTrail = new Set(trail).add(goal.id);
    const pct = Math.max(0, Math.min(100, Math.round(goal.progress)));
    return (
      <li key={goal.id} className="mn-goal-tree__item" data-depth={depth} data-status={goal.status}>
        <div className="mn-goal-tree__row">
          {kids.length > 0 || adds ? (
            <button
              type="button"
              className="mn-goal-tree__toggle"
              aria-expanded={open}
              aria-controls={open ? groupId : undefined}
              aria-label={open ? messages.collapse(title) : messages.expand(title)}
              onClick={() => toggle(goal.id)}
            >
              <ChevronRightGlyph aria-hidden />
            </button>
          ) : (
            <span className="mn-goal-tree__spacer" aria-hidden />
          )}
          <div className="mn-goal-tree__body">
            <div className="mn-goal-tree__heading">
              {goal.owner ? (
                <Avatar
                  name={goal.owner.name}
                  src={goal.owner.avatarSrc ?? null}
                  size="xs"
                  decorative
                />
              ) : null}
              <p className="mn-goal-tree__title">{goal.title}</p>
              {showBadge ? (
                <Badge size="sm" tone={BADGE_TONE[goal.status]}>
                  {statusText[goal.status]}
                </Badge>
              ) : (
                <span className="mn-visually-hidden">{statusText[goal.status]}</span>
              )}
              {goal.meta ? <span className="mn-goal-tree__meta">{goal.meta}</span> : null}
              {!open && kids.length > 0 ? (
                <span className="mn-goal-tree__meta">{messages.childCount(kids.length)}</span>
              ) : null}
            </div>
            {goal.owner ? <span className="mn-visually-hidden">{goal.owner.name}</span> : null}
            {goal.alignedTo ? (
              <p className="mn-goal-tree__aligned">
                <LinkGlyph />
                {messages.alignedTo} {goal.alignedTo}
              </p>
            ) : null}
            <div className="mn-goal-tree__progress">
              <Progress
                size="sm"
                value={pct}
                tone={BAR_TONE[goal.status]}
                label={messages.progress(title)}
                valueText={`${pct}%`}
              />
              <span className="mn-goal-tree__percent" aria-hidden>
                {pct}%
              </span>
            </div>
          </div>
          {renderActions ? (
            <div className="mn-goal-tree__actions">{renderActions(goal)}</div>
          ) : null}
        </div>
        {open && (kids.length > 0 || adds) ? (
          <ul id={groupId} className="mn-goal-tree__children">
            {kids.map((k) => renderGoal(k, depth + 1, nextTrail))}
            {adds ? (
              <li className="mn-goal-tree__add">
                <button
                  type="button"
                  className="mn-goal-tree__add-button"
                  aria-label={messages.addChildTo(title)}
                  onClick={() => onAddChild(goal)}
                >
                  <PlusGlyph aria-hidden />
                  {messages.addChild}
                </button>
              </li>
            ) : null}
          </ul>
        ) : null}
      </li>
    );
  };

  return (
    <div ref={ref} {...rest} className={cx('mn-goal-tree', className)}>
      <ul className="mn-goal-tree__list">{roots.map((g) => renderGoal(g, 0, new Set()))}</ul>
    </div>
  );
});

function LinkGlyph() {
  return (
    <svg
      viewBox="0 0 16 16"
      width="12"
      height="12"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      aria-hidden
      focusable={false}
    >
      <path d="M6.5 9.5l3-3M7 4.5l1-1a2.5 2.5 0 0 1 3.5 3.5l-1 1M9 11.5l-1 1A2.5 2.5 0 0 1 4.5 9l1-1" />
    </svg>
  );
}
