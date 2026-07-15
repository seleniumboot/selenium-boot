// @ts-check
const { themes } = require('prism-react-renderer');

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Selenium Boot',
  tagline: 'The Spring Boot of Selenium — Playwright-inspired APIs, zero setup, without hiding Selenium',
  favicon: 'img/favicon.svg',

  url: 'https://seleniumboot.github.io',
  baseUrl: '/selenium-boot/',

  organizationName: 'seleniumboot',
  projectName: 'selenium-boot',
  deploymentBranch: 'gh-pages',
  trailingSlash: false,

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

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
