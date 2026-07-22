// @ts-check
const { themes } = require('prism-react-renderer');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Selenium Boot',
  tagline: 'The Spring Boot of Selenium — Playwright-inspired APIs, zero setup, without hiding Selenium',
  favicon: 'img/favicon.svg',

  url: 'https://docs.seleniumboot.com',
  baseUrl: '/',

  organizationName: 'seleniumboot',
  projectName: 'selenium-boot',
  deploymentBranch: 'gh-pages',
  trailingSlash: false,

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  // GoatCounter — cookieless, no personal data, no consent banner (same account as
  // seleniumboot.com, see the site's /privacy page).
  //
  // Docs and the marketing site report into ONE GoatCounter site, so raw paths would
  // collide: the apex homepage and the docs homepage both count as "/". The first
  // script namespaces every docs hit under "/docs-site/..." so the two hostnames stay
  // separable in the dashboard. Order matters — settings must run before count.js.
  headTags: [
    {
      tagName: 'script',
      attributes: { type: 'text/javascript' },
      innerHTML:
        "window.goatcounter={path:function(p){return '/docs-site'+p;}};",
    },
    {
      tagName: 'script',
      attributes: {
        async: 'true',
        src: 'https://gc.zgo.at/count.js',
        'data-goatcounter': 'https://seleniumboot.goatcounter.com/count',
      },
    },
  ],

  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: 'https://github.com/seleniumboot/selenium-boot/edit/master/docs-site/',
        },
        blog: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
        // Explicit so sitemap generation can't be silently disabled.
        // Ships with preset-classic; emits build/sitemap.xml listing all pages.
        sitemap: {
          changefreq: 'weekly',
          priority: 0.5,
          filename: 'sitemap.xml',
        },
      }),
    ],
  ],

  // Offline, self-hosted search (no external service / account needed).
  themes: [
    [
      require.resolve('@easyops-cn/docusaurus-search-local'),
      {
        hashed: true,
        indexBlog: false,
        highlightSearchTermsOnTargetPage: true,
        explicitSearchResultPath: true,
      },
    ],
  ],

  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      image: 'img/selenium-boot-social.png',
      colorMode: {
        defaultMode: 'dark',
        disableSwitch: false,
        respectPrefersColorScheme: false,
      },
      navbar: {
        title: 'Selenium Boot',
        logo: {
          alt: 'Selenium Boot Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'docSidebar',
            sidebarId: 'docsSidebar',
            position: 'left',
            label: 'Docs',
          },
          {
            href: 'https://central.sonatype.com/artifact/io.github.seleniumboot/selenium-boot',
            label: 'Maven Central',
            position: 'right',
          },
          {
            href: 'https://github.com/seleniumboot/selenium-boot',
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              { label: 'Getting Started', to: '/docs/getting-started' },
              { label: 'Configuration', to: '/docs/configuration' },
              { label: 'Step Logging', to: '/docs/guides/step-logging' },
              { label: 'CI/CD', to: '/docs/ci/github-actions' },
            ],
          },
          {
            title: 'Community',
            items: [
              { label: 'GitHub Issues', href: 'https://github.com/seleniumboot/selenium-boot/issues' },
              { label: 'GitHub Discussions', href: 'https://github.com/seleniumboot/selenium-boot/discussions' },
            ],
          },
          {
            title: 'More',
            items: [
              { label: 'GitHub', href: 'https://github.com/seleniumboot/selenium-boot' },
              { label: 'Maven Central', href: 'https://central.sonatype.com/artifact/io.github.seleniumboot/selenium-boot' },
              { label: 'Changelog', to: '/docs/changelog' },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} Selenium Boot. Built with Docusaurus.`,
      },
      prism: {
        theme: themes.github,
        darkTheme: themes.dracula,
        additionalLanguages: ['java', 'yaml', 'bash', 'markup'],
      },
      algolia: undefined,
    }),
};

module.exports = config;
