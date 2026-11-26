// Copies the root LICENSE and NOTICE into the package being packed, so every
// published package ships both, as section 4 of the Apache License 2.0
// requires. Runs from each package's `prepack` script (npm pack and npm
// publish); the copies are git-ignored.
import { copyFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
for (const file of ['LICENSE', 'NOTICE']) {
  copyFileSync(join(root, file), join(process.cwd(), file));
}
