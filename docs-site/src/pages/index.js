import React, { useEffect } from 'react';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import styles from './index.module.css';

// ─── Feature data ─────────────────────────────────────────────────────────────

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
    description: 'Auto-retry flaky tests with @Retryable or globally. After each run, Flakiness Prediction ranks HIGH / WATCH / STABLE tests in the report dashboard.',
  },
  {
    icon: '📋',
    title: 'Page Object Toolkit',
    description: 'BasePage covers click, type, getText, dropdowns, alerts, hover, scroll, iFrame helpers, Shadow DOM, and file upload — all wait-backed. SmartLocator tries multiple strategies.',
  },
  {
    icon: '🔗',
    title: 'Fluent Locator API',
    description: '$(".row").filter(".active").nth(0).click() — Playwright-style chainable locators. assertThat(By.id("title")).hasText("Welcome") auto-retries until the condition is met.',
  },
  {
    icon: '🌐',
    title: 'Network & Storage Mocking',
    description: 'Stub API responses via CDP: networkMock().stub("**/api/users").returnJson(...). Read/write localStorage, sessionStorage, cookies, geolocation, and clipboard in tests.',
  },
  {
    icon: '📸',
    title: 'Visual Regression + Mobile',
    description: 'assertScreenshot("homepage") compares pixel-by-pixel against a baseline. emulateDevice("iPhone 14") applies full CDP viewport + UA emulation. 6 built-in device profiles.',
  },
  {
    icon: '🪜',
    title: 'Step Logging + Trace Viewer',
    description: 'StepLogger.step() captures named steps with screenshots. On failure a self-contained dark-themed trace HTML is generated — step timeline, final screenshot, and stack trace.',
  },
  {
    icon: '📊',
    title: 'Advanced HTML Report',
    description: 'Tabbed dashboard: pass-rate gauge, donut chart, retry badges, expandable error rows, AI analysis panel, Flakiness Radar, "View Trace" links, filter bar, search, dark mode.',
  },
  {
    icon: '🩺',
    title: 'Self-Healing Locators',
    description: 'When a locator times out, the framework automatically tries fallback strategies — id, name, text, class, data-testid — and continues. Healed locators are flagged in the report.',
  },
  {
    icon: '🧠',
    title: 'AI Failure Analysis',
    description: 'Set ai.failureAnalysis: true and point to your Claude API key. On every failure, Claude Haiku analyses the error, steps, URL, and title — and embeds a plain-English root-cause + fix.',
  },
  {
    icon: '🔐',
    title: '@PreCondition',
    description: 'Eliminate @BeforeMethod login boilerplate. Declare @PreCondition("loginAsAdmin") — the framework runs setup once, caches cookies + localStorage, and restores the session for every test.',
  },
  {
    icon: '📧',
    title: 'Email Verification',
    description: 'mailbox().waitForEmail(to("user@test.com")) polls until a matching email arrives. Supports Mailhog, Mailtrap, Outlook (Graph API), and IMAP. extractLink() finds anchors by text.',
  },
  {
    icon: '🕐',
    title: 'Clock Mocking',
    description: 'clock().set("2030-01-01T00:00:00Z") freezes the browser Date object. Test subscription expiry banners, trial periods, and countdown timers without touching the database.',
  },
  {
    icon: '☁️',
    title: 'Cloud Execution',
    description: 'Switch to BrowserStack or Sauce Labs by changing one line. execution.mode: browserstack — no test-code changes. Session video and logs linked in the HTML report.',
  },
  {
    icon: '🔌',
    title: 'Extensible via SPI',
    description: 'Java ServiceLoader plugin system. Register custom driver providers, report adapters (Allure, Slack, Teams), and lifecycle hooks — zero framework code changes needed.',
  },
];

const stats = [
  { value: '1',   label: 'Dependency to add' },
  { value: '20+', label: 'Built-in features' },
  { value: '4',   label: 'CI platforms auto-detected' },
  { value: '0',   label: 'Boilerplate required' },
];

// ─── Components ───────────────────────────────────────────────────────────────

function CodeWindow({ filename, code, className }) {
  return (
    <div className={`${styles.codeWindow} ${className || ''}`}>
      <div className={styles.codeWindowBar}>
        <span className={styles.dot} data-color="red" />
        <span className={styles.dot} data-color="yellow" />
        <span className={styles.dot} data-color="green" />
        <span className={styles.codeWindowFilename}>{filename}</span>
      </div>
      <pre className={styles.codeWindowBody}>{code}</pre>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function Home() {
  const { siteConfig } = useDocusaurusContext();

  // Scroll-reveal: add data-visible attribute when element enters the viewport
  useEffect(() => {
    const els = document.querySelectorAll('[data-reveal]');
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            e.target.setAttribute('data-visible', '');
            observer.unobserve(e.target); // fire once
          }
        });
      },
      { threshold: 0.1, rootMargin: '0px 0px -40px 0px' }
    );
    els.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, []);

  return (
    <Layout title="Home" description={siteConfig.tagline}>

      {/* ── Hero ─────────────────────────────────────────────────────────── */}
      <section className={styles.heroSection}>
        {/* Decorative background blobs */}
        <div className={styles.heroBlob1} aria-hidden />
        <div className={styles.heroBlob2} aria-hidden />
        <div className={styles.heroBlob3} aria-hidden />
        {/* Dot-grid overlay */}
        <div className={styles.heroDots} aria-hidden />

        <div className="container">
          <div className={styles.heroInner}>

            {/* Left column — text */}
            <div className={styles.heroLeft}>
              <div className={styles.heroBadge}>
                <span className={styles.badgePulse} />
                Java · Selenium · TestNG · JUnit 5
              </div>

              <h1 className={styles.heroTitle}>
                Test automation<br />
                <span className={styles.gradientText}>without the noise</span>
              </h1>

              <p className={styles.heroSubtitle}>{siteConfig.tagline}</p>

              <div className={styles.heroActions}>
                <Link className={styles.btnPrimary} to="/docs/getting-started">
                  Get Started <span className={styles.arrow}>→</span>
                </Link>
                <Link className={styles.btnGhost} to="https://github.com/seleniumboot/selenium-boot">
                  <svg className={styles.githubIcon} viewBox="0 0 24 24" fill="currentColor" aria-hidden>
                    <path d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0 1 12 6.844a9.59 9.59 0 0 1 2.504.337c1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.02 10.02 0 0 0 22 12.017C22 6.484 17.522 2 12 2z"/>
                  </svg>
                  GitHub
                </Link>
              </div>

              <CodeWindow
                filename="pom.xml"
                className={styles.heroSnippet}
                code={`<dependency>
  <groupId>io.github.seleniumboot</groupId>
  <artifactId>selenium-boot</artifactId>
  <version>2.2.0</version>
</dependency>`}
              />
            </div>

            {/* Right column — code preview */}
            <div className={styles.heroRight}>
              <CodeWindow
                filename="LoginTest.java"
                code={`public class LoginTest extends BaseTest {

  @Test(description = "Valid user can log in")
  public void loginTest() {
    StepLogger.step("Open login page");
    open();

    StepLogger.step("Enter credentials", true);
    new LoginPage(getDriver())
        .login("admin", "secret");

    StepLogger.step("Assert dashboard");
    Assert.assertTrue(
        new DashboardPage(getDriver()).isLoaded()
    );
  }
}`}
              />
              {/* Glow ring behind the code card */}
              <div className={styles.codeGlow} aria-hidden />
            </div>

          </div>
        </div>
      </section>

      <main>

        {/* ── Stats strip ──────────────────────────────────────────────────── */}
        <section className={styles.statsSection}>
          <div className="container">
            <div className={styles.statsGrid}>
              {stats.map((s, i) => (
                <div key={i} className={styles.statItem} data-reveal style={{ '--i': i }}>
                  <span className={styles.statValue}>{s.value}</span>
                  <span className={styles.statLabel}>{s.label}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Features ─────────────────────────────────────────────────────── */}
        <section className={styles.featuresSection}>
          <div className="container">

            <div className={styles.sectionHeader} data-reveal>
              <span className={styles.sectionTag}>Features</span>
              <h2 className={styles.sectionTitle}>Everything you need,<br />nothing you don't</h2>
              <p className={styles.sectionSubtitle}>
                One dependency. Zero required config. Full-stack automation power, ready the moment you extend BaseTest.
              </p>
            </div>

            <div className={styles.featuresGrid}>
              {features.map((f, i) => (
                <div
                  key={i}
                  className={styles.featureCard}
                  data-reveal
                  style={{ '--i': i % 8 }}
                >
                  <div className={styles.featureIconWrap}>
                    <span className={styles.featureIcon}>{f.icon}</span>
                  </div>
                  <h3 className={styles.featureTitle}>{f.title}</h3>
                  <p className={styles.featureDesc}>{f.description}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* ── Quick start ───────────────────────────────────────────────────── */}
        <section className={styles.quickSection}>
          <div className="container">
            <div className={styles.quickInner}>

              <div className={styles.quickText} data-reveal>
                <span className={styles.sectionTag}>Quick Start</span>
                <h2 className={styles.quickTitle}>Up and running<br />in 3 minutes</h2>
                <p className={styles.quickSubtitle}>
                  Add the dependency, create a YAML config, extend BaseTest — your first test runs with full reporting, retry, and smart waits already configured.
                </p>
                <Link className={styles.btnPrimary} to="/docs/getting-started">
                  Read the guide <span className={styles.arrow}>→</span>
                </Link>
              </div>

              <div className={styles.quickCode} data-reveal style={{ '--i': 1 }}>
                <CodeWindow
                  filename="selenium-boot.yml"
                  code={`browser:
  name: chrome
  headless: false

execution:
  baseUrl: https://your-app.com

retry:
  enabled: true
  maxAttempts: 2

email:
  provider: mailhog

clock:
  injectHeader: false`}
                />
              </div>

            </div>
          </div>
        </section>

      </main>
    </Layout>
  );
}
