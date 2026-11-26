import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PresenceDot, presenceLabel } from './index.js';

describe('PresenceDot', () => {
  it('reads its status as text and exposes it as data-status', async () => {
    render(
      <p>
        Ada Lovelace <PresenceDot status="online" data-testid="dot" />
      </p>,
    );
    const dot = screen.getByTestId('dot');
    expect(dot).toHaveClass('mn-presence-dot');
    expect(dot).toHaveAttribute('data-status', 'online');
    expect(dot).toHaveAttribute('title', 'Online');
    expect(screen.getByText('Ada Lovelace', { exact: false })).toHaveTextContent(
      'Ada Lovelace Online',
    );
    await expectNoA11yViolations();
  });

  it('adds the custom status to the text', async () => {
    render(
      <PresenceDot
        status="away"
        customStatus={{ emoji: '🌴', text: 'On holiday' }}
        data-testid="dot"
      />,
    );
    expect(screen.getByTestId('dot')).toHaveTextContent('Away · 🌴 On holiday');
    expect(screen.getByTestId('dot')).toHaveAttribute('title', 'Away · 🌴 On holiday');
    await expectNoA11yViolations();
  });

  it('distinguishes do not disturb from busy', () => {
    render(
      <>
        <PresenceDot status="dnd" data-testid="dnd" />
        <PresenceDot status="busy" data-testid="busy" />
      </>,
    );
    expect(screen.getByTestId('dnd')).toHaveTextContent('Do not disturb');
    expect(screen.getByTestId('busy')).toHaveTextContent('Busy');
  });

  it('can be decorative, overlaid and sized', async () => {
    render(
      <span style={{ position: 'relative' }}>
        <PresenceDot status="offline" decorative overlay size="sm" data-testid="dot" />
      </span>,
    );
    const dot = screen.getByTestId('dot');
    expect(dot).toHaveAttribute('aria-hidden', 'true');
    expect(dot).toHaveAttribute('data-overlay');
    expect(dot).toHaveAttribute('data-size', 'sm');
    expect(dot).toBeEmptyDOMElement();
    await expectNoA11yViolations();
  });

  it('translates and can drop the title', () => {
    render(
      <PresenceDot
        status="online"
        showTitle={false}
        messages={{ online: 'En ligne' }}
        data-testid="dot"
      />,
    );
    expect(screen.getByTestId('dot')).toHaveTextContent('En ligne');
    expect(screen.getByTestId('dot')).not.toHaveAttribute('title');
  });

  it('presenceLabel ignores empty custom status parts', () => {
    expect(presenceLabel('online', { text: '  ' })).toBe('Online');
    expect(presenceLabel('busy', { text: 'Focusing' })).toBe('Busy · Focusing');
  });

  it('renders on the server', () => {
    const html = renderToString(<PresenceDot status="away" />);
    expect(html).toContain('mn-presence-dot');
    expect(html).toContain('Away');
  });
});
