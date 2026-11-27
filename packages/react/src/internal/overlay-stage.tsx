'use client';

/**
 * Overlay stage: shows an open overlay (dialog, sheet, menu, popover, tooltip,
 * listbox) in place, as a picture of the component, instead of floating it
 * over the page. Used by the docs site for preview frames (an example with
 * `stage` in its docs file) and by tests; it is not part of the public API.
 *
 * Inside a stage an overlay:
 * - renders where it is written, not in a portal on <body>, so it stays in the
 *   preview frame (the docs site styles `[data-mn-overlay-stage]` so floating
 *   surfaces sit in the flow);
 * - is not modal: no focus trap, no scroll lock, the page stays readable by
 *   assistive technology;
 * - does not take focus when it mounts open, and is not closed by clicks or
 *   focus elsewhere on the page. Escape still closes it while focus is inside.
 *
 * The context lives on `globalThis` under a registered symbol, so the docs
 * site (which reads this file from source) and the built package it renders
 * (which bundles its own copy) share one context object.
 */
import { createContext, useContext, type ComponentType, type Context, type ReactNode } from 'react';

const KEY = Symbol.for('@rookcom/monarch-react/overlay-stage');
const holder = globalThis as { [KEY]?: Context<boolean> };
const StageContext: Context<boolean> = holder[KEY] ?? (holder[KEY] = createContext(false));

/** True inside an overlay stage. */
export function useOverlayStage(): boolean {
  return useContext(StageContext);
}

export interface OverlayStageProps {
  children?: ReactNode;
  className?: string;
  /** Where staged popups sit relative to their trigger. Default `bottom`. */
  side?: 'top' | 'bottom';
  /** Which edge of the trigger staged popups line up with. Default `start`. */
  align?: 'start' | 'center' | 'end';
}

/** A frame whose overlays render open and in place (see the module comment). */
export function OverlayStage({
  children,
  className,
  side = 'bottom',
  align = 'start',
}: OverlayStageProps) {
  return (
    <StageContext.Provider value={true}>
      <div
        className={className ? `mn-overlay-stage ${className}` : 'mn-overlay-stage'}
        data-mn-overlay-stage=""
        data-side={side}
        data-align={align}
      >
        {children}
      </div>
    </StageContext.Provider>
  );
}

interface LayerEvent {
  preventDefault(): void;
  readonly target: EventTarget | null;
}

/**
 * Radix dismissal handlers for a staged overlay: never closed from outside,
 * and Escape only while focus is inside a stage. Outside a stage the handlers
 * are only wrapped, so they run as before. Pair with `preventDefault()` in
 * `onOpenAutoFocus` so a staged overlay does not take focus when it mounts.
 */
export function stagedDismiss<E extends LayerEvent, O extends LayerEvent>(
  staged: boolean,
  handlers: {
    onEscapeKeyDown?: ((event: E) => void) | undefined;
    onInteractOutside?: ((event: O) => void) | undefined;
  },
): { onEscapeKeyDown: (event: E) => void; onInteractOutside: (event: O) => void } {
  return {
    onEscapeKeyDown(event) {
      if (staged) {
        const target = event.target as Element | null;
        if (!target?.closest?.('[data-mn-overlay-stage]')) {
          event.preventDefault();
          return;
        }
      }
      handlers.onEscapeKeyDown?.(event);
    },
    onInteractOutside(event) {
      if (staged) event.preventDefault();
      handlers.onInteractOutside?.(event);
    },
  };
}

/**
 * A Radix `Portal` part, skipped inside a stage so the content renders where
 * it is written: `<StagePortal portal={PopoverPrimitive.Portal}>`.
 */
export function StagePortal({
  portal: Portal,
  children,
}: {
  portal: ComponentType<{ children?: ReactNode }>;
  children?: ReactNode;
}) {
  const staged = useOverlayStage();
  return staged ? <>{children}</> : <Portal>{children}</Portal>;
}

/**
 * Props for a Radix layer's content inside a stage (no focus on mount, no
 * dismissal from outside, Escape only from inside); nothing outside one.
 * Spread last, after the content's own handlers.
 */
export function stagedLayerProps(staged: boolean): {
  onOpenAutoFocus?: (event: Event) => void;
  onInteractOutside?: (event: LayerEvent) => void;
  onEscapeKeyDown?: (event: LayerEvent) => void;
} {
  if (!staged) return {};
  return {
    ...stagedDismiss<LayerEvent, LayerEvent>(true, {}),
    onOpenAutoFocus: (event) => event.preventDefault(),
  };
}
