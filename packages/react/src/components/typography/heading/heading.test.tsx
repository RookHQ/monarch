import { describe, expect, it } from 'vitest';
import { createRef } from 'react';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Heading, HEADING_DEFAULT_SIZES } from './index.js';

describe('Heading', () => {
  it('renders an h2 by default with the level size and forwards ref and props', async () => {
    const ref = createRef<HTMLHeadingElement>();
    render(
      <Heading ref={ref} id="intro" data-testid="root">
        Overview
      </Heading>,
    );
    const heading = screen.getByRole('heading', { level: 2, name: 'Overview' });
    expect(heading).toBe(ref.current);
    expect(heading).toHaveClass('mn-heading');
    expect(heading).toHaveAttribute('id', 'intro');
    expect(heading).toHaveAttribute('data-size', '2xl');
    expect(heading).toHaveAttribute('data-weight', 'semibold');
    expect(heading).not.toHaveAttribute('data-tone');
    await expectNoA11yViolations();
  });

  it('keeps the outline level separate from the visual size', () => {
    render(
      <>
        <Heading level={1}>Page</Heading>
        <Heading level={3} size="base">
          Card title
        </Heading>
        <Heading level={2} size="4xl" weight="bold" tone="muted" truncate>
          Big
        </Heading>
      </>,
    );
    expect(screen.getByRole('heading', { level: 1 })).toHaveAttribute('data-size', '3xl');
    const card = screen.getByRole('heading', { level: 3, name: 'Card title' });
    expect(card.tagName).toBe('H3');
    expect(card).toHaveAttribute('data-size', 'base');
    const big = screen.getByRole('heading', { level: 2, name: 'Big' });
    expect(big).toHaveAttribute('data-size', '4xl');
    expect(big).toHaveAttribute('data-weight', 'bold');
    expect(big).toHaveAttribute('data-tone', 'muted');
    expect(big).toHaveAttribute('data-truncate');
  });

  it('maps every level to a default size', () => {
    for (const level of [1, 2, 3, 4, 5, 6] as const) {
      const { unmount } = render(<Heading level={level}>Level {level}</Heading>);
      expect(screen.getByRole('heading', { level })).toHaveAttribute(
        'data-size',
        HEADING_DEFAULT_SIZES[level],
      );
      unmount();
    }
  });

  it('renders on the server', () => {
    const html = renderToString(<Heading level={4}>Settings</Heading>);
    expect(html).toContain('<h4');
    expect(html).toContain('mn-heading');
    expect(html).toContain('data-size="lg"');
  });
});
