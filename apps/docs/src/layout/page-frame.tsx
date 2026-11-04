import { useEffect, useState, type ReactNode } from 'react';
import { useLocation, href } from '../lib/router.js';
import { displayLabel, neighbours } from '../app/navigation.js';
import { ArrowLeftIcon, ArrowRightIcon } from '../ui/icons.js';
import { Toc } from './toc.js';

/**
 * The frame of every documentation page: eyebrow, title, lead, the content,
 * previous/next links, and an "On this page" list on wide screens. Headings
 * marked `data-toc` (with an id) appear in the list.
 */
export function PageFrame({
  eyebrow,
  title,
  lead,
  meta,
  children,
  toc = true,
  wide = false,
}: {
  eyebrow?: ReactNode;
  title: ReactNode;
  lead?: ReactNode;
  /** Badges or links under the lead. */
  meta?: ReactNode;
  children: ReactNode;
  toc?: boolean;
  /** Use the full content width (overview grids) instead of the reading column. */
  wide?: boolean;
}) {
  const { path, anchor } = useLocation();
  const [article, setArticle] = useState<HTMLElement | null>(null);

  // Deep links: `#/path#section` scrolls to the section once it exists.
  useEffect(() => {
    if (!anchor) return;
    let frame = 0;
    let tries = 0;
    const go = () => {
      const el = document.getElementById(anchor);
      if (el) el.scrollIntoView({ block: 'start' });
      else if (tries++ < 30) frame = requestAnimationFrame(go);
    };
    go();
    return () => cancelAnimationFrame(frame);
  }, [anchor, path]);

  const { prev, next } = neighbours(path);
  return (
    <div className="docs-page" data-toc={toc || undefined} data-wide={wide || undefined}>
      <article ref={setArticle} className="docs-page__article">
        <header className="docs-page__header">
          {eyebrow ? <p className="docs-page__eyebrow">{eyebrow}</p> : null}
          <h1 className="docs-page__title">{title}</h1>
          {lead ? <p className="docs-page__lead">{lead}</p> : null}
          {meta ? <div className="docs-page__meta">{meta}</div> : null}
        </header>
        <div className="docs-page__body">{children}</div>
        {prev || next ? (
          <nav className="docs-pager" aria-label="Previous and next page">
            {prev ? (
              <a className="docs-pager__link" data-dir="prev" href={href(prev.path)}>
                <span className="docs-pager__hint">
                  <ArrowLeftIcon /> Previous
                </span>
                <span className="docs-pager__label">{displayLabel(prev)}</span>
              </a>
            ) : (
              <span />
            )}
            {next ? (
              <a className="docs-pager__link" data-dir="next" href={href(next.path)}>
                <span className="docs-pager__hint">
                  Next <ArrowRightIcon />
                </span>
                <span className="docs-pager__label">{displayLabel(next)}</span>
              </a>
            ) : null}
          </nav>
        ) : null}
      </article>
      {toc ? (
        <div className="docs-page__aside">
          <Toc path={path} root={article} />
        </div>
      ) : null}
    </div>
  );
}

/** A titled section of a page that appears in "On this page". */
export function PageSection({
  id,
  title,
  children,
  className,
}: {
  id: string;
  title: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={className ? `docs-section ${className}` : 'docs-section'}
      aria-labelledby={id}
    >
      <h2 id={id} className="docs-section__title" data-toc="">
        {title}
      </h2>
      {children}
    </section>
  );
}
