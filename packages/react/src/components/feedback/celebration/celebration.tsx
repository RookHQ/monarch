'use client';

import {
  forwardRef,
  useState,
  type CSSProperties,
  type ElementRef,
  type HTMLAttributes,
  type ReactElement,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { SuccessGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogTitle,
  DialogTrigger,
  type DialogContentProps,
} from '../../overlays/dialog/index.js';

/** Decoration behind the icon. `confetti` falls once; `none` is plain. */
export type CelebrationDecoration = 'confetti' | 'none';

/** Colour of the icon tile. */
export type CelebrationTone = 'success' | 'accent';

// Fixed positions so the server and the client render the same thing.
// [left %, delay ms, rotation deg, drift px, colour 1–5, shape]
const PIECES: readonly [number, number, number, number, number, 'bar' | 'dot'][] = [
  [6, 0, 20, -12, 1, 'bar'],
  [14, 120, -35, 10, 2, 'dot'],
  [22, 60, 60, -8, 3, 'bar'],
  [31, 200, -15, 14, 4, 'bar'],
  [39, 40, 45, -10, 5, 'dot'],
  [47, 160, -60, 6, 1, 'bar'],
  [55, 20, 30, -14, 2, 'bar'],
  [62, 240, -25, 12, 3, 'dot'],
  [70, 80, 70, -6, 4, 'bar'],
  [78, 180, -45, 10, 5, 'bar'],
  [86, 100, 15, -12, 1, 'dot'],
  [93, 260, -70, 8, 2, 'bar'],
];

function Confetti() {
  return (
    <span className="mn-celebration__confetti" aria-hidden>
      {PIECES.map(([left, delay, rotate, drift, color, shape], index) => (
        <span
          key={index}
          className="mn-celebration__piece"
          data-color={color}
          data-shape={shape}
          style={
            {
              '--_left': `${left}%`,
              '--_delay': `${delay}ms`,
              '--_rotate': `${rotate}deg`,
              '--_drift': `${drift}px`,
              '--_rest': `${(delay / 100).toFixed(1)}rem`,
            } as CSSProperties
          }
        />
      ))}
    </span>
  );
}

interface CelebrationVisualProps {
  media?: ReactNode | undefined;
  tone: CelebrationTone;
  decoration: CelebrationDecoration;
}

function CelebrationVisual({ media, tone, decoration }: CelebrationVisualProps) {
  return (
    <div className="mn-celebration__visual">
      {decoration === 'confetti' ? <Confetti /> : null}
      <span className="mn-celebration__media" data-tone={tone} aria-hidden>
        {media ?? <SuccessGlyph />}
      </span>
    </div>
  );
}

export interface CelebrationPanelProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** What was achieved, e.g. "Domain verified". */
  title: ReactNode;
  /** What it means or what to do next. */
  description?: ReactNode;
  /**
   * Image or icon in the centre, e.g. a badge. Decorative: say everything
   * in the title and description. Default a check mark.
   */
  media?: ReactNode;
  /** Colour of the icon tile. Default `success`. Exposed as `data-tone`. */
  tone?: CelebrationTone;
  /** Default `confetti`. It falls once and is static with reduced motion. */
  decoration?: CelebrationDecoration;
  /** Buttons or links under the text. */
  actions?: ReactNode;
  /** Title element. Default `h2`. */
  headingLevel?: 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6' | 'p';
  /**
   * Announce the title and description politely when the panel appears
   * (`role="status"`). Default `true`; turn it off when the panel is part of
   * the page from the start.
   */
  announce?: boolean;
}

/**
 * An inline success moment, for the end of a flow or onboarding step: a
 * badge or check mark, a title, a short description and the next action.
 * The decoration is CSS only, hidden from assistive technology, never
 * flashes, and stays still for people who prefer reduced motion.
 */
export const CelebrationPanel = forwardRef<HTMLDivElement, CelebrationPanelProps>(
  function CelebrationPanel(
    {
      title,
      description,
      media,
      tone = 'success',
      decoration = 'confetti',
      actions,
      headingLevel: Heading = 'h2',
      announce = true,
      className,
      children,
      ...rest
    },
    ref,
  ) {
    return (
      <div
        ref={ref}
        role={announce ? 'status' : undefined}
        {...rest}
        className={cx('mn-celebration', className)}
        data-tone={tone}
        data-decoration={decoration}
      >
        <CelebrationVisual media={media} tone={tone} decoration={decoration} />
        <Heading className="mn-celebration__title">{title}</Heading>
        {description ? <p className="mn-celebration__description">{description}</p> : null}
        {children}
        {actions ? <div className="mn-celebration__actions">{actions}</div> : null}
      </div>
    );
  },
);

export interface CelebrationProps extends Omit<
  DialogContentProps,
  'title' | 'footer' | 'size' | 'busy'
> {
  /** Controlled open state. */
  open?: boolean;
  /** Uncontrolled initial open state. */
  defaultOpen?: boolean;
  onOpenChange?: ((open: boolean) => void) | undefined;
  /** An element that opens it (rendered with `asChild`). Usually the product opens it itself. */
  trigger?: ReactElement;
  /** What was achieved, e.g. "Workspace verified". Becomes the dialog's accessible name. */
  title: ReactNode;
  /** What it means or what to do next. Becomes the accessible description. */
  description?: ReactNode;
  /** Image or icon in the centre, e.g. a badge. Decorative. Default a check mark. */
  media?: ReactNode;
  /** Colour of the icon tile. Default `success`. */
  tone?: CelebrationTone;
  /** Default `confetti`. It falls once and is static with reduced motion. */
  decoration?: CelebrationDecoration;
  /** Label of the button that closes it. Default "Continue". `null` hides it. */
  continueLabel?: ReactNode;
  /** Called when the continue button is pressed, before the dialog closes. */
  onContinue?: (() => void) | undefined;
  /** More actions next to the continue button, e.g. a link to the new page. */
  actions?: ReactNode;
  /** Extra content under the description. */
  children?: ReactNode;
}

/**
 * A short celebratory dialog for a real milestone (an account or domain
 * verified, a first project published). Focus starts on the continue
 * button; Escape and outside clicks close it. The confetti is decorative,
 * falls once, never flashes and is still under reduced motion.
 */
export const Celebration = forwardRef<ElementRef<typeof DialogContent>, CelebrationProps>(
  function Celebration(
    {
      open: openProp,
      defaultOpen = false,
      onOpenChange,
      trigger,
      title,
      description,
      media,
      tone = 'success',
      decoration = 'confetti',
      continueLabel = 'Continue',
      onContinue,
      actions,
      children,
      closeLabel = null,
      className,
      ...rest
    },
    ref,
  ) {
    const [openState, setOpenState] = useState(defaultOpen);
    const open = openProp ?? openState;
    const setOpen = (next: boolean) => {
      if (openProp === undefined) setOpenState(next);
      onOpenChange?.(next);
    };
    return (
      <Dialog open={open} onOpenChange={setOpen}>
        {trigger ? <DialogTrigger asChild>{trigger}</DialogTrigger> : null}
        <DialogContent
          ref={ref}
          size="sm"
          closeLabel={closeLabel}
          {...rest}
          className={cx('mn-celebration', 'mn-celebration--dialog', className)}
          data-tone={tone}
          data-decoration={decoration}
          {...(description ? {} : { 'aria-describedby': undefined })}
        >
          <CelebrationVisual media={media} tone={tone} decoration={decoration} />
          <DialogTitle className="mn-celebration__title">{title}</DialogTitle>
          {description ? (
            <DialogDescription className="mn-celebration__description">
              {description}
            </DialogDescription>
          ) : null}
          {children}
          {continueLabel !== null || actions ? (
            <div className="mn-celebration__actions">
              {actions}
              {continueLabel !== null ? (
                <DialogClose asChild>
                  <Button variant="primary" onClick={() => onContinue?.()}>
                    {continueLabel}
                  </Button>
                </DialogClose>
              ) : null}
            </div>
          ) : null}
        </DialogContent>
      </Dialog>
    );
  },
);
