/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docsSidebar: [
    'intro',
    'getting-started',
    'configuration',
    {
      type: 'category',
      label: 'Core Guides',
      collapsed: false,
      items: [
        'guides/base-test',
        'guides/base-page',
        'guides/wait-engine',
        'guides/retry',
        'guides/screenshots',
        'guides/step-logging',
        'guides/browser-lifecycle',
        'guides/parallel',
      ],
    },
    {
      type: 'category',
      label: 'CI / CD',
      items: [
        'ci/github-actions',
        'ci/jenkins',
        'ci/quality-gates',
      ],
    },
    {
      type: 'category',
      label: 'Extensibility',
      items: [
        'extensibility/plugins',
        'extensibility/custom-drivers',
        'extensibility/hooks',
        'extensibility/report-adapters',
      ],
    },
    {
      type: 'category',
      label: 'Reporting',
      items: [
        'reporting/html-report',
        'reporting/junit-xml',
      ],
    },
    'junit5',
    'changelog',
  ],
};

module.exports = sidebars;
