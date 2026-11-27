'use client';

import { Fragment, type ReactElement, type ReactNode } from 'react';
import { useControllable } from '../../../internal/use-controllable.js';
import { useDirectionContext } from '../../../internal/direction.js';
import { useOverlayStage } from '../../../internal/overlay-stage.js';
import * as TooltipPrimitive from '@radix-ui/react-tooltip';

/** Mount once near the root. Shares the open delay across tooltips. */
export function TooltipProvider({
  delayDuration = 400,
  children,
}: {
  delayDuration?: number;
  children: ReactNode;
}) {
  return (
    <TooltipPrimitive.Provider delayDuration={delayDuration}>{children}</TooltipPrimitive.Provider>
  );
}

export interface TooltipProps {
  /** Supplementary text. Never put essential information or interactive content here. */
  content: ReactNode;
  /** A single focusable element. */
  children: ReactElement;
  side?: 'top' | 'right' | 'bottom' | 'left';
  align?: 'start' | 'center' | 'end';
  open?: boolean;
  defaultOpen?: boolean;
  onOpenChange?: (open: boolean) => void;
  /**
   * Suppresses the tooltip without switching it between controlled and
   * uncontrolled (for example while a navigation rail is expanded and the
   * label is already visible).
   */
  disabled?: boolean;
}

/** Shows on hover and keyboard focus; Escape dismisses it. */
export function Tooltip({
  content,
  children,
  side = 'top',
  align = 'center',
  open: openProp,
  defaultOpen = false,
  onOpenChange,
  disabled = false,
}: TooltipProps) {
  const [open, setOpen] = useControllable(openProp, defaultOpen, onOpenChange);
  // The bubble is portalled to <body>, outside the element that carries `dir`.
  const dir = useDirectionContext();
  const staged = useOverlayStage();
  const Portal = staged ? Fragment : TooltipPrimitive.Portal;
  // Radix always receives `open`, so toggling `disabled` never flips it between
  // controlled and uncontrolled.
  return (
    <TooltipPrimitive.Root
      open={!disabled && open}
      onOpenChange={(next) => {
        // A staged tooltip (a docs picture) stays as it was rendered: hover,
        // focus and page scroll would otherwise close it.
        if (staged) return;
        if (!(disabled && next)) setOpen(next);
      }}
    >
      <TooltipPrimitive.Trigger asChild>{children}</TooltipPrimitive.Trigger>
      <Portal>
        <TooltipPrimitive.Content
          side={side}
          align={align}
          sideOffset={6}
          className="mn-tooltip"
          {...(dir ? { dir } : {})}
        >
          {content}
        </TooltipPrimitive.Content>
      </Portal>
    </TooltipPrimitive.Root>
  );
}
