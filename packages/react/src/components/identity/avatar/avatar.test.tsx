import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { contrast, hsl, themes, toRgb } from '@rookcom/monarch-tokens';
import userEvent from '@testing-library/user-event';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Avatar, AvatarGroup, avatarPaletteIndex, initials } from './index.js';

/** sRGB mix, as CSS `color-mix(in srgb, a p%, b)` computes it. */
function mix(a: [number, number, number], b: [number, number, number], p: number) {
  return a.map((v, i) => v * p + (b[i] ?? 0) * (1 - p)) as [number, number, number];
}
function lum([r, g, b]: [number, number, number]) {
  const f = (c: number) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4);
  return 0.2126 * f(r) + 0.7152 * f(g) + 0.0722 * f(b);
}
function ratio(a: [number, number, number], b: [number, number, number]) {
  const [x, y] = [lum(a), lum(b)].sort((m, n) => n - m) as [number, number];
  return (x + 0.05) / (y + 0.05);
}

describe('Avatar', () => {
  it('computes initials', () => {
    expect(initials('Ada Lovelace')).toBe('AL');
    expect(initials('  cher ')).toBe('C');
    expect(initials('')).toBe('?');
  });

  it('falls back to initials when the image fails', () => {
    const { container } = render(<Avatar name="Grace Hopper" src="/missing.png" />);
    expect(screen.getByRole('img', { name: 'Grace Hopper' })).toBeInTheDocument();
    fireEvent.error(container.querySelector('img')!);
    expect(screen.getByText('GH')).toBeInTheDocument();
  });

  it('fallback colours keep AA contrast for every hue', () => {
    // Mirrors the hsl() values in src/components/identity/avatar/avatar.css.
    for (let h = 0; h < 360; h++) {
      expect(contrast(hsl(h, 55, 28), hsl(h, 45, 92)), `light hue ${h}`).toBeGreaterThanOrEqual(
        4.5,
      );
      expect(contrast(hsl(h, 60, 85), hsl(h, 30, 22)), `dark hue ${h}`).toBeGreaterThanOrEqual(4.5);
    }
  });

  it('has no axe violations, and decorative avatars are hidden', async () => {
    const { container } = render(
      <main>
        <Avatar name="Ada Lovelace" />
        <Avatar name="Grace Hopper" decorative />
      </main>,
    );
    expect(container.querySelectorAll('.mn-avatar')[1]).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Avatar name="Ada Lovelace" />);
    expect(html).toContain('mn-avatar');
  });

  it('keeps the 0.3 markup by default', () => {
    const { container } = render(<Avatar name="Ada Lovelace" />);
    const el = container.firstElementChild!;
    expect(el.getAttributeNames().sort()).toEqual(
      ['aria-label', 'class', 'data-shape', 'data-size', 'role', 'style'].sort(),
    );
    expect(el.getAttribute('style')).toMatch(/^--mn-avatar-hue: \d+;?$/);
  });

  it('accepts a numeric size in pixels', () => {
    const { container } = render(<Avatar name="Ada Lovelace" size={28} />);
    const el = container.firstElementChild as HTMLElement;
    expect(el).toHaveAttribute('data-size', 'custom');
    expect(el.style.getPropertyValue('--mn-avatar-size')).toBe('28px');
  });

  it('derives the colour from seed, not name, when given', () => {
    const a = render(<Avatar name="Ada" seed="user_1" />).container
      .firstElementChild as HTMLElement;
    const b = render(<Avatar name="Ada L." seed="user_1" />).container
      .firstElementChild as HTMLElement;
    expect(a.style.getPropertyValue('--mn-avatar-hue')).toBe(
      b.style.getPropertyValue('--mn-avatar-hue'),
    );
  });

  it('palette="tokens" maps a seed to one of the five data tokens, deterministically', () => {
    const { container } = render(<Avatar name="Grace Hopper" palette="tokens" />);
    const el = container.firstElementChild as HTMLElement;
    const index = avatarPaletteIndex('Grace Hopper');
    expect(index).toBeGreaterThanOrEqual(1);
    expect(index).toBeLessThanOrEqual(5);
    expect(el).toHaveAttribute('data-palette', String(index));
    expect(el.style.getPropertyValue('--mn-avatar-hue')).toBe('');
    const seen = new Set(
      ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'].map(avatarPaletteIndex),
    );
    expect(seen.size).toBeGreaterThan(2);
  });

  it('palette="tokens" colours keep AA contrast in light and dark', () => {
    // Mirrors avatar.css (16% tint on surface, 40% tint into default text) and the data tokens.
    const data = { light: 49, dark: 68 } as const;
    const hues: Array<[number, number]> = [
      [214, 90],
      [24, 94],
      [256, 82],
      [330, 78],
      [280, 68],
    ];
    for (const id of ['light', 'dark'] as const) {
      const surface = toRgb(themes[id].neutrals['bg-surface']);
      const text = toRgb(themes[id].neutrals['text-default']);
      for (const [h, s] of hues) {
        const tint = toRgb(hsl(h, s, data[id]));
        const bg = mix(tint, surface, 0.16);
        const fg = mix(tint, text, 0.4);
        expect(ratio(fg, bg), `${id} hue ${h}`).toBeGreaterThanOrEqual(4.5);
      }
    }
  });

  it('falls back from image to initials to icon', () => {
    const { container, rerender } = render(<Avatar name="Rook HQ" src="/logo.png" />);
    fireEvent.error(container.querySelector('img')!);
    expect(screen.getByText('RH')).toBeInTheDocument();
    rerender(<Avatar name="Rook HQ" fallback="icon" shape="square" />);
    expect(container.querySelector('.mn-avatar__icon svg')).not.toBeNull();
    rerender(<Avatar name="  " />);
    expect(container.querySelector('.mn-avatar__icon')).not.toBeNull();
    rerender(<Avatar name="Rook HQ" fallback="icon" icon={<svg data-t="org" />} />);
    expect(container.querySelector('[data-t="org"]')).not.toBeNull();
  });

  it('retries the image when src changes after a failure', () => {
    const { container, rerender } = render(<Avatar name="Ada" src="/a.png" />);
    fireEvent.error(container.querySelector('img')!);
    expect(container.querySelector('img')).toBeNull();
    rerender(<Avatar name="Ada" src="/b.png" />);
    expect(container.querySelector('img')).toHaveAttribute('src', '/b.png');
  });

  it('adds the status text alternative to the accessible name', async () => {
    render(
      <main>
        <Avatar name="Ada Lovelace" status="online" />
        <Avatar name="Grace Hopper" status="busy" statusLabel="Beschäftigt" />
      </main>,
    );
    expect(screen.getByRole('img', { name: 'Ada Lovelace (Online)' })).toHaveAttribute(
      'data-status',
      'online',
    );
    expect(screen.getByRole('img', { name: 'Grace Hopper (Beschäftigt)' })).toBeInTheDocument();
    await expectNoA11yViolations();
  });

  it('forwards the ref', () => {
    let node: HTMLSpanElement | null = null;
    render(
      <Avatar
        ref={(n) => {
          node = n;
        }}
        name="Ada"
      />,
    );
    expect(node).toHaveClass('mn-avatar');
  });
});

describe('AvatarGroup', () => {
  const people = [
    'Ada Lovelace',
    'Grace Hopper',
    'Katherine Johnson',
    'Margaret Hamilton',
    'Hedy Lamarr',
  ];

  it('is a labelled group that shows max avatars and a +N tile', async () => {
    render(
      <main>
        <AvatarGroup label="Assignees" max={3} size="sm">
          {people.map((p) => (
            <Avatar key={p} name={p} />
          ))}
        </AvatarGroup>
      </main>,
    );
    const group = screen.getByRole('group', { name: 'Assignees' });
    expect(group.querySelectorAll('.mn-avatar:not(.mn-avatar-group__overflow)')).toHaveLength(3);
    expect(screen.getByRole('img', { name: '2 more' })).toHaveTextContent('+2');
    expect(screen.getByRole('img', { name: 'Ada Lovelace' })).toHaveAttribute('data-size', 'sm');
    await expectNoA11yViolations();
  });

  it('shows the last avatar instead of a +1 tile', () => {
    render(
      <AvatarGroup label="Reviewers" max={4}>
        {people.map((p) => (
          <Avatar key={p} name={p} />
        ))}
      </AvatarGroup>,
    );
    expect(screen.getAllByRole('img')).toHaveLength(5);
    expect(screen.queryByText('+1')).toBeNull();
  });

  it('keeps a child’s own size and uses a custom overflow label', () => {
    render(
      <AvatarGroup label="Team" max={1} size={24} overflowLabel={(n) => `und ${n} weitere`}>
        <Avatar name="Ada Lovelace" size="lg" />
        <Avatar name="Grace Hopper" />
        <Avatar name="Hedy Lamarr" />
      </AvatarGroup>,
    );
    expect(screen.getByRole('img', { name: 'Ada Lovelace' })).toHaveAttribute('data-size', 'lg');
    expect(screen.getByRole('img', { name: 'und 2 weitere' })).toHaveAttribute(
      'data-size',
      'custom',
    );
  });

  it('is not a tab stop (avatars are not interactive)', async () => {
    render(
      <>
        <AvatarGroup label="Team">
          <Avatar name="Ada" />
        </AvatarGroup>
        <button type="button">Next</button>
      </>,
    );
    await userEvent.setup().tab();
    expect(screen.getByRole('button', { name: 'Next' })).toHaveFocus();
  });

  it('renders on the server', () => {
    const html = renderToString(
      <AvatarGroup label="Team" max={1}>
        <Avatar name="Ada" />
        <Avatar name="Grace" />
        <Avatar name="Hedy" />
      </AvatarGroup>,
    );
    expect(html).toContain('mn-avatar-group');
    expect(html).toContain('+2');
  });
});
