# Monarch and Figma

Monarch does not publish a Figma library today. What it ships is two things you can use with a
Figma library of your own:

- the design tokens as Figma variables (`@rookcom/monarch-tokens/figma-variables.json`);
- a generator, `monarch figma-connect`, that writes
  [Code Connect](https://github.com/figma/code-connect) files so Figma's Dev Mode shows real
  Monarch code next to your components.

You need your own Figma file with your own components, and the Figma CLI to publish. The
generated files are a starting point to review, not a finished mapping.

## 1. Import the variables

`@rookcom/monarch-tokens/figma-variables.json` is the request body for Figma's REST endpoint
`POST /v1/files/:file_key/variables`. It creates two collections:

- `Primitives` (one mode, `Value`): every primitive colour, plus space, radius and control
  heights as numbers in px. Hidden from publishing.
- `Semantic` (modes `Light`, `Dark`, `HC Light`, `HC Dark`): every semantic colour, aliased to
  the primitives.

Post it once, into a file that does not have these variables yet (the body only creates):

```sh
curl -X POST "https://api.figma.com/v1/files/<FILE_KEY>/variables" \
  -H "X-Figma-Token: $FIGMA_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  --data @node_modules/@rookcom/monarch-tokens/dist/figma-variables.json
```

Figma limits who may write variables through the REST API, by plan and by token scope. Check
Figma's current requirements before you start.

The file is not in a plugin's format. If you cannot use the REST API, the same tokens ship in
W3C DTCG form (`@rookcom/monarch-tokens/tokens.dtcg.json` and `@rookcom/monarch-tokens/dtcg/*`),
which you can bring in with a variables import plugin that reads DTCG. We have not tested any
plugin, so we do not name one.

## 2. Build or map your Figma components

Build your components in Figma with the variables, or use the ones you have. The generator
guesses each Figma property name from the React prop name in Title Case (`leadingIcon` becomes
`Leading Icon`) and each option label from the value (`danger-subtle` becomes `Danger Subtle`).
Naming your Figma properties that way saves work; where they differ, say so in the map (step 3).

## 3. Write the map

Copy `figma/map.example.json` to `figma/map.json` and replace every URL with a real one. In
Figma, select the component and choose "Copy link to selection". The link must contain a
`node-id`.

```json
{
  "components": [
    {
      "component": "Button",
      "url": "https://www.figma.com/design/<FILE_KEY>/<FILE_NAME>?node-id=<NODE_ID>",
      "props": { "leadingIcon": "Icon Left" },
      "values": { "variant": { "danger-subtle": "Danger (subtle)" } }
    }
  ]
}
```

- `component`: the Monarch component name, as exported.
- `url`: the Figma link to that component.
- `props` (optional): React prop name to Figma property name, where the guess is wrong.
- `values` (optional): per prop, value to Figma option label, where the guess is wrong.

The example file is rejected on purpose: placeholder URLs stop the command, so no file is ever
written with a made-up node.

## 4. Generate the Code Connect files

```sh
npx monarch figma-connect --map figma/map.json --out figma/
```

| Flag                | What it does                                                     |
| ------------------- | ---------------------------------------------------------------- |
| `--map <file.json>` | The map from step 3.                                             |
| `--out <dir>`       | Where the files go. Default `figma`.                             |
| `--force`           | Overwrite files that exist. Without it, existing files are kept. |
| `--root <dir>`      | The app folder, used to find the installed manifest.             |
| `--manifest <file>` | Read this component manifest instead of the installed one.       |

It writes one `<Component>.figma.tsx` per entry and lists what it skipped and why (a name that
is not in the manifest, a hook or utility, a file that exists).

What is mapped:

| React prop                   | Code Connect                |
| ---------------------------- | --------------------------- |
| A fixed set of string values | `figma.enum('Name', { … })` |
| `boolean`                    | `figma.boolean('Name')`     |
| `string`                     | `figma.string('Name')`      |
| `children` (React nodes)     | `figma.children('*')`       |

Everything else is left out: functions and event handlers, objects, arrays, numbers, refs,
`className`, `style`, `id`, `aria-*`, and node props other than `children` (icons and slots).
Add those by hand where your Figma component has a matching property, for example with
`figma.instance`. Remove any line whose property your Figma component does not have: the
Figma CLI reports properties it cannot find.

## 5. Publish

```sh
npm install --save-dev @figma/code-connect
FIGMA_ACCESS_TOKEN=<your token> npx figma connect publish
```

The Figma CLI reads `figma.config.json` at the repository root, which includes
`figma/**/*.figma.tsx`. Copy that file into your app if you run the command there. Keep the
token out of the repository.

Code Connect needs a Figma plan that includes Dev Mode, and a personal access token with the
scopes Figma lists for Code Connect. Check Figma's current plan requirements.

## What to expect

- The generated files have not been checked against any Figma file. Review each one.
- Run the command again after a Monarch upgrade to pick up new props; use `--force` only for
  files you have not edited by hand.
- This repository does not contain generated `*.figma.tsx` files, because it has no Figma
  library to point them at.
