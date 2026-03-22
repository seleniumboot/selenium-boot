import React from 'react';
import clsx from 'clsx';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import styles from './index.module.css';

const features = [
  {
    icon: '⚡',
    title: 'Zero Boilerplate',
    description: 'Extend BaseTest. Write @Test methods. Everything else — driver lifecycle, waits, retry, reports — is handled automatically.',
  },
  {
    icon: '📄',
    title: 'YAML Configuration',
    description: 'One selenium-boot.yml controls browser, parallel threads, timeouts, retry, CI thresholds, and more. Switch environments with a profile flag.',
  },
  {
    icon: '🔁',
    title: 'Smart Retry',
    description: 'retry.enabled: true retries every flaky test automatically. Use @Retryable for per-method control. Recovered vs still-failing shown in the report.',
  },
  {
    icon: '⏳',
    title: 'WaitEngine',
    description: 'Fluent explicit waits built in — waitForVisible, waitForClickable, waitForText, waitForPageLoad, and a custom condition escape hatch.',
  },
  {
    icon: '📊',
    title: 'Advanced HTML Report',
    description: 'Tabbed dashboard with pass rate, donut chart, slowest tests, step timeline, base64 screenshots, dark mode, and collapsible test groups.',
  },
  {
    icon: '🪜',
    title: 'Step Logging',
    description: 'StepLogger.step("name") logs named steps with timestamps. Optional per-step screenshots. Full timeline visible in the Failures tab.',
  },
  {
    icon: '🔌',
    title: 'Extensible',
    description: 'Java SPI-powered plugin system. Register custom browser providers, report adapters (Slack, Allure), and lifecycle hooks — zero framework changes.',
  },
  {
    icon: '🤖',
    title: 'CI-Ready',
    description: 'Auto-detects GitHub Actions, Jenkins, CircleCI, and more. Forces headless, adjusts thread count, emits JUnit XML. Includes ready-to-use workflow templates.',
  },
  {
    icon: '📋',
    title: 'Page Object Toolkit',
    description: 'BasePage provides wait-backed click, type, getText, isDisplayed. SmartLocator tries multiple strategies in order. Built-in iFrame helpers and file upload support.',
  },
  {
    icon: '🔐',
    title: '@PreCondition',
    description: 'Eliminate @BeforeMethod login boilerplate. Declare @PreCondition("loginAsAdmin") on a test — the framework runs the setup once, caches cookies + localStorage, and restores the session automatically.',
  },
];

function Feature({ icon, title, description }) {
  return (
    <div className={clsx('col col--3', styles.featureCol)}>
      <div className="feature-card">
        <div className={styles.featureIcon}>{icon}</div>
        <h3 className={styles.featureTitle}>{title}</h3>
        <p className={styles.featureDesc}>{description}</p>
      </div>
    </div>
  );
}

export default function Home() {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout title="Home" description={siteConfig.tagline}>
      {/* Hero */}
      <header className={clsx('hero hero--primary', styles.heroBanner)}>
        <div className="container">
          <div className={styles.heroInner}>
            <div className={styles.heroText}>
              <div className={styles.heroBadge}>Java · Selenium · TestNG</div>
              <h1 className={styles.heroTitle}>Selenium Boot</h1>
              <p className={styles.heroSubtitle}>{siteConfig.tagline}</p>
              <div className={styles.heroButtons}>
                <Link className="button button--secondary button--lg" to="/docs/getting-started">
                  Get Started →
                </Link>
                <Link className="button button--outline button--lg" to="https://github.com/seleniumboot/selenium-boot">
                  GitHub
                </Link>
              </div>
              <div className={styles.heroSnippet}>
                <pre>{`<dependency>
  <groupId>io.github.seleniumboot</groupId>
  <artifactId>selenium-boot</artifactId>
  <version>0.10.0</version>
</dependency>`}</pre>
              </div>
            </div>
            <div className={styles.heroCode}>
              <pre>{`public class LoginTest extends BaseTest {

    @Test(description = "Valid user can log in")
    public void loginTest() {
        StepLogger.step("Open login page");
        open();

        StepLogger.step("Enter credentials", true);
        new LoginPage(getDriver())
            .login("admin", "secret");

        StepLogger.step("Assert dashboard", StepStatus.PASS);
        Assert.assertTrue(
            new DashboardPage(getDriver()).isLoaded()
        );
    }
}`}</pre>
            </div>
          </div>
        </div>
      </header>

      {/* Features */}
      <main>
        <section className={styles.featuresSection}>
          <div className="container">
            <h2 className={styles.sectionTitle}>Everything you need, nothing you don't</h2>
            <div className="row">
              {features.map((f, i) => <Feature key={i} {...f} />)}
            </div>
          </div>
        </section>

        {/* Quick start strip */}
        <section className={styles.quickStart}>
          <div className="container">
            <div className={styles.quickStartInner}>
              <div>
                <h2>Up and running in 3 minutes</h2>
                <p>Add the dependency, create a YAML config, extend BaseTest — your first test runs with full reporting, retry, and smart waits already configured.</p>
                <Link className="button button--primary button--lg" to="/docs/getting-started">
                  Read the guide →
                </Link>
              </div>
              <div className={styles.quickStartCode}>
                <pre>{`# selenium-boot.yml
browser:
  name: chrome
  headless: false

execution:
  baseUrl: https://your-app.com

retry:
  enabled: true
  maxAttempts: 2`}</pre>
              </div>
            </div>
          </div>
        </section>
      </main>
    </Layout>
  );
}
