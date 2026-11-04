import { forwardRef, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';

export interface AppShellProps extends HTMLAttributes<HTMLDivElement> {
  /** The primary navigation, usually `<RookRail />`. */
  rail?: ReactNode;
  /** The top bar, usually `<IdentityBar />`. */
  header?: ReactNode;
  /** The page. Rendered inside `<main>`. */
  children?: ReactNode;
  /**
   * `main` (default): the shell fills the viewport and only the main region
   * scrolls, so the rail and header stay put. `page`: the document scrolls and
   * the rail and header are sticky (for long, printable pages).
   */
  scroll?: 'main' | 'page' | undefined;
  /** Id of `<main>`, the skip link's target. Default `main`. */
  mainId?: string | undefined;
  /** Text of the skip link. Default "Skip to content". */
  skipLinkLabel?: string | undefined;
  /** Extra props for `<main>` (an `aria-label`, a class). */
  mainProps?: Omit<HTMLAttributes<HTMLElement>, 'id' | 'children'> | undefined;
  /**
   * `main` (default), or `region` for a shell embedded in another page that
   * already has its `<main>` (docs, previews, onboarding tours): the content
   * becomes a `<div role="region">`, named by `mainProps['aria-label']`.
   */
  mainRole?: 'main' | 'region' | undefined;
}

/**
 * The layout of a signed-in Rook app: the rail on the left (a top bar and
 * drawer on phones), an optional identity bar on top, and the main region.
 * It adds the skip link (the first Tab stop; `<main>` takes focus because it
 * has `tabindex="-1"`), keeps the landmarks in order (navigation, banner,
 * main) and contains scrolling to the main region. Server-safe: no state.
 */
export const AppShell = forwardRef<HTMLDivElement, AppShellProps>(function AppShell(
  {
    rail,
    header,
    children,
    scroll = 'main',
    mainId = 'main',
    skipLinkLabel = 'Skip to content',
    mainProps,
    mainRole = 'main',
    className,
    ...rest
  },
  ref,
) {
  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-app-shell', className)}
      data-scroll={scroll}
      data-rail={rail ? '' : undefined}
      data-header={header ? '' : undefined}
    >
      <a className="mn-app-shell__skip" href={`#${mainId}`}>
        {skipLinkLabel}
      </a>
      {rail ? <div className="mn-app-shell__rail">{rail}</div> : null}
      {header ? <div className="mn-app-shell__header">{header}</div> : null}
      {mainRole === 'region' ? (
        <div
          role="region"
          {...mainProps}
          id={mainId}
          tabIndex={-1}
          className={cx('mn-app-shell__main', mainProps?.className)}
        >
          {children}
        </div>
      ) : (
        <main
          {...mainProps}
          id={mainId}
          tabIndex={-1}
          className={cx('mn-app-shell__main', mainProps?.className)}
        >
          {children}
        </main>
      )}
    </div>
  );
});
