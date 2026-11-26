import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { InlineCode } from './index.js';

describe('InlineCode', () => {
  it('renders a code element inside text and forwards ref and props', async () => {
    const ref = createRef<HTMLElement>();
    render(
      <p>
        Run{' '}
        <InlineCode ref={ref} data-testid="code" title="Install command">
          npm install
        </InlineCode>{' '}
        first.
      </p>,
    );
    const code = screen.getByTestId('code');
    expect(code).toBe(ref.current);
    expect(code.tagName).toBe('CODE');
    expect(code).toHaveClass('mn-inline-code');
    expect(code).toHaveAttribute('data-variant', 'subtle');
    expect(code).not.toHaveAttribute('data-nowrap');
    expect(code).toHaveAttribute('title', 'Install command');
    expect(code.closest('p')).toHaveTextContent('Run npm install first.');
    await expectNoA11yViolations();
  });

  it('exposes the plain variant and nowrap', () => {
    render(
      <InlineCode data-testid="code" variant="plain" nowrap>
        main
      </InlineCode>,
    );
    expect(screen.getByTestId('code')).toHaveAttribute('data-variant', 'plain');
    expect(screen.getByTestId('code')).toHaveAttribute('data-nowrap');
  });

  it('renders on the server', () => {
    const html = renderToString(<InlineCode>git status</InlineCode>);
    expect(html).toContain('<code');
    expect(html).toContain('mn-inline-code');
    expect(html).toContain('git status');
  });
});
