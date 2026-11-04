/**
 * Renders the social share card (apps/docs/public/og.png, 1200x630) from
 * HTML, in the same dark theme as the launch page: the crown, the headline,
 * one quiet line. Run `npm run docs:og` after changing the design; the PNG is
 * committed so the site build needs no browser.
 */
import { readFileSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const root = fileURLToPath(new URL('../../', import.meta.url));
const fonts = (w) =>
  `data:font/woff2;base64,${readFileSync(`${root}apps/docs/src/fonts/poppins-${w}-latin.woff2`).toString('base64')}`;
const { chromium } = createRequire(import.meta.url)('@playwright/test');

const html = `<!doctype html><html><head><meta charset="utf-8"><style>
@font-face{font-family:Poppins;font-weight:400;src:url(${fonts(400)}) format('woff2')}
@font-face{font-family:Poppins;font-weight:500;src:url(${fonts(500)}) format('woff2')}
@font-face{font-family:Poppins;font-weight:700;src:url(${fonts(700)}) format('woff2')}
*{box-sizing:border-box;margin:0}
body{width:1200px;height:630px;position:relative;overflow:hidden;background:#050508;color:#fff;font-family:Poppins,sans-serif;
 background-image:radial-gradient(ellipse 55% 50% at 50% 44%,rgba(0,150,212,.20),rgba(220,46,62,.07) 55%,transparent 80%);
 display:flex;flex-direction:column;align-items:center;justify-content:center}
.edge{position:absolute;top:0;left:0;right:0;height:6px;display:flex}.edge i{flex:1}
h1{font-size:104px;font-weight:700;letter-spacing:-.04em;line-height:1}
p{margin-top:28px;font-size:34px;font-weight:400;color:rgba(255,255,255,.72);letter-spacing:-.01em}
svg{margin-bottom:44px}
.foot{position:absolute;left:56px;right:56px;bottom:44px;display:flex;justify-content:space-between;align-items:center;font-size:24px;font-weight:500;color:rgba(255,255,255,.6)}
.tag{padding:10px 24px;border-radius:999px;border:1px solid rgba(255,255,255,.22);color:#fff}
</style></head><body>
<div class="edge"><i style="background:#0096d4"></i><i style="background:#28af4c"></i><i style="background:#f4b400"></i><i style="background:#dc2e3e"></i></div>
<svg width="132" height="88" viewBox="0 0 120 80"><path d="M8 20l26 24L60 8l26 36 26-24-12 52H20z" fill="none" stroke="#fff" stroke-width="8" stroke-linejoin="round" stroke-linecap="round"/></svg>
<h1>Meet Monarch.</h1>
<p>An open-source design system.</p>
<div class="foot"><span>monarchui.dev</span><span class="tag">Open source · MIT</span></div>
</body></html>`;

const browser = await chromium.launch();
const page = await browser.newPage({
  viewport: { width: 1200, height: 630 },
  deviceScaleFactor: 1,
});
await page.setContent(html, { waitUntil: 'load' });
await page.evaluate(() => document.fonts.ready);
writeFileSync(`${root}apps/docs/public/og.png`, await page.screenshot({ type: 'png' }));
await browser.close();
console.log('Wrote apps/docs/public/og.png');
