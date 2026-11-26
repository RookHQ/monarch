import '../../styles/foundations.css';
import { contrast, primitives, toHex } from '@rookcom/monarch-tokens';
import { Badge } from '@rookcom/monarch-react';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { Swatch } from './swatch.js';

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
const INTENTS = [
  ['accent', 'Accent', 'The product brand. Never a status.'],
  ['danger', 'Danger', 'Rook Red: errors and destructive actions.'],
  ['success', 'Success', 'Rook Green: completed and healthy.'],
  ['warning', 'Warning', 'Rook Yellow: needs attention.'],
  ['info', 'Info', 'Rook Blue: neutral information.'],
] as const;

export function ColoursPage() {
  const brandEntries = Object.entries(primitives.brand);
  return (
    <PageFrame
      eyebrow="Foundations"
      title="Colours"
      lead="Live values for the current theme, contrast and product accent. Change them in the appearance settings and every swatch and ratio updates. Ratios are the WCAG contrast of the sample text against the swatch."
    >
      <PageSection id="brand" title="Brand">
        <p className="docs-text">
          The only hard-coded colours in Monarch. Every other colour derives from them.
        </p>
        <div className="docs-swatches">
          {brandEntries.map(([name, c]) => {
            const hex = toHex(c);
            return (
              <div className="docs-swatch" key={name}>
                <div className="docs-swatch__chip" style={{ background: hex }} />
                <div className="docs-swatch__text">
                  <span className="docs-swatch__name">brand.{name}</span>
                  <span className="docs-swatch__meta">
                    {hex} · white text {contrast(primitives.neutral[0], c).toFixed(2)}:1
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </PageSection>

      <PageSection id="surfaces" title="Surfaces">
        <div className="docs-swatches">
          {NEUTRAL_BG.map((r) => (
            <Swatch
              key={r}
              name={`--mn-color-${r}`}
              bgVar={`--mn-color-${r}`}
              fgVar={r === 'bg-inverse' ? '--mn-color-text-inverse' : undefined}
            />
          ))}
        </div>
      </PageSection>

      <PageSection id="text" title="Text on canvas">
        <div className="docs-swatches">
          {TEXT.map((r) => (
            <Swatch
              key={r}
              name={`--mn-color-${r}`}
              bgVar="--mn-color-bg-canvas"
              fgVar={`--mn-color-${r}`}
            />
          ))}
        </div>
      </PageSection>

      <PageSection id="borders" title="Borders">
        <div className="docs-swatches">
          {BORDER.map((r) => (
            <Swatch key={r} name={`--mn-color-${r}`} bgVar={`--mn-color-${r}`} label=" " />
          ))}
        </div>
      </PageSection>

      {INTENTS.map(([i, title, meaning]) => (
        <PageSection key={i} id={`intent-${i}`} title={title}>
          <p className="docs-text">
            <Badge tone={i}>{i}</Badge> {meaning}
          </p>
          <div className="docs-swatches">
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
        </PageSection>
      ))}
    </PageFrame>
  );
}
