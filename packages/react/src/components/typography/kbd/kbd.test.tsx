import { createRef } from 'react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Kbd } from './index.js';

describe('Kbd', () => {
  it('forwards its ref to the kbd element', () => {
    const ref = createRef<HTMLElement>();
    render(<Kbd ref={ref}>K</Kbd>);
    expect(ref.current?.tagName).toBe('KBD');
  });

  it('renders a kbd element accessibly', async () => {
    render(
      <main>
        <Kbd>⌘K</Kbd>
      </main>,
    );
    expect(screen.getByText('⌘K').tagName).toBe('KBD');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Kbd>K</Kbd>);
    expect(html).toContain('mn-kbd');
  });
});
