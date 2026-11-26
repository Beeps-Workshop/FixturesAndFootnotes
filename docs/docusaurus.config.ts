import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

// This runs in Node.js - Don't use client-side code here (browser APIs, JSX...)

// Docs for Fixtures & Footnotes, served at workshop.beepsterr.com/fnf/. The workshop site
// (Beeps-Workshop/docs) builds this folder as its own Docusaurus instance with WORKSHOP_SHARED set to
// its shared/ folder, which adds the workshop's navbar, footer and styles. Without it (npm start
// here) the docs get a plain navbar and footer.
const shared = process.env.WORKSHOP_SHARED;

// The project's own navbar items. The workshop renders them as pixel buttons on the right; standalone
// they are plain navbar items.
const right = [
  {label: 'download', href: 'https://modrinth.com/mod/fixtures-and-footnotes'},
  {label: 'Modrinth', href: 'https://modrinth.com/mod/fixtures-and-footnotes'},
  {label: 'CurseForge', href: 'https://www.curseforge.com/minecraft/mc-mods/fixtures-footnotes'},
];

let withWorkshop: (config: Config, opts: {projectId?: string; right?: typeof right}) => Config = (config, opts) => ({
  ...config,
  url: 'https://workshop.beepsterr.com',
  baseUrl: process.env.DOCS_BASE_URL ?? '/',
  themeConfig: {
    ...config.themeConfig,
    navbar: {
      title: 'Fixtures & Footnotes',
      items: (opts.right ?? []).map((item) => ({...item, position: 'right' as const})),
    },
    footer: {style: 'dark', copyright: `Copyright © ${new Date().getFullYear()} Beeps' Workshop.`},
  } satisfies Preset.ThemeConfig,
});
if (shared) {
  ({withWorkshop} = require(shared));
}

export default withWorkshop(
  {
    // Page titles end in the workshop's name, like the rest of the site.
    title: "Beeps' Workshop",
    favicon: undefined,

    future: {v4: true},

    // Required by the type; withWorkshop sets both (baseUrl from DOCS_BASE_URL).
    url: 'https://workshop.beepsterr.com',
    baseUrl: '/',

    onBrokenLinks: 'throw',

    i18n: {defaultLocale: 'en', locales: ['en']},

    presets: [
      [
        'classic',
        {
          docs: {
            routeBasePath: '/',
            sidebarPath: './sidebars.ts',
            // Points at the real file, whether it's the current version (docs/...) or a frozen one
            // (versioned_docs/version-x/...).
            editUrl: ({versionDocsDirPath, docPath}) =>
              `https://github.com/Beeps-Workshop/FixturesAndFootnotes/edit/main/docs/${versionDocsDirPath}/${docPath}`,
          },
          blog: false,
        } satisfies Preset.Options,
      ],
    ],
  },
  {projectId: 'fnf', right},
);
