package com.seleniumboot.test;

import com.seleniumboot.api.SeleniumBootApi;
import com.seleniumboot.assertion.SoftAssertionCollector;
import com.seleniumboot.assertion.SoftAssertions;
import com.seleniumboot.client.ApiClient;
import com.seleniumboot.context.ScenarioContext;
import com.seleniumboot.context.SuiteContext;
import com.seleniumboot.listeners.SuiteExecutionListener;
import com.seleniumboot.listeners.TestExecutionListener;
import com.seleniumboot.testdata.TestDataStore;
import org.testng.annotations.Listeners;

import java.util.Map;

/**
 * BaseApiTest is the mandatory superclass for pure API tests.
 *
 * Same framework lifecycle as {@link BaseTest} — reporting, {@code @TestData},
 * retry, CI gates — but no browser is started.
 *
 * <pre>
 * public class UserApiTest extends BaseApiTest {
 *
 *     {@literal @}Test
 *     public void createUser() {
 *         ApiResponse res = apiClient().post("/api/users")
 *                 .body(Map.of("name", "John", "email", "john@example.com"))
 *                 .send();
 *         res.assertStatus(201);
 *         suiteCtx().set("createdUserId", res.json("$.id"));
 *     }
 * }
 * </pre>
 */
@SeleniumBootApi(since = "1.1.0")
@Listeners({
        SuiteExecutionListener.class,
        TestExecutionListener.class
})
public abstract class BaseApiTest {

    private static final ScenarioContext SCENARIO_CTX = new ScenarioContext();
    private static final SuiteContext    SUITE_CTX    = new SuiteContext();

    /**
     * Returns a new {@link ApiClient} for making HTTP requests.
     * Base URL is read from {@code execution.baseUrl} or {@code api.baseUrl} in {@code selenium-boot.yml}.
     */
    protected ApiClient apiClient() {
        return ApiClient.create();
    }

    /** Returns the in-test (thread-local) context store. Cleared after each test. */
    protected ScenarioContext ctx() {
        return SCENARIO_CTX;
    }

    /** Returns the suite-scoped (global) context store. Survives between tests. */
    protected SuiteContext suiteCtx() {
        return SUITE_CTX;
    }

    /**
     * Returns the test data loaded by {@code @TestData} for the current test.
     */
    protected Map<String, Object> getTestData() {
        return TestDataStore.get();
    }

    /**
     * Typed test data retrieval.
     *
     * <pre>
     * String username = getTestData("username", String.class);
     * int    age      = getTestData("age", Integer.class);
     * </pre>
     */
    @SuppressWarnings("unchecked")
    protected <T> T getTestData(String key, Class<T> type) {
        Object value = TestDataStore.get().get(key);
        if (value == null) throw new IllegalStateException(
            "[TestData] Key not found: '" + key + "'");
        return (T) value;
    }

    /** Soft assertion collector — failures are reported all-at-once at test end. */
    protected SoftAssertionCollector softAssert() {
        return SoftAssertions.get();
    }
}
