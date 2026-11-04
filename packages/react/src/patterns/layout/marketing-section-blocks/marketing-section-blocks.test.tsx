import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import {
  CTABand,
  FAQ,
  FeatureGrid,
  MarketingSection,
  MarketingSectionHeader,
  ProductFrame,
  StatStrip,
} from './index.js';

function Blocks() {
  return (
    <main>
      <h1>Page</h1>
      <MarketingSection aria-labelledby="features">
        <MarketingSectionHeader
          titleId="features"
          eyebrow="Editor"
          title="Features"
          description="What it does."
        />
        <FeatureGrid
          items={[
            { title: 'Linked', description: 'One', href: '/linked' },
            { title: 'Plain', description: 'Two' },
          ]}
        />
        <ProductFrame label="Editor with an outline" title="Plan">
          <h3>Inside heading</h3>
          <input aria-label="Inside input" />
        </ProductFrame>
      </MarketingSection>
      <MarketingSection tone="inverse" aria-label="Numbers">
        <StatStrip items={[{ value: '24', label: 'Editor blocks' }]} />
      </MarketingSection>
      <MarketingSection aria-label="Questions">
        <FAQ
          items={[
            { question: 'Can I share?', answer: 'Yes, with links.' },
            { question: 'Which formats?', answer: 'HTML and PDF.' },
          ]}
        />
      </MarketingSection>
      <CTABand title="Start now" actions={<button type="button">Go</button>} />
    </main>
  );
}

describe('marketing section blocks', () => {
  it('compose with no axe violations', async () => {
    render(<Blocks />);
    await expectNoA11yViolations();
  });

  it('render on the server', () => {
    const html = renderToString(<Blocks />);
    expect(html).toContain('mn-msection');
    expect(html).toContain('mn-cta');
  });
});

describe('MarketingSection and MarketingSectionHeader', () => {
  it('sets tone, spacing and width and labels the section by its h2', () => {
    const { container } = render(
      <MarketingSection tone="subtle" spacing="lg" width="narrow" aria-labelledby="t">
        <MarketingSectionHeader titleId="t" title="Sharing" align="center" />
      </MarketingSection>,
    );
    const section = container.querySelector('section');
    expect(section).toHaveAttribute('data-tone', 'subtle');
    expect(section).toHaveAttribute('data-spacing', 'lg');
    expect(container.querySelector('.mn-msection__inner')).toHaveAttribute('data-width', 'narrow');
    expect(screen.getByRole('region', { name: 'Sharing' })).toBe(section);
    expect(screen.getByRole('heading', { level: 2, name: 'Sharing' })).toBeInTheDocument();
  });
});

describe('ProductFrame', () => {
  it('is one labelled image whose contents are inert', () => {
    render(
      <ProductFrame label="Share dialog illustration">
        <button type="button">Copy link</button>
      </ProductFrame>,
    );
    const img = screen.getByRole('img', { name: 'Share dialog illustration' });
    // jsdom does not implement inert; the docs-site axe run covers real browsers.
    expect(img.querySelector('.mn-product-frame__body')).toHaveAttribute('inert');
  });
});

describe('FeatureGrid', () => {
  it('makes a linked card a single tab stop named by its title', async () => {
    render(
      <FeatureGrid
        items={[
          { title: 'Search', description: 'Find it', href: '/search' },
          { title: 'Share', description: 'Send it' },
        ]}
      />,
    );
    const user = userEvent.setup();
    await user.tab();
    expect(screen.getByRole('link', { name: 'Search' })).toHaveFocus();
    await user.tab();
    expect(document.body).toHaveFocus();
    expect(screen.getAllByRole('heading', { level: 3 })).toHaveLength(2);
  });
});

describe('StatStrip', () => {
  it('pairs each label with its value in a description list', () => {
    render(<StatStrip items={[{ value: '13', label: 'Interface languages' }]} />);
    const term = screen.getByText('Interface languages');
    expect(term.tagName).toBe('DT');
    expect(term.nextElementSibling?.tagName).toBe('DD');
    expect(term.nextElementSibling).toHaveTextContent('13');
  });
});

describe('FAQ', () => {
  it('opens defaultOpen and toggles answers from the keyboard', async () => {
    render(
      <FAQ
        defaultOpen={1}
        items={[
          { question: 'Can I share?', answer: 'Yes, with links.' },
          { question: 'Which formats?', answer: 'HTML and PDF.' },
        ]}
      />,
    );
    const user = userEvent.setup();
    expect(screen.getByRole('button', { name: 'Which formats?' })).toHaveAttribute(
      'aria-expanded',
      'true',
    );
    const first = screen.getByRole('button', { name: 'Can I share?' });
    expect(first.closest('h3')).not.toBeNull();
    first.focus();
    await user.keyboard('{Enter}');
    expect(first).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('Yes, with links.')).toBeVisible();
  });
});

describe('CTABand', () => {
  it('renders an h2 by default, honours headingAs and sets the tone', () => {
    const { rerender, container } = render(<CTABand title="Start now" />);
    expect(screen.getByRole('heading', { level: 2, name: 'Start now' })).toBeInTheDocument();
    expect(container.querySelector('.mn-cta')).toHaveAttribute('data-tone', 'accent');
    rerender(<CTABand title="Start now" headingAs="h3" tone="subtle" />);
    expect(screen.getByRole('heading', { level: 3, name: 'Start now' })).toBeInTheDocument();
    expect(container.querySelector('.mn-cta')).toHaveAttribute('data-tone', 'subtle');
  });
});
