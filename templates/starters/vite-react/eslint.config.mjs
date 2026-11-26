import monarch from '@rookcom/monarch-conformance/eslint';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist/**', 'node_modules/**'] },
  ...tseslint.configs.recommended,
  monarch.configs.general,
);
