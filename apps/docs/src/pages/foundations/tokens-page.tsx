import '../../styles/foundations.css';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { T, cssKey } from './token-data.js';

const LAYOUT = [
  ['--mn-layout-content-max', 'Widest content column in an app'],
  ['--mn-layout-reading-max', 'Comfortable line length for prose'],
  ['--mn-layout-gutter', 'Page side padding, narrow screens'],
  ['--mn-layout-gutter-lg', 'Page side padding, wide screens'],
  ['--mn-width-narrow', 'Marketing: narrow column'],
  ['--mn-width-content', 'Marketing: content column'],
  ['--mn-width-wide', 'Marketing: wide column'],
] as const;

export function TokensPage() {
  return (
    <PageFrame
      eyebrow="Foundations"
      title="Tokens"
      lead="Spacing, radius, elevation, motion and layout, as CSS variables. Colour and type have their own pages. Use the variables or the Tailwind preset; never raw values."
    >
      <PageSection id="spacing" title="Spacing">
        <p className="docs-text">A 4px grid with half steps at the small end.</p>
        <div className="docs-scale">
          {Object.entries(T.space).map(([k, v]) => (
            <div className="docs-scale__row" key={k}>
              <span className="docs-scale__label">
                --mn-space-{cssKey(k)} · {v.$value}
              </span>
              <span className="docs-space-bar" style={{ width: `var(--mn-space-${cssKey(k)})` }} />
            </div>
          ))}
        </div>
      </PageSection>

      <PageSection id="radius" title="Radius">
        <div className="docs-swatches docs-swatches--compact">
          {Object.entries(T.radius).map(([k, v]) => (
            <div className="docs-swatch" key={k}>
              <div className="docs-radius-box" style={{ borderRadius: `var(--mn-radius-${k})` }} />
              <div className="docs-swatch__text">
                <span className="docs-swatch__name">--mn-radius-{k}</span>
                <span className="docs-swatch__meta">{v.$value}</span>
              </div>
            </div>
          ))}
        </div>
      </PageSection>

      <PageSection id="elevation" title="Elevation">
        <div className="docs-swatches docs-swatches--compact">
          {['xs', 'sm', 'md', 'lg', 'xl'].map((k) => (
            <div className="docs-swatch" key={k}>
              <div className="docs-shadow-box" style={{ boxShadow: `var(--mn-shadow-${k})` }} />
              <div className="docs-swatch__text">
                <span className="docs-swatch__name">--mn-shadow-{k}</span>
              </div>
            </div>
          ))}
        </div>
      </PageSection>

      <PageSection id="motion" title="Motion">
        <p className="docs-text">
          Hover a row to see the duration. All durations become 0ms under{' '}
          <code className="docs-code-inline">prefers-reduced-motion</code>.
        </p>
        <div className="docs-scale">
          {Object.entries(T.duration).map(([k, v]) => (
            <div className="docs-scale__row docs-motion" key={k}>
              <span className="docs-scale__label">
                --mn-duration-{k} · {v.$value}
              </span>
              <span
                className="docs-motion__dot"
                style={{
                  transitionDuration: `var(--mn-duration-${k})`,
                  transitionTimingFunction: 'var(--mn-ease-standard)',
                }}
              />
            </div>
          ))}
        </div>
      </PageSection>

      <PageSection id="layout" title="Layout">
        <div className="docs-table-scroll">
          <table className="docs-table">
            <thead>
              <tr>
                <th scope="col">Variable</th>
                <th scope="col">Use</th>
              </tr>
            </thead>
            <tbody>
              {LAYOUT.map(([name, use]) => (
                <tr key={name}>
                  <td>
                    <code className="docs-code-inline">{name}</code>
                  </td>
                  <td>{use}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </PageSection>
    </PageFrame>
  );
}
