import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Prose, ProseExclude } from './index.js';

function Article() {
  return (
    <>
      <h2 id="setup">Setup</h2>
      <p>
        Install the package, then read the <a href="#usage">usage notes</a>. Run{' '}
        <code>npm test</code> to check.
      </p>
      <ul>
        <li>First point</li>
        <li>Second point</li>
      </ul>
      <blockquote>
        <p>A quoted line.</p>
      </blockquote>
      <pre>
        <code>const answer = 42;</code>
      </pre>
      <table>
        <thead>
          <tr>
            <th scope="col">Option</th>
            <th scope="col">Default</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>size</td>
            <td>base</td>
          </tr>
        </tbody>
      </table>
      <h3 id="usage">Usage</h3>
      <p>
        Press <kbd>Enter</kbd> to confirm.
      </p>
    </>
  );
}

describe('Prose', () => {
  it('wraps rendered content with defaults and forwards ref and props', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <Prose ref={ref} data-testid="root" aria-label="Guide">
        <Article />
      </Prose>,
    );
    const root = screen.getByTestId('root');
    expect(root).toBe(ref.current);
    expect(root.tagName).toBe('DIV');
    expect(root).toHaveClass('mn-prose');
    expect(root).toHaveAttribute('data-size', 'base');
    expect(root).toHaveAttribute('data-measure');
    expect(screen.getByRole('heading', { level: 2, name: 'Setup' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'usage notes' })).toHaveAttribute('href', '#usage');
    expect(screen.getByRole('table')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('renders as an article, with a size and without the reading measure', () => {
    render(
      <Prose as="article" size="sm" measure={false} data-testid="root">
        <p>Short comment.</p>
      </Prose>,
    );
    const root = screen.getByTestId('root');
    expect(root.tagName).toBe('ARTICLE');
    expect(root).toHaveAttribute('data-size', 'sm');
    expect(root).not.toHaveAttribute('data-measure');
  });

  it('marks excluded content so Prose styles skip it', () => {
    const ref = createRef<HTMLDivElement>();
    render(
      <Prose>
        <ProseExclude ref={ref} data-testid="ex">
          <p>Widget</p>
        </ProseExclude>
      </Prose>,
    );
    expect(screen.getByTestId('ex')).toBe(ref.current);
    expect(screen.getByTestId('ex')).toHaveClass('mn-prose__exclude');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Prose size="lg">
        <Article />
      </Prose>,
    );
    expect(html).toContain('mn-prose');
    expect(html).toContain('data-size="lg"');
    expect(html).toContain('<h2 id="setup">');
  });
});
