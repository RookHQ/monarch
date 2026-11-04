import { useEffect, useState } from 'react';
import { contrast } from '@rookcom/monarch-tokens';

/** Reads the live computed value of a CSS variable, so swatches follow the current product and theme. */
export function useVar(name: string): string {
  const [v, setV] = useState('');
  useEffect(() => {
    const read = () =>
      setV(getComputedStyle(document.documentElement).getPropertyValue(name).trim());
    read();
    const obs = new MutationObserver(read);
    obs.observe(document.documentElement, { attributes: true });
    return () => obs.disconnect();
  }, [name]);
  return v;
}

/** Parses `hsl(h s% l%)` as emitted by Monarch; returns null for anything else. */
function parseHsl(v: string) {
  const m = /hsl\(\s*([\d.]+)\s+([\d.]+)%\s+([\d.]+)%/.exec(v);
  return m ? { h: Number(m[1]), s: Number(m[2]), l: Number(m[3]) } : null;
}

export function ratioOf(fg: string, bg: string): number | null {
  const a = parseHsl(fg);
  const b = parseHsl(bg);
  return a && b ? contrast(a, b) : null;
}

/**
 * One colour token: a chip in the live value, its name, value and the WCAG
 * contrast of `fgVar` on it.
 */
export function Swatch({
  name,
  bgVar,
  fgVar,
  label,
}: {
  name: string;
  bgVar: string;
  fgVar?: string | undefined;
  label?: string | undefined;
}) {
  const bg = useVar(bgVar);
  const fg = useVar(fgVar ?? '--mn-color-text-default');
  const ratio = bg && fg ? ratioOf(fg, bg) : null;
  return (
    <div className="docs-swatch">
      {/* Sample text is decorative; the ratio is in the caption. Disabled text is shown below AA on purpose. */}
      <div
        className="docs-swatch__chip"
        style={{ background: `var(${bgVar})`, color: `var(${fgVar ?? '--mn-color-text-default'})` }}
        aria-hidden
      >
        {label ?? 'Aa'}
      </div>
      <div className="docs-swatch__text">
        <span className="docs-swatch__name">{name}</span>
        <span className="docs-swatch__meta">
          {bg} {ratio ? `· ${ratio.toFixed(2)}:1` : ''}
        </span>
      </div>
    </div>
  );
}
