// Generates docs/architecture/CONTRAST-MATRIX.md: the measured contrast of every
// text/background pairing, every intent and the AI colours, and every product
// accent, in all four themes.
//
//   npx tsx scripts/tokens/contrast-matrix.ts           write it
//   npx tsx scripts/tokens/contrast-matrix.ts --check   fail if stale or any pair fails
//
// Targets come from each theme (text 4.5:1 standard, 7:1 high contrast; UI
// 3:1 and 4.5:1). `text-disabled` is exempt from WCAG but must stay
// distinguishable: at least 2.5:1.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import * as prettier from 'prettier';
import { contrast, relativeLuminance, type Hsl } from '../../packages/tokens/src/color.js';
import { defaultAccent } from '../../packages/tokens/src/primitives.js';
import {
  INTENT_IDS,
  THEME_IDS,
  resolveTheme,
  themes,
  type ThemeId,
} from '../../packages/tokens/src/themes.js';
import { products, productIds } from '../../packages/tokens/src/generated/products.js';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..');
const OUT = 'docs/architecture/CONTRAST-MATRIX.md';
const DISABLED_FLOOR = 2.5;

const TEXT = ['text-default', 'text-muted', 'text-subtle', 'text-disabled'] as const;
const BG = [
  'bg-canvas',
  'bg-surface',
  'bg-raised',
  'bg-subtle',
  'bg-muted',
  'bg-emphasis',
] as const;

const fmt = (n: number) => n.toFixed(2);
let failures = 0;
const failList: string[] = [];
const mark = (ratio: number, target: number, what: string) => {
  const ok = ratio + 1e-9 >= target;
  if (!ok) {
    failures++;
    failList.push(`${what}: ${fmt(ratio)} < ${target}`);
  }
  return `${fmt(ratio)}${ok ? '' : ' ✗'}`;
};

function table(headers: string[], rows: string[][]): string {
  return [
    `| ${headers.join(' | ')} |`,
    `| ${headers.map(() => '---').join(' | ')} |`,
    ...rows.map((r) => `| ${r.join(' | ')} |`),
  ].join('\n');
}

const sections: string[] = [];
for (const id of THEME_IDS) {
  const theme = themes[id];
  const n = theme.neutrals;
  const r = resolveTheme(theme, defaultAccent);
  const { text, ui } = theme.targets;
  sections.push(`## ${id} (text ${text}:1, UI ${ui}:1)`);

  // Text on backgrounds.
  sections.push(
    table(
      ['Text \\ background', ...BG.map((b) => `\`${b}\``)],
      TEXT.map((t) => [
        `\`${t}\``,
        ...BG.map((b) =>
          mark(
            contrast(n[t], n[b]),
            t === 'text-disabled'
              ? b === 'bg-emphasis'
                ? 1 // disabled text does not sit on emphasis fills
                : DISABLED_FLOOR
              : b === 'bg-emphasis' && t !== 'text-default'
                ? ui
                : text,
            `${id} ${t} on ${b}`,
          ),
        ),
      ]),
    ),
  );

  // Intents (including ai).
  sections.push(
    table(
      [
        'Intent',
        'on-solid / solid',
        'strong / canvas',
        'strong / surface',
        'strong / subtle',
        'solid / canvas (UI)',
      ],
      INTENT_IDS.map((i) => {
        const s = r.intents[i];
        return [
          `\`${i}\``,
          mark(contrast(s['on-solid'], s.solid), text, `${id} ${i} on-solid`),
          mark(contrast(s.strong, n['bg-canvas']), text, `${id} ${i} strong/canvas`),
          mark(contrast(s.strong, n['bg-surface']), text, `${id} ${i} strong/surface`),
          mark(contrast(s.strong, s.subtle), text, `${id} ${i} strong/subtle`),
          // Fills must be distinguishable from the page (WCAG 1.4.11 applies to
          // their boundaries; Monarch adds a border where a fill is the only cue).
          fmt(contrast(s.solid, n['bg-canvas'])),
        ];
      }),
    ),
  );

  // Elevation in dark themes: lighter surfaces, not heavier shadows.
  // High contrast is flat by design (borders carry structure), so only the
  // standard dark theme must step its surfaces lighter.
  if (theme.scheme === 'dark' && theme.contrast === 'standard') {
    const L = (c: Hsl) => relativeLuminance(c) * 100;
    const canvas = L(n['bg-canvas']);
    const surface = L(n['bg-surface']);
    const raised = L(n['bg-raised']);
    sections.push(
      `Elevation: relative luminance canvas ${fmt(canvas)} < surface ${fmt(surface)} < raised ${fmt(raised)}.`,
    );
    if (!(canvas < surface && surface < raised)) {
      failures++;
      failList.push(`${id}: dark elevation is not monotonic`);
    }
  }
}

// Product accents.
const accentRows: string[][] = [];
for (const pid of ['default', ...productIds]) {
  const accent: Hsl =
    pid === 'default' ? defaultAccent : products[pid as (typeof productIds)[number]].accent;
  const row = [`\`${pid}\``];
  for (const id of THEME_IDS) {
    const theme = themes[id];
    const s = resolveTheme(theme, accent).intents.accent;
    const t = theme.targets.text;
    row.push(
      `${mark(contrast(s['on-solid'], s.solid), t, `${pid} ${id} on-solid`)} / ${mark(
        contrast(s.strong, theme.neutrals['bg-canvas']),
        t,
        `${pid} ${id} strong`,
      )}`,
    );
  }
  accentRows.push(row);
}

const body = `# Contrast matrix

<!-- Generated by scripts/tokens/contrast-matrix.ts. Do not edit; run \`npx tsx scripts/tokens/contrast-matrix.ts\`. -->

Measured WCAG contrast ratios of Monarch's colour tokens in every theme. A
✗ marks a pair below its target. Targets: text ${themes.light.targets.text}:1
(standard) and ${themes['hc-light'].targets.text}:1 (high contrast); muted and
subtle text on \`bg-emphasis\` use the UI target; \`text-disabled\` is exempt
from WCAG but must reach ${DISABLED_FLOOR}:1 and is never the only state cue.

**${failures === 0 ? 'Every pair meets its target.' : `${failures} pair(s) below target.`}**

${sections.join('\n\n')}

## Product accents

\`on-solid / solid\` (text on a filled accent control) and \`strong / canvas\`
(accent text and links on the page), per theme.

${table(['Product', ...THEME_IDS.map((t: ThemeId) => t)], accentRows)}
`;

const options = (await prettier.resolveConfig(join(ROOT, OUT))) ?? {};
const text = await prettier.format(body, { ...options, filepath: join(ROOT, OUT) });
if (process.argv.includes('--check')) {
  const current = existsSync(join(ROOT, OUT)) ? readFileSync(join(ROOT, OUT), 'utf8') : '';
  if (current !== text) {
    console.error(`${OUT} is stale: run \`npx tsx scripts/tokens/contrast-matrix.ts\``);
    process.exit(1);
  }
  if (failures) {
    for (const f of failList) console.error(`error: ${f}`);
    process.exit(1);
  }
  console.log(`${OUT} is current; every pair meets its target`);
} else {
  writeFileSync(join(ROOT, OUT), text);
  console.log(`${OUT}: ${failures} failure(s)`);
  for (const f of failList) console.log(`  ${f}`);
}
