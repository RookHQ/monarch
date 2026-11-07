import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { ReadReceipt } from './index.js';

const readers = [
  { id: 'u1', name: 'Ana Lopez' },
  { id: 'u2', name: 'Ben Okafor' },
  { id: 'u3', name: 'Chloé Martin' },
];

describe('ReadReceipt', () => {
  it('shows “Seen” and reads the full list of names', async () => {
    render(<ReadReceipt data-testid="r" readers={readers.slice(0, 1)} />);
    const root = screen.getByTestId('r');
    expect(root).toHaveClass('mn-read-receipt');
    expect(root).toHaveAttribute('data-display', 'text');
    expect(root).toHaveAttribute('data-align', 'end');
    expect(root).toHaveAttribute('title', 'Seen by Ana Lopez');
    expect(root.querySelector('.mn-read-receipt__visual')).toHaveAttribute('aria-hidden');
    expect(screen.getByText('Seen by Ana Lopez')).toHaveClass('mn-visually-hidden');
    await expectNoA11yViolations();
  });

  it('shows a count for groups and joins names', () => {
    render(<ReadReceipt data-testid="r" readers={readers} display="count" />);
    expect(screen.getByText('Seen by 3')).toBeInTheDocument();
    expect(screen.getByText('Seen by Ana Lopez, Ben Okafor and Chloé Martin')).toBeInTheDocument();
  });

  it('renders nothing without readers', () => {
    const { container } = render(<ReadReceipt readers={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('shows avatars with an overflow count', async () => {
    const many = [...readers, { name: 'Dev Patel' }, { name: 'Eve Stone' }];
    render(<ReadReceipt data-testid="r" readers={many} display="avatars" maxAvatars={2} />);
    const root = screen.getByTestId('r');
    expect(root.querySelectorAll('.mn-avatar')).toHaveLength(2);
    expect(root).toHaveTextContent('+3');
    await expectNoA11yViolations();
  });

  it('shows the avatar instead of a “+1” tile', () => {
    render(<ReadReceipt data-testid="r" readers={readers} display="avatars" maxAvatars={2} />);
    expect(screen.getByTestId('r').querySelectorAll('.mn-avatar')).toHaveLength(3);
  });

  it('becomes a keyboard button with onShowReaders', async () => {
    const onShowReaders = vi.fn();
    render(<ReadReceipt readers={readers} display="count" onShowReaders={onShowReaders} />);
    const button = screen.getByRole('button', {
      name: 'Seen by Ana Lopez, Ben Okafor and Chloé Martin',
    });
    await userEvent.tab();
    expect(button).toHaveFocus();
    await userEvent.keyboard('{Enter}');
    expect(onShowReaders).toHaveBeenCalledTimes(1);
    await expectNoA11yViolations();
  });

  it('takes translated text and start alignment', () => {
    render(
      <ReadReceipt
        data-testid="r"
        readers={readers.slice(0, 2)}
        align="start"
        messages={{ seen: 'Visto', seenBy: (n) => `Visto por ${n}`, list: (n) => n.join(' y ') }}
      />,
    );
    expect(screen.getByText('Visto')).toBeInTheDocument();
    expect(screen.getByText('Visto por Ana Lopez y Ben Okafor')).toBeInTheDocument();
    expect(screen.getByTestId('r')).toHaveAttribute('data-align', 'start');
  });

  it('renders on the server', () => {
    const html = renderToString(<ReadReceipt readers={readers} display="avatars" />);
    expect(html).toContain('mn-read-receipt');
    expect(html).toContain('Seen by Ana Lopez');
  });
});
