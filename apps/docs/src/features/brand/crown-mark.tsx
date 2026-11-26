/** The Monarch crown: a closed outline on a 120×80 grid, in `currentColor`. */
const CROWN_PATH = 'M8 20l26 24L60 8l26 36 26-24-12 52H20z';

export function CrownMark({
  strokeWidth = 9,
  animated = false,
  className,
}: {
  strokeWidth?: number;
  /** Reveals the crown on first paint, with opacity and scale only (static under reduced motion). */
  animated?: boolean;
  className?: string;
}) {
  return (
    <svg
      viewBox="0 0 120 80"
      className={
        [className, animated ? 'docs-crown-draw' : null].filter(Boolean).join(' ') || undefined
      }
      aria-hidden
      focusable="false"
    >
      <path
        d={CROWN_PATH}
        fill="none"
        stroke="currentColor"
        strokeWidth={strokeWidth}
        strokeLinejoin="round"
        strokeLinecap="round"
      />
    </svg>
  );
}

/** Crown plus "Monarch", the site's wordmark. */
export function Wordmark() {
  return (
    <span className="docs-wordmark">
      <CrownMark strokeWidth={10} className="docs-wordmark__mark" />
      <span className="docs-wordmark__name">Monarch</span>
    </span>
  );
}
