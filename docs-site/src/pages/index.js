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
    description: 'Extend BaseTest. Write @Test methods. Driver lifecycle, waits, retry, reports, and screenshots are handled automatically — no setup code required.',
  },
  {
    icon: '📄',
    title: 'YAML Configuration',
    description: 'One selenium-boot.yml controls browser, parallel threads, timeouts, retry, CI thresholds, tracing, AI analysis, and more. Switch environments with a profile flag.',
  },
  {
    icon: '🔁',
    title: 'Smart Retry + Flakiness Radar',
    description: 'Auto-retry flaky tests with @Retryable or globally. After each run, Flakiness Prediction analyses history and ranks HIGH / WATCH / STABLE tests in the report dashboard.',
  },
  {
    icon: '📋',
    title: 'Page Object Toolkit',
    description: 'BasePage covers click, type, getText, dropdowns, alerts, hover, scroll, iFrame helpers, Shadow DOM, and file upload — all wait-backed. SmartLocator tries multiple strategies in order.',
  },
  {
    icon: '🔗',
    title: 'Fluent Locator API',
    description: '$(".row").filter(".active").nth(0).click() — Playwright-style chainable locators. assertThat(By.id("title")).hasText("Welcome") auto-retries until the condition is met or times out.',
  },
  {
    icon: '🌐',
    title: 'Network & Storage Mocking',
    description: 'Stub API responses via CDP: networkMock().stub("**/api/users").returnJson(...). Read/write localStorage, sessionStorage, cookies, geolocation, and clipboard in tests — no JS boilerplate.',
  },
  {
    icon: '📸',
    title: 'Visual Regression + Mobile',
    description: 'assertScreenshot("homepage") compares pixel-by-pixel against a stored baseline. emulateDevice("iPhone 14") applies full CDP viewport + UA emulation. 6 built-in device profiles; register your own.',
  },
  {
    icon: '🪜',
    title: 'Step Logging + Trace Viewer',
    description: 'StepLogger.step("name", true) captures named steps with screenshots. On failure a self-contained dark-themed trace HTML is generated — clickable step timeline, final screenshot, and stack trace.',
  },
  {
    icon: '📊',
    title: 'Advanced HTML Report',
    description: 'Tabbed dashboard: pass-rate gauge, donut chart, retry badges, expandable error + stack trace rows, AI analysis panel, Flakiness Radar, "View Trace" links, filter bar, search, and dark mode.',
  },
  {
    icon: '🩺',
    title: 'Self-Healing Locators',
    description: 'When a locator times out, the framework automatically tries fallback strategies — id, name, text, class, data-testid — and continues the test. Healed locators are logged and flagged in the report.',
  },
  {
    icon: '🧠',
    title: 'AI Failure Analysis',
    description: 'Set ai.failureAnalysis: true and point to your Claude API key. On every failure, Claude Haiku analyses the error, steps, URL, and title — and embeds a plain-English root-cause + fix in the report.',
  },
  {
    icon: '🔐',
    title: '@PreCondition',
    description: 'Eliminate @BeforeMethod login boilerplate. Declare @PreCondition("loginAsAdmin") — the framework runs setup once, caches cookies + localStorage, and restores the session for every test.',
  },
  {
    icon: '🔌',
    title: 'Extensible via SPI',
    description: 'Java ServiceLoader plugin system. Register custom driver providers, report adapters (Allure, Slack, Teams), and lifecycle hooks — zero framework code changes needed.',
  },
  {
    icon: '🤖',
    title: 'CI-Ready',
    description: 'Auto-detects GitHub Actions, Jenkins, CircleCI, and GitLab CI. Forces headless, adjusts thread count, emits JUnit XML, and enforces pass-rate / flakiness gates to fail the build on regressions.',
  },
  {
    icon: '🥒',
    title: 'BDD / Cucumber',
    description: 'BaseCucumberTest + BaseCucumberSteps wire Cucumber into the full framework — driver lifecycle, step timeline, screenshots, and HTML report per scenario. Add the glue path and go.',
  },
  {
    icon: '5️⃣',
    title: 'JUnit 5 Support',
    description: 'Extend BaseJUnit5Test or use @ExtendWith(SeleniumBootExtension.class). Full parity with TestNG: driver injection, $(), assertThat(), step(), HTML report, AI analysis, and trace viewer.',
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
  <version>1.11.0</version>
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
