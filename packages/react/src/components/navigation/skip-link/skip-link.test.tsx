import { describe, expect, it, vi } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { SkipLink } from './index.js';

function Page(props: { href?: string; onClick?: () => void }) {
  return (
    <>
      <SkipLink {...props} />
      <nav aria-label="Main">
        <a href="#/one">One</a>
      </nav>
      <main id="main-content">
        <h1>Title</h1>
      </main>
    </>
  );
}

describe('SkipLink', () => {
  it('is the first tab stop with the default name', async () => {
    render(<Page />);
    const user = userEvent.setup();
    await user.tab();
    const link = screen.getByRole('link', { name: 'Skip to main content' });
    expect(link).toHaveFocus();
    expect(link).toHaveAttribute('href', '#main-content');
    expect(link).toHaveClass('mn-skip-link');
    await expectNoA11yViolations();
  });

  it('moves focus to the target with Enter, making it focusable', async () => {
    render(<Page />);
    const user = userEvent.setup();
    await user.tab();
    await user.keyboard('{Enter}');
    const main = screen.getByRole('main');
    expect(main).toHaveFocus();
    expect(main).toHaveAttribute('tabindex', '-1');
  });

  it('keeps an existing tabindex, calls onClick and supports custom targets', async () => {
    const onClick = vi.fn();
    render(
      <>
        <SkipLink href="#search" onClick={onClick}>
          Skip to search
        </SkipLink>
        <input id="search" aria-label="Search" />
      </>,
    );
    const user = userEvent.setup();
    await user.click(screen.getByRole('link', { name: 'Skip to search' }));
    expect(onClick).toHaveBeenCalledTimes(1);
    const input = screen.getByRole('textbox', { name: 'Search' });
    expect(input).toHaveFocus();
    expect(input).not.toHaveAttribute('tabindex');
  });

  it('translates the label and forwards its ref', () => {
    const ref = createRef<HTMLAnchorElement>();
    render(<SkipLink ref={ref} messages={{ label: 'Zum Inhalt springen' }} />);
    expect(screen.getByRole('link', { name: 'Zum Inhalt springen' })).toBe(ref.current);
  });

  it('does nothing special when the target is missing', async () => {
    render(<SkipLink href="#nowhere" />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('link'));
    expect(screen.getByRole('link')).toHaveFocus();
  });

  it('does not throw on a target id with a stray percent sign', async () => {
    render(
      <>
        <SkipLink href="#100%-done" />
        <main id="100%-done">Content</main>
      </>,
    );
    await userEvent.setup().click(screen.getByRole('link'));
    expect(screen.getByRole('main')).toHaveFocus();
  });

  it('renders on the server', () => {
    const html = renderToString(<SkipLink />);
    expect(html).toContain('mn-skip-link');
    expect(html).toContain('Skip to main content');
  });
});
