/**
 * The web side of the cross-platform scenario fixtures (CROSS-PLATFORM.md section 8, item 3).
 *
 * Every `src/**\/<id>.fixture.json` holds the scenario of the web primary example of `<id>`: the
 * first item of `sections.examples` in `<id>.docs.tsx`. This test renders that example and checks
 * that what a person sees is the fixture: every text of `texts` in that order, the `data-tone`
 * values in DOM order equal to `tones`, and the buttons equal to `actions` (label, variant, size).
 * iOS and Android render the same fixture (scripts/native/fixtures.mjs), so the three agree.
 */
import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import type { ReactNode } from 'react';
import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

interface Fixture {
  id: string;
  example: string;
  texts: string[];
  tones?: string[];
  actions?: { label: string; variant?: string; size?: string }[];
  docsHarness?: string[];
}

const src = join(dirname(fileURLToPath(import.meta.url)), '..', 'src');

function fixtureFiles(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return fixtureFiles(path);
    return entry.name.endsWith('.fixture.json') ? [path] : [];
  });
}

const files = fixtureFiles(src).sort();

describe('parity fixtures', () => {
  it('there is at least one fixture', () => {
    expect(files.length).toBeGreaterThan(0);
  });

  for (const file of files) {
    const fixture = JSON.parse(readFileSync(file, 'utf8')) as Fixture;
    const where = relative(src, file);

    it(`${fixture.id}: the web primary example shows the fixture (${where})`, async () => {
      const docsFile = file.replace(/\.fixture\.json$/, '.docs.tsx');
      const docs = (await import(docsFile)) as {
        default: {
          id: string;
          sections: { examples: { items: { title: string; render: () => ReactNode }[] } };
        };
      };
      expect(docs.default.id).toBe(fixture.id);
      const [primary] = docs.default.sections.examples.items;
      expect(primary?.title, 'the fixture is the first example').toBe(fixture.example);

      const { container } = render(<>{primary?.render()}</>);

      // Texts, in reading order.
      const seen = (container.textContent ?? '').replace(/\s+/g, ' ');
      let from = 0;
      for (const text of fixture.texts) {
        const at = seen.indexOf(text, from);
        expect(at, `"${text}" after position ${from} in:\n${seen}`).toBeGreaterThanOrEqual(0);
        from = at + text.length;
      }

      // Tones, in DOM order.
      if (fixture.tones) {
        const tones = [...container.querySelectorAll('[data-tone]')].map((el) =>
          el.getAttribute('data-tone'),
        );
        expect(tones).toEqual(fixture.tones);
      }

      // Actions: every button, with its variant and size.
      if (fixture.actions) {
        const buttons = [...container.querySelectorAll('button')].map((b) => ({
          label: (b.textContent ?? '').trim(),
          variant: b.getAttribute('data-variant') ?? undefined,
          size: b.getAttribute('data-size') ?? undefined,
        }));
        expect(buttons).toEqual(
          fixture.actions.map((a) => ({ label: a.label, variant: a.variant, size: a.size })),
        );
      }
    });
  }
});
