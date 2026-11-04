// Copies the root LICENSE into the package being packed, so every published
// package ships it. Runs from each package's `prepack` script (npm pack and
// npm publish); the copies are git-ignored.
import { copyFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
copyFileSync(join(root, 'LICENSE'), join(process.cwd(), 'LICENSE'));
