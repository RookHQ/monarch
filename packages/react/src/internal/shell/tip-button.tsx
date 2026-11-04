'use client';

import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { Tooltip, TooltipProvider } from '../../components/overlays/tooltip/index.js';

export interface TipButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** Tooltip text, shown on hover and focus. Omit for no tooltip. */
  tip?: ReactNode;
  tipSide?: 'top' | 'right' | 'bottom' | 'left';
}

/**
 * A button that can carry a tooltip and still be used as another primitive's
 * `asChild` trigger (a popover or menu): every prop and the ref reach the
 * `<button>`, and the tooltip wraps it with its own provider.
 */
export const TipButton = forwardRef<HTMLButtonElement, TipButtonProps>(function TipButton(
  { tip, tipSide = 'right', type = 'button', ...props },
  ref,
) {
  const button = <button ref={ref} type={type} {...props} />;
  if (!tip) return button;
  return (
    <TooltipProvider delayDuration={100}>
      <Tooltip content={tip} side={tipSide}>
        {button}
      </Tooltip>
    </TooltipProvider>
  );
});
