import { forwardRef, useId, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

/** Root class, declared before any other `mn-` string so dist/manifest.json reads it as the root. */
const ROOT = 'mn-drag-handle';

/** Where the item is in a drag: at rest, picked up, or just dropped (a brief confirmation). */
export type DragHandleState = 'idle' | 'grabbed' | 'dropped';

/** Text read to screen-reader users. Override it to translate. */
export interface DragHandleMessages {
  /** Read once as the handle's description. */
  instructions: string;
}

const DEFAULT_MESSAGES: DragHandleMessages = {
  instructions:
    'Press Space or Enter to pick up. Use the arrow keys to move, Space or Enter to drop, Escape to cancel.',
};

export interface DragHandleProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  /** Accessible name, naming the item: "Reorder Design review". Required: the handle is icon-only. */
  label: string;
  /**
   * The drag state, which the component that moves items owns. `grabbed`
   * sets `aria-pressed="true"` and the picked-up style; `dropped` shows a
   * short confirmation tint. Default `idle`.
   */
  state?: DragHandleState | undefined;
  /** Which way the item moves, for the glyph: `vertical` (lists), `horizontal`, or `both` (grids). Default `vertical`. */
  axis?: 'vertical' | 'horizontal' | 'both' | undefined;
  /** `sm` (24px) or `md` (32px); both grow to 44px on touch screens. Default `md`. */
  size?: 'sm' | 'md' | undefined;
  /**
   * Describe the handle with the keyboard instructions. Default true. Set it
   * false when the container already describes every handle with one shared
   * `aria-describedby`.
   */
  describe?: boolean | undefined;
  /** A custom glyph instead of the grip dots. Decorative: it is hidden from assistive technology. */
  icon?: ReactNode;
  /** Override the instructions text. */
  messages?: Partial<DragHandleMessages> | undefined;
}

function GripGlyph({ axis }: { axis: 'vertical' | 'horizontal' | 'both' }) {
  const dots =
    axis === 'horizontal'
      ? [4, 8, 12].flatMap((x) => [6, 10].map((y) => [x, y] as const))
      : axis === 'both'
        ? [4, 8, 12].flatMap((x) => [4, 8, 12].map((y) => [x, y] as const))
        : [6, 10].flatMap((x) => [4, 8, 12].map((y) => [x, y] as const));
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 16 16"
      fill="currentColor"
      aria-hidden
      focusable={false}
    >
      {dots.map(([x, y]) => (
        <circle key={`${x}-${y}`} cx={x} cy={y} r="1.25" />
      ))}
    </svg>
  );
}

/**
 * The grip button an item is dragged by. It owns the look, the touch target,
 * the accessible name and the keyboard instructions; the component that moves
 * items owns the pointer and key handling and passes `state`. Spread your
 * pointer and key handlers onto it (they reach the `<button>`).
 */
export const DragHandle = forwardRef<HTMLButtonElement, DragHandleProps>(function DragHandle(
  {
    label,
    state = 'idle',
    axis = 'vertical',
    size = 'md',
    describe = true,
    icon,
    messages: messagesProp,
    className,
    type = 'button',
    'aria-describedby': describedBy,
    ...rest
  },
  ref,
) {
  const messages = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const instructionsId = `mn-drag-handle-${id}`;
  const describedByIds =
    [describedBy, describe ? instructionsId : undefined].filter(Boolean).join(' ') || undefined;
  return (
    <>
      <button
        ref={ref}
        type={type}
        {...rest}
        className={cx(ROOT, className)}
        aria-label={label}
        aria-pressed={state === 'grabbed'}
        aria-describedby={describedByIds}
        data-state={state}
        data-axis={axis}
        data-size={size}
      >
        {icon ?? <GripGlyph axis={axis} />}
      </button>
      {describe ? (
        <span id={instructionsId} className="mn-visually-hidden" hidden>
          {messages.instructions}
        </span>
      ) : null}
    </>
  );
});
