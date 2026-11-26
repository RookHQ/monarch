import monarch from '@rookcom/monarch-conformance/eslint';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['.next/**', 'node_modules/**', 'next-env.d.ts'] },
  ...tseslint.configs.recommended,
  monarch.configs.general,
);
