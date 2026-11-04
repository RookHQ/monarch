import { describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { renderToString } from 'react-dom/server';
import { expectNoA11yViolations } from '../../../../test/a11y.js';
import { Skeleton } from './index.js';

describe('Skeleton', () => {
  it('renders accessibly and is hidden from assistive technology', async () => {
    const { container } = render(
      <main>
        <Skeleton shape="text" width="8rem" />
      </main>,
    );
    expect(container.querySelector('.mn-skeleton')).toHaveAttribute('aria-hidden', 'true');
    await expectNoA11yViolations();
  });

  it('renders on the server', () => {
    const html = renderToString(<Skeleton />);
    expect(html).toContain('mn-skeleton');
  });
});
