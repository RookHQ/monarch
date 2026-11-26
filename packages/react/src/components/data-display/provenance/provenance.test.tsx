import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Provenance } from './index.js';

const NOW = '2026-03-10T12:00:00Z';

describe('Provenance', () => {
  it('reads as source and age', async () => {
    const { container } = render(
      <Provenance
        data-testid="root"
        source="Registry"
        checkedAt="2026-03-10T10:00:00Z"
        now={NOW}
        locale="en"
        icon={<svg />}
      />,
    );
    const root = screen.getByTestId('root');
    expect(root).toHaveClass('mn-provenance');
    expect(root).toHaveAttribute('data-variant', 'chip');
    expect(root).toHaveTextContent('Source: Registry · , checked 2 hours ago');
    expect(container.querySelector('time')).toHaveAttribute('datetime', '2026-03-10T10:00:00.000Z');
    expect(container.querySelector('.mn-provenance__icon')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('shows the absolute time on focus', async () => {
    const user = userEvent.setup();
    render(<Provenance source="Import" checkedAt="2026-03-09T12:00:00Z" now={NOW} locale="en" />);
    await user.tab();
    expect(document.activeElement?.tagName).toBe('TIME');
    expect(screen.getByRole('tooltip', { hidden: true })).toBeInTheDocument();
  });

  it('leaves out an invalid or missing time and can be inline', () => {
    render(
      <>
        <Provenance data-testid="a" source="Manual" checkedAt="not a date" />
        <Provenance
          data-testid="b"
          source="Computed"
          variant="inline"
          timeTooltip={false}
          checkedAt={null}
        />
      </>,
    );
    expect(screen.getByTestId('a').querySelector('time')).toBeNull();
    expect(screen.getByTestId('b')).toHaveAttribute('data-variant', 'inline');
  });

  it('renders nothing without a source', () => {
    const { container } = render(<Provenance source="" />);
    expect(container).toBeEmptyDOMElement();
  });

  it('links to safe sources only', async () => {
    const { rerender } = render(<Provenance source="Registry" href="https://example.com/r/1" />);
    expect(screen.getByRole('link', { name: 'Registry' })).toHaveAttribute(
      'href',
      'https://example.com/r/1',
    );
    rerender(<Provenance source="Registry" href="javascript:alert(1)" />);
    expect(screen.queryByRole('link')).toBeNull();
    await expectNoA11yViolations();
  });

  it('uses messages', () => {
    render(
      <Provenance
        data-testid="r"
        source="DNS"
        checkedAt="2026-03-10T11:00:00Z"
        now={NOW}
        locale="en"
        messages={{ source: 'Quelle:', checked: 'geprüft' }}
      />,
    );
    expect(screen.getByTestId('r')).toHaveTextContent('Quelle: DNS');
    expect(screen.getByTestId('r')).toHaveTextContent('geprüft');
  });

  it('renders on the server', () => {
    const html = renderToString(
      <Provenance source="Registry" checkedAt="2026-03-10T10:00:00Z" now={NOW} locale="en" />,
    );
    expect(html).toContain('mn-provenance');
    expect(html).toContain('2 hours ago');
  });
});
