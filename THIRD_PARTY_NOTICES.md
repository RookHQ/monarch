# Third-party notices

Monarch is licensed under the MIT License (see [LICENSE](LICENSE)). It uses the
third-party software and assets below. Every runtime dependency of the
published packages and of the documentation site is under an MIT-compatible
license (MIT or 0BSD); no GPL, LGPL or AGPL code is used or shipped.

The full license text of each npm dependency is included in its own package
when installed (`node_modules/<name>/LICENSE`). Copyright holders are listed
below as declared by each package.

## npm runtime dependencies

These are the dependencies of `@rookcom/monarch-react` and of the
documentation site (`apps/docs`), including their transitive dependencies.
`@rookcom/monarch-tokens` and `@rookcom/monarch-conformance` have no runtime
dependencies.

| License | Copyright holder (as declared)                 | Packages                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| ------- | ---------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 0BSD    | Microsoft Corporation.                         | `tslib`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| MIT     | (see package)                                  | `react-remove-scroll-bar`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| MIT     | 2017 Anton Korzunov                            | `aria-hidden`, `react-remove-scroll`, `react-style-singleton`, `use-callback-ref`, `use-sidecar`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| MIT     | 2017 Ilya Kantor                               | `detect-node-es`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| MIT     | 2018+, MarkedJS (https://github.com/markedjs/) | `marked`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| MIT     | 2020 Anton Korzunov                            | `get-nonce`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| MIT     | 2021-present Floating UI contributors          | `@floating-ui/core`, `@floating-ui/dom`, `@floating-ui/react-dom`, `@floating-ui/utils`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| MIT     | 2022 WorkOS                                    | `@radix-ui/number`, `@radix-ui/primitive`, `@radix-ui/react-accordion`, `@radix-ui/react-arrow`, `@radix-ui/react-checkbox`, `@radix-ui/react-collapsible`, `@radix-ui/react-collection`, `@radix-ui/react-compose-refs`, `@radix-ui/react-context`, `@radix-ui/react-dialog`, `@radix-ui/react-direction`, `@radix-ui/react-dismissable-layer`, `@radix-ui/react-dropdown-menu`, `@radix-ui/react-focus-guards`, `@radix-ui/react-focus-scope`, `@radix-ui/react-id`, `@radix-ui/react-menu`, `@radix-ui/react-popover`, `@radix-ui/react-popper`, `@radix-ui/react-portal`, `@radix-ui/react-presence`, `@radix-ui/react-primitive`, `@radix-ui/react-progress`, `@radix-ui/react-roving-focus`, `@radix-ui/react-select`, `@radix-ui/react-separator`, `@radix-ui/react-slot`, `@radix-ui/react-switch`, `@radix-ui/react-tabs`, `@radix-ui/react-toast`, `@radix-ui/react-tooltip`, `@radix-ui/react-use-callback-ref`, `@radix-ui/react-use-controllable-state`, `@radix-ui/react-use-effect-event`, `@radix-ui/react-use-is-hydrated`, `@radix-ui/react-use-layout-effect`, `@radix-ui/react-use-rect`, `@radix-ui/react-use-size`, `@radix-ui/react-visually-hidden`, `@radix-ui/rect` |
| MIT     | Meta Platforms, Inc. and affiliates.           | `react`, `react-dom`, `scheduler`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |

`tslib` is licensed under the BSD Zero Clause License (0BSD), which places no
conditions on use. Packages marked "(see package)" declare the MIT license
without a copyright line in a LICENSE file.

## Editor entry (Tiptap, ProseMirror)

The optional `./editor` entry of `@rookcom/monarch-react` is built on Tiptap
and ProseMirror. They are optional peer dependencies installed by the
consuming application and are not bundled into the Monarch packages. The
documentation site does bundle them to run the editor examples. That tree
(Tiptap, ProseMirror, markdown-it, linkifyjs, tippy.js and their dependencies)
is MIT licensed, with one exception: `entities` (pulled in by `markdown-it`) is
BSD-2-Clause, Copyright (c) Felix Boehm. Its license text:

> Copyright (c) Felix Böhm. All rights reserved.
>
> Redistribution and use in source and binary forms, with or without
> modification, are permitted provided that the following conditions are met:
>
> Redistributions of source code must retain the above copyright notice, this
> list of conditions and the following disclaimer.
>
> Redistributions in binary form must reproduce the above copyright notice, this
> list of conditions and the following disclaimer in the documentation and/or
> other materials provided with the distribution.
>
> THIS IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
> EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
> WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
> DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
> FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
> DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
> SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
> CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
> OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
> OF THIS, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Icons

The `Bot` glyph used in the documentation site's examples
(`apps/docs/src/marketing/icons.tsx`) follows the geometry of the `Bot` icon
from [Lucide](https://lucide.dev), which is licensed under the ISC License:

> ISC License
>
> Copyright (c) for portions of Lucide are held by Cole Bemis 2013-2022 as part
> of Feather (MIT). All other copyright (c) for Lucide are held by Lucide
> Contributors 2022.
>
> Permission to use, copy, modify, and/or distribute this software for any
> purpose with or without fee is hereby granted, provided that the above
> copyright notice and this permission notice appear in all copies.
>
> THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
> REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
> AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
> INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
> LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
> OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
> PERFORMANCE OF THIS SOFTWARE.

Monarch itself ships no icon set; applications bring their own.

## Fonts

The npm packages bundle no font files; the token packages only reference
Poppins by name in a CSS font stack.

The documentation site self-hosts **Poppins** (regular, medium, semibold and
bold; latin and latin-ext subsets, `woff2`) in `apps/docs/src/fonts`, and its
build ships the licence next to the files as `assets/Poppins-OFL.txt`.
Poppins is Copyright 2020 The Poppins Project Authors
(<https://github.com/itfoundry/Poppins>) and is licensed under the SIL Open
Font License, Version 1.1. The full licence text, also in
`apps/docs/src/fonts/OFL.txt`:

```text
Copyright 2020 The Poppins Project Authors (https://github.com/itfoundry/Poppins)

This Font Software is licensed under the SIL Open Font License, Version 1.1.
This license is copied below, and is also available with a FAQ at:
http://scripts.sil.org/OFL


-----------------------------------------------------------
SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007
-----------------------------------------------------------

PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide
development of collaborative font projects, to support the font creation
efforts of academic and linguistic communities, and to provide a free and
open framework in which fonts may be shared and improved in partnership
with others.

The OFL allows the licensed fonts to be used, studied, modified and
redistributed freely as long as they are not sold by themselves. The
fonts, including any derivative works, can be bundled, embedded,
redistributed and/or sold with any software provided that any reserved
names are not used by derivative works. The fonts and derivatives,
however, cannot be released under any other type of license. The
requirement for fonts to remain under this license does not apply
to any document created using the fonts or their derivatives.

DEFINITIONS
"Font Software" refers to the set of files released by the Copyright
Holder(s) under this license and clearly marked as such. This may
include source files, build scripts and documentation.

"Reserved Font Name" refers to any names specified as such after the
copyright statement(s).

"Original Version" refers to the collection of Font Software components as
distributed by the Copyright Holder(s).

"Modified Version" refers to any derivative made by adding to, deleting,
or substituting -- in part or in whole -- any of the components of the
Original Version, by changing formats or by porting the Font Software to a
new environment.

"Author" refers to any designer, engineer, programmer, technical
writer or other person who contributed to the Font Software.

PERMISSION & CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining
a copy of the Font Software, to use, study, copy, merge, embed, modify,
redistribute, and sell modified and unmodified copies of the Font
Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components,
in Original or Modified Versions, may be sold by itself.

2) Original or Modified Versions of the Font Software may be bundled,
redistributed and/or sold with any software, provided that each copy
contains the above copyright notice and this license. These can be
included either as stand-alone text files, human-readable headers or
in the appropriate machine-readable metadata fields within text or
binary files as long as those fields can be easily viewed by the user.

3) No Modified Version of the Font Software may use the Reserved Font
Name(s) unless explicit written permission is granted by the corresponding
Copyright Holder. This restriction only applies to the primary font name as
presented to the users.

4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font
Software shall not be used to promote, endorse or advertise any
Modified Version, except to acknowledge the contribution(s) of the
Copyright Holder(s) and the Author(s) or with their explicit written
permission.

5) The Font Software, modified or unmodified, in part or in whole,
must be distributed entirely under this license, and must not be
distributed under any other license. The requirement for fonts to
remain under this license does not apply to any document created
using the Font Software.

TERMINATION
This license becomes null and void if any of the above conditions are
not met.

DISCLAIMER
THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE
COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
INCLUDING ANY GENERAL, SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL
DAMAGES, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR FROM
OTHER DEALINGS IN THE FONT SOFTWARE.
```

## Development-only dependencies

Test and build tooling (for example `axe-core`, MPL-2.0, used only in tests)
is installed for development and is not included in the published packages or
the documentation site bundle.
