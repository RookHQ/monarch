import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { MarketingHero } from './index.js';

describe('MarketingHero', () => {
  it('renders the page h1 with eyebrow, lead, actions and note', async () => {
    render(
      <main>
        <MarketingHero
          eyebrow="Rook Example"
          title="Write it once."
          lead="Lead copy."
          actions={<button type="button">Start</button>}
          note="Free for small teams."
        />
      </main>,
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Write it once.' })).toBeInTheDocument();
    expect(screen.getByText('Lead copy.')).toBeInTheDocument();
    expect(screen.getByText('Free for small teams.')).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('supports layout, background and headingAs', () => {
    const { container } = render(
      <MarketingHero
        title="Launch"
        layout="split"
        background="none"
        headingAs="h2"
        media={<p>Media</p>}
      />,
    );
    const hero = container.querySelector('.mn-hero');
    expect(hero).toHaveAttribute('data-layout', 'split');
    expect(hero).toHaveAttribute('data-background', 'none');
    expect(screen.getByRole('heading', { level: 2, name: 'Launch' })).toBeInTheDocument();
    expect(screen.getByText('Media')).toBeInTheDocument();
  });

  it('renders on the server', () => {
    expect(renderToString(<MarketingHero title="Hello" />)).toContain('mn-hero__title');
  });
});
