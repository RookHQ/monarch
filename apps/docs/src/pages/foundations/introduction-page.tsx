import { Button } from '@rookcom/monarch-react';
import { PageFrame, PageSection } from '../../layout/page-frame.js';
import { REPO_URL } from '../../layout/top-bar.js';
import { CodeBlock } from '../../features/code/code-block.js';
import { facts, INSTALL_COMMAND } from '../../lib/facts.js';
import { href } from '../../lib/router.js';

const PACKAGES = [
  [
    '@rookcom/monarch-tokens',
    'CSS variables, light, dark and high-contrast themes, per-product accents with checked contrast, a Tailwind 3 preset and tokens.json.',
    'Nothing',
  ],
  [
    '@rookcom/monarch-react',
    'Accessible, typed React components and patterns (React 18 and 19, server-rendering safe) and styles.css.',
    'Tokens, React',
  ],
  [
    '@rookcom/monarch-conformance',
    'The monarch CLI: design-conformance checks, SARIF output and registry verification.',
    'Node only',
  ],
] as const;

export function IntroductionPage() {
  return (
    <PageFrame
      eyebrow="Foundations"
      title="Introduction"
      lead="Monarch is Rook's open-source design system: design tokens, accessible React components and the conformance tooling that keeps many products consistent, without any of them depending on a live Monarch service. It is released under the MIT License."
      meta={
        <>
          <Button asChild variant="primary" size="sm">
            <a href={href('/getting-started')}>Get started</a>
          </Button>
          <Button asChild size="sm">
            <a href={REPO_URL}>View the source</a>
          </Button>
        </>
      }
    >
      <PageSection id="packages" title="Packages">
        <div className="docs-table-scroll">
          <table className="docs-table">
            <thead>
              <tr>
                <th scope="col">Package</th>
                <th scope="col">What it is</th>
                <th scope="col">Needs</th>
              </tr>
            </thead>
            <tbody>
              {PACKAGES.map(([name, what, needs]) => (
                <tr key={name}>
                  <td>
                    <code className="docs-code-inline">{name}</code>
                  </td>
                  <td>{what}</td>
                  <td>{needs}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <CodeBlock code={INSTALL_COMMAND} label="Code: install" />
      </PageSection>

      <PageSection id="in-numbers" title="In this release">
        <dl className="docs-facts">
          <div>
            <dt>Components</dt>
            <dd>{facts.components}</dd>
          </div>
          <div>
            <dt>Patterns</dt>
            <dd>{facts.patterns}</dd>
          </div>
          <div>
            <dt>Live examples</dt>
            <dd>{facts.examples}</dd>
          </div>
          <div>
            <dt>Themes</dt>
            <dd>{facts.themes}</dd>
          </div>
        </dl>
        <p className="docs-footnote">
          Counted from the docs files of{' '}
          <code className="docs-code-inline">@rookcom/monarch-react</code> {facts.reactVersion} and
          the tokens package.
        </p>
      </PageSection>

      <PageSection id="how-it-fits" title="How it fits together">
        <ol className="docs-steps">
          <li>
            <strong>Tokens</strong> define every colour, size and motion value as a CSS variable,
            for each theme and product accent.
          </li>
          <li>
            <strong>Components</strong> use only those variables, so a theme or accent change
            reaches every control without new code.
          </li>
          <li>
            <strong>Conformance</strong> scans a product's code for raw colours, off-grid spacing
            and local copies of Monarch components, and reports counts.
          </li>
        </ol>
        <p className="docs-text">
          Next: <a href={href('/colours')}>Colours</a>, <a href={href('/theming')}>Theming</a> or
          the <a href={href('/components')}>component overview</a>.
        </p>
      </PageSection>
    </PageFrame>
  );
}
