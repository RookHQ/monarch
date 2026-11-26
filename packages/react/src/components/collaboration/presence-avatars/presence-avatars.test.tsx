import { describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { PresenceAvatars, type PresencePerson } from './index.js';

const PEOPLE: PresencePerson[] = [
  { id: 'u1', name: 'Ada Lovelace', activity: 'Editing' },
  { id: 'u2', name: 'Grace Hopper', status: 'away' },
  { id: 'u3', name: 'Alan Turing', status: 'dnd', customStatus: { text: 'Focusing' } },
  { id: 'u4', name: 'Katherine Johnson', status: 'busy' },
  { id: 'u5', name: 'Edsger Dijkstra' },
  { id: 'u6', name: 'Barbara Liskov', status: 'offline' },
];

describe('PresenceAvatars', () => {
  it('names the group with the count and each person with presence and activity', async () => {
    const { container } = render(<PresenceAvatars people={PEOPLE.slice(0, 3)} />);
    const group = screen.getByRole('group', { name: '3 people here' });
    expect(group).toHaveClass('mn-presence-avatars');
    expect(screen.getByRole('img', { name: 'Ada Lovelace, Online, Editing' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Grace Hopper, Away' })).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: 'Alan Turing, Do not disturb · Focusing' }),
    ).toBeInTheDocument();
    expect(container.querySelectorAll('.mn-presence-dot')).toHaveLength(3);
    expect(container.querySelector('.mn-presence-dot')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.queryByRole('button')).toBeNull();
    await expectNoA11yViolations();
  });

  it('collapses past max into a +N button that lists everyone', async () => {
    render(<PresenceAvatars people={PEOPLE} max={3} />);
    expect(screen.getAllByRole('img', { name: /, / })).toHaveLength(3);
    const more = screen.getByRole('button', { name: 'Show 3 more' });
    expect(more).toHaveTextContent('+3');
    const user = userEvent.setup();
    await user.click(more);
    const dialog = await screen.findByRole('dialog', { name: 'Everyone here' });
    const list = within(dialog).getByRole('list', { name: 'Everyone here' });
    expect(within(list).getAllByRole('listitem')).toHaveLength(6);
    expect(list).toHaveTextContent('Barbara LiskovOffline');
    expect(list).toHaveTextContent('Ada LovelaceOnline · Editing');
    await expectNoA11yViolations();
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).toBeNull();
    expect(more).toHaveFocus();
  });

  it('shows the last person instead of a +1', () => {
    render(<PresenceAvatars people={PEOPLE.slice(0, 5)} max={4} />);
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getAllByRole('img', { name: /, / })).toHaveLength(5);
  });

  it('makes people buttons with onPersonClick, in the stack and the list', async () => {
    const onPersonClick = vi.fn();
    render(<PresenceAvatars people={PEOPLE} max={2} onPersonClick={onPersonClick} />);
    const user = userEvent.setup();
    await user.tab();
    // A face button's name contains the initials it shows (WCAG 2.5.3); the tooltip does not.
    const ada = screen.getByRole('button', { name: 'Ada Lovelace, AL, Online, Editing' });
    expect(ada).toHaveFocus();
    expect(ada).toHaveAttribute('title', 'Ada Lovelace, Online, Editing');
    await user.keyboard('{Enter}');
    expect(onPersonClick).toHaveBeenLastCalledWith(PEOPLE[0]);
    await user.tab();
    await user.tab();
    expect(screen.getByRole('button', { name: 'Show 4 more' })).toHaveFocus();
    await user.keyboard('{Enter}');
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: /Barbara Liskov/ }));
    expect(onPersonClick).toHaveBeenLastCalledWith(PEOPLE[5]);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('leaves initials out of a face button with a picture', () => {
    render(
      <PresenceAvatars
        people={[{ id: 'p', name: 'Ada Lovelace', avatarSrc: 'data:image/png;base64,' }]}
        onPersonClick={() => {}}
      />,
    );
    expect(screen.getByRole('button', { name: 'Ada Lovelace, Online' })).toBeInTheDocument();
  });

  it('translates its text', () => {
    render(
      <PresenceAvatars
        people={PEOPLE.slice(0, 1)}
        size="md"
        messages={{ label: (n) => `${n} hier`, person: (name, p) => `${name} (${p})` }}
        presenceMessages={{ online: 'Online jetzt' }}
      />,
    );
    const group = screen.getByRole('group', { name: '1 hier' });
    expect(group).toHaveAttribute('data-size', 'md');
    expect(screen.getByRole('img', { name: 'Ada Lovelace (Online jetzt)' })).toBeInTheDocument();
  });

  it('renders on the server', () => {
    const html = renderToString(<PresenceAvatars people={PEOPLE} />);
    expect(html).toContain('mn-presence-avatars');
    expect(html).toContain('+2');
  });
});
