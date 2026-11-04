import { useEffect, useState } from 'react';
import { contrast, primitives, toHex } from '@rookcom/monarch-tokens';
import tokens from '@rookcom/monarch-tokens/tokens.json';
import { Badge } from '@rookcom/monarch-react';

type ColorToken = { $type: 'color'; $value: string };
type Dim = { $type: string; $value: string | number };
const T = tokens as unknown as {
  themes: Record<string, { color: Record<string, ColorToken | Record<string, ColorToken>> }>;
  font: { size: Record<string, Dim>; weight: Record<string, Dim> };
  space: Record<string, Dim>;
  radius: Record<string, Dim>;
  duration: Record<string, Dim>;
};

/** Reads the live computed value of a CSS variable so swatches follow the current product and theme. */
function useVar(name: string): string {
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

function Swatch({
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
      {/* Sample text is decorative; the ratio is reported in the caption. Disabled text is shown below AA on purpose. */}
      <div
        className="docs-swatch__chip"
        style={{ background: `var(${bgVar})`, color: `var(${fgVar ?? '--mn-color-text-default'})` }}
        aria-hidden
      >
        {label ?? 'Aa'}
      </div>
      <span className="docs-swatch__name">{name}</span>
      <span className="docs-swatch__meta">
        {bg} {ratio ? `· ${ratio.toFixed(2)}:1` : ''}
      </span>
    </div>
  );
}

/** Parses `hsl(h s% l%)` as emitted by Monarch; returns null for anything else. */
function parseHsl(v: string) {
  const m = /hsl\(\s*([\d.]+)\s+([\d.]+)%\s+([\d.]+)%/.exec(v);
  return m ? { h: Number(m[1]), s: Number(m[2]), l: Number(m[3]) } : null;
}
function ratioOf(fg: string, bg: string): number | null {
  const a = parseHsl(fg);
  const b = parseHsl(bg);
  return a && b ? contrast(a, b) : null;
}

const NEUTRAL_BG = [
  'bg-canvas',
  'bg-surface',
  'bg-raised',
  'bg-subtle',
  'bg-muted',
  'bg-emphasis',
  'bg-inverse',
];
const TEXT = ['text-default', 'text-muted', 'text-subtle', 'text-disabled', 'text-link'];
const BORDER = ['border-subtle', 'border-default', 'border-strong', 'border-input'];
const INTENTS = ['accent', 'danger', 'success', 'warning', 'info'];

export function TokensPage() {
  const brandEntries = Object.entries(primitives.brand);
  return (
    <>
      <h1>Tokens</h1>
      <p className="docs-lead">
        Live values for the current theme and product accent. Change them in the header and every
        swatch, ratio and example updates. Ratios are WCAG contrast of the shown text against the
        swatch.
      </p>

      <h2 className="docs-h2">Brand</h2>
      <p className="docs-prose">
        The only hard-coded colours in Monarch, from rookhq.com/brand-kit. Everything else derives
        from them.
      </p>
      <div className="docs-grid">
        {brandEntries.map(([name, c]) => {
          const hex = toHex(c);
          return (
            <div className="docs-swatch" key={name}>
              <div className="docs-swatch__chip" style={{ background: hex }} />
              <span className="docs-swatch__name">brand.{name}</span>
              <span className="docs-swatch__meta">
                {hex} · white text {contrast(primitives.neutral[0], c).toFixed(2)}:1
              </span>
            </div>
          );
        })}
      </div>

      <h2 className="docs-h2">Surfaces</h2>
      <div className="docs-grid">
        {NEUTRAL_BG.map((r) => (
          <Swatch
            key={r}
            name={`--mn-color-${r}`}
            bgVar={`--mn-color-${r}`}
            fgVar={r === 'bg-inverse' ? '--mn-color-text-inverse' : undefined}
          />
        ))}
      </div>

      <h2 className="docs-h2">Text on canvas</h2>
      <div className="docs-grid">
        {TEXT.map((r) => (
          <Swatch
            key={r}
            name={`--mn-color-${r}`}
            bgVar="--mn-color-bg-canvas"
            fgVar={`--mn-color-${r}`}
          />
        ))}
      </div>

      <h2 className="docs-h2">Borders on surface</h2>
      <div className="docs-grid">
        {BORDER.map((r) => (
          <Swatch key={r} name={`--mn-color-${r}`} bgVar={`--mn-color-${r}`} label=" " />
        ))}
      </div>

      {INTENTS.map((i) => (
        <div key={i}>
          <h2 className="docs-h2">
            {i} <Badge tone={i === 'accent' ? 'accent' : (i as 'danger')}>{i}</Badge>
          </h2>
          <div className="docs-grid">
            <Swatch
              name={`${i}-solid`}
              bgVar={`--mn-color-${i}-solid`}
              fgVar={`--mn-color-${i}-on-solid`}
            />
            <Swatch
              name={`${i}-solid-hover`}
              bgVar={`--mn-color-${i}-solid-hover`}
              fgVar={`--mn-color-${i}-on-solid`}
            />
            <Swatch
              name={`${i}-solid-active`}
              bgVar={`--mn-color-${i}-solid-active`}
              fgVar={`--mn-color-${i}-on-solid`}
            />
            <Swatch
              name={`${i}-subtle`}
              bgVar={`--mn-color-${i}-subtle`}
              fgVar={`--mn-color-${i}-strong`}
            />
            <Swatch
              name={`${i}-subtle-hover`}
              bgVar={`--mn-color-${i}-subtle-hover`}
              fgVar={`--mn-color-${i}-strong`}
            />
            <Swatch
              name={`${i}-strong on canvas`}
              bgVar="--mn-color-bg-canvas"
              fgVar={`--mn-color-${i}-strong`}
            />
            <Swatch
              name={`${i}-strong`}
              bgVar={`--mn-color-${i}-strong`}
              fgVar={`--mn-color-${i}-on-strong`}
            />
            <Swatch name={`${i}-border`} bgVar={`--mn-color-${i}-border`} label=" " />
          </div>
        </div>
      ))}

      <h2 className="docs-h2">Type scale</h2>
      {Object.entries(T.font.size).map(([k, v]) => (
        <div className="docs-row" key={k}>
          <span className="docs-row__label">
            --mn-text-{k} · {v.$value}
          </span>
          <span style={{ fontSize: `var(--mn-text-${k})` }}>
            The quick brown fox jumps over the lazy dog
          </span>
        </div>
      ))}
      <div className="docs-row">
        <span className="docs-row__label">weights</span>
        {Object.entries(T.font.weight).map(([k, v]) => (
          <span key={k} style={{ fontWeight: `var(--mn-weight-${k})` }}>
            {k} {String(v.$value)}
          </span>
        ))}
      </div>

      <h2 className="docs-h2">Spacing</h2>
      {Object.entries(T.space).map(([k, v]) => (
        <div className="docs-row" key={k}>
          <span className="docs-row__label">
            --mn-space-{k.replace('.', '-')} · {v.$value}
          </span>
          <span
            className="docs-space-bar"
            style={{ width: `var(--mn-space-${k.replace('.', '-')})` }}
          />
        </div>
      ))}

      <h2 className="docs-h2">Radius</h2>
      <div className="docs-grid">
        {Object.entries(T.radius).map(([k, v]) => (
          <div className="docs-swatch" key={k}>
            <div className="docs-radius-box" style={{ borderRadius: `var(--mn-radius-${k})` }} />
            <span className="docs-swatch__name">--mn-radius-{k}</span>
            <span className="docs-swatch__meta">{v.$value}</span>
          </div>
        ))}
      </div>

      <h2 className="docs-h2">Elevation</h2>
      <div className="docs-grid">
        {['xs', 'sm', 'md', 'lg', 'xl'].map((k) => (
          <div className="docs-swatch" key={k}>
            <div className="docs-shadow-box" style={{ boxShadow: `var(--mn-shadow-${k})` }} />
            <span className="docs-swatch__name">--mn-shadow-{k}</span>
          </div>
        ))}
      </div>

      <h2 className="docs-h2">Motion</h2>
      <p className="docs-prose">
        Hover a row to see the duration. All durations become 0ms under{' '}
        <code>prefers-reduced-motion</code>.
      </p>
      {Object.entries(T.duration).map(([k, v]) => (
        <div className="docs-row" key={k}>
          <span className="docs-row__label">
            --mn-duration-{k} · {v.$value}
          </span>
          <span
            className="docs-motion-dot"
            style={{
              transitionDuration: `var(--mn-duration-${k})`,
              transitionTimingFunction: 'var(--mn-ease-standard)',
            }}
          />
        </div>
      ))}
    </>
  );
}
