// Writes app/brand.generated.css from the colour in brand.json.
// Runs before `npm run dev` and `npm run build`.
import { readFileSync, writeFileSync } from 'node:fs';
import { brandThemeCss, deriveTheme } from '@rookcom/monarch-tokens';

const OUT = new URL('../app/brand.generated.css', import.meta.url);
const NEUTRAL = '/* No brand colour in brand.json: the neutral accent is used. */\n';

const { accent } = JSON.parse(readFileSync(new URL('../brand.json', import.meta.url), 'utf8'));

function brandCss(colour) {
  const theme = deriveTheme(colour);
  if (!theme.valid)
    throw new Error(`brand.json: "accent" must be #rgb or #rrggbb, got "${colour}"`);
  return brandThemeCss(theme);
}

writeFileSync(OUT, accent ? brandCss(accent) : NEUTRAL);
