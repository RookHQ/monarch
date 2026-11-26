'use client';

import {
  useCallback,
  useEffect,
  useId,
  useRef,
  useState,
  type CSSProperties,
  type ReactNode,
} from 'react';

interface DescriptionValueClampProps {
  lines: number;
  expandLabel: string;
  collapseLabel: string;
  children: ReactNode;
}

/**
 * A value clamped to `lines` lines with a disclosure button. The button is
 * rendered only when the content overflows the clamp (measured after mount
 * and on resize), so short values look exactly like unclamped ones.
 */
export function DescriptionValueClamp({
  lines,
  expandLabel,
  collapseLabel,
  children,
}: DescriptionValueClampProps) {
  const id = useId();
  const ref = useRef<HTMLSpanElement>(null);
  const [expanded, setExpanded] = useState(false);
  const [overflows, setOverflows] = useState(false);

  const measure = useCallback(() => {
    const el = ref.current;
    if (!el || expanded) return;
    setOverflows(el.scrollHeight - el.clientHeight > 1 || el.scrollWidth - el.clientWidth > 1);
  }, [expanded]);

  useEffect(() => {
    measure();
    const el = ref.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const observer = new ResizeObserver(measure);
    observer.observe(el);
    return () => observer.disconnect();
  }, [measure, children, lines]);

  return (
    <>
      <span
        ref={ref}
        id={id}
        className="mn-dl__clamp"
        data-expanded={expanded || undefined}
        data-single={lines === 1 || undefined}
        style={{ '--_lines': lines } as CSSProperties}
      >
        {children}
      </span>
      {overflows || expanded ? (
        <button
          type="button"
          className="mn-dl__expand"
          aria-expanded={expanded}
          aria-controls={id}
          onClick={() => setExpanded((v) => !v)}
        >
          {expanded ? collapseLabel : expandLabel}
        </button>
      ) : null}
    </>
  );
}
