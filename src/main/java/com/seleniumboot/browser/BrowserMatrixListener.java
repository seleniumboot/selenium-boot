package com.seleniumboot.browser;

import com.seleniumboot.config.ConfigurationLoader;
import com.seleniumboot.config.SeleniumBootConfig;
import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TestNG {@link IAlterSuiteListener} that expands every {@link XmlTest} into one clone per
 * browser when {@code browser.matrix} is configured in {@code selenium-boot.yml}.
 *
 * <p>Example configuration:
 * <pre>
 * browser:
 *   matrix: [chrome, firefox]
 * </pre>
 *
 * <p>Each clone gets:
 * <ul>
 *   <li>a name suffix, e.g. {@code "Login Tests [Chrome]"}</li>
 *   <li>a parameter {@code selenium.boot.browser=chrome} that
 *       {@link com.seleniumboot.listeners.TestExecutionListener} reads at test start
 *       and stores in {@link BrowserContext}</li>
 * </ul>
 *
 * <p>When {@code browser.matrix} is empty or not set, this listener is a no-op and
 * existing behaviour is fully preserved.
 */
public final class BrowserMatrixListener implements IAlterSuiteListener {

    @Override
    public void alter(List<XmlSuite> suites) {
        SeleniumBootConfig config;
        try {
            config = ConfigurationLoader.load();
        } catch (Exception e) {
            // Config may not be available in all contexts; skip silently.
            return;
        }

        List<String> matrix = config.getBrowser() != null
                ? config.getBrowser().getMatrix()
                : null;

        if (matrix == null || matrix.isEmpty()) {
            return;
        }

        for (XmlSuite suite : suites) {
            List<XmlTest> originals = new ArrayList<>(suite.getTests());
            suite.getTests().clear();
            for (XmlTest original : originals) {
                for (String browser : matrix) {
                    XmlTest clone = new XmlTest(suite);
                    clone.setName(original.getName() + " [" + capitalize(browser) + "]");
                    clone.setXmlClasses(original.getXmlClasses());
                    clone.setParallel(original.getParallel());
                    clone.setThreadCount(original.getThreadCount());
                    // Copy existing parameters then add the browser override
                    Map<String, String> params = new LinkedHashMap<>(original.getLocalParameters());
                    params.put("selenium.boot.browser", browser.toLowerCase());
                    clone.setParameters(params);
                }
            }
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}
