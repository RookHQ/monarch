import '../../styles/foundations.css';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { T } from './token-data.js';

const DISPLAY = ['xl', 'lg', 'md', 'sm'] as const;
const SAMPLE = 'Every Rook experience';

export function TypographyPage() {
  const sizes = Object.entries(T.font.size);
  return (
    <PageFrame
      eyebrow="Foundations"
      title="Typography"
      lead="One sans family, one mono family, a fixed text scale for interfaces and a fluid display scale for public pages."
    >
      <PageSection id="families" title="Families">
        <div className="docs-specimens">
          {Object.entries(T.font.family).map(([k, v]) => (
            <div key={k} className="docs-specimen">
              <p className="docs-specimen__sample" style={{ fontFamily: `var(--mn-font-${k})` }}>
                Aa Bb Cc 0123
              </p>
              <p className="docs-specimen__name">
                <code className="docs-code-inline">--mn-font-{k}</code>
              </p>
              <p className="docs-specimen__meta">
                {String(v.$value)
                  .split(',')[0]
                  ?.replace(/var\(--[\w-]+, |\)/g, '')}
              </p>
            </div>
          ))}
        </div>
      </PageSection>

      <PageSection id="display" title="Display scale">
        <p className="docs-text">
          Fluid sizes for marketing headings: they scale with the viewport between a fixed minimum
          and maximum, with their own leading, tracking and weight.
        </p>
        <div className="docs-scale">
          {DISPLAY.map((k) => (
            <div className="docs-scale__row" key={k}>
              <span className="docs-scale__label">--mn-display-{k}</span>
              <span
                className="docs-scale__sample"
                style={{
                  fontSize: `var(--mn-display-${k})`,
                  lineHeight: `var(--mn-display-${k}-leading)`,
                  letterSpacing: `var(--mn-display-${k}-tracking)`,
                  fontWeight: `var(--mn-display-${k}-weight)`,
                }}
              >
                {SAMPLE}
              </span>
            </div>
          ))}
        </div>
      </PageSection>

      <PageSection id="text-scale" title="Text scale">
        <div className="docs-scale">
          {sizes.map(([k, v]) => (
            <div className="docs-scale__row" key={k}>
              <span className="docs-scale__label">
                --mn-text-{k} · {v.$value}
              </span>
              <span className="docs-scale__sample" style={{ fontSize: `var(--mn-text-${k})` }}>
                The quick brown fox jumps over the lazy dog
              </span>
            </div>
          ))}
        </div>
      </PageSection>

      <PageSection id="weights" title="Weights">
        <div className="docs-scale">
          {Object.entries(T.font.weight).map(([k, v]) => (
            <div className="docs-scale__row" key={k}>
              <span className="docs-scale__label">
                --mn-weight-{k} · {String(v.$value)}
              </span>
              <span
                className="docs-scale__sample docs-scale__sample--lg"
                style={{ fontWeight: `var(--mn-weight-${k})` }}
              >
                {SAMPLE}
              </span>
            </div>
          ))}
        </div>
      </PageSection>
    </PageFrame>
  );
}
