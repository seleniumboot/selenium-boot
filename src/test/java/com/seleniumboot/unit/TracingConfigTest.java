package com.seleniumboot.unit;

import com.seleniumboot.config.SeleniumBootConfig;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TracingConfigTest {

    @Test
    public void unsetFollowsCiEnvironment() {
        SeleniumBootConfig.Tracing tracing = new SeleniumBootConfig.Tracing();
        assertTrue(tracing.isEffectivelyEnabled(true), "unset tracing is on in CI");
        assertFalse(tracing.isEffectivelyEnabled(false), "unset tracing stays off locally");
    }

    @Test
    public void explicitFalseWinsInCi() {
        SeleniumBootConfig.Tracing tracing = new SeleniumBootConfig.Tracing();
        tracing.setEnabled(false);
        assertFalse(tracing.isEffectivelyEnabled(true));
    }

    @Test
    public void explicitTrueWinsLocally() {
        SeleniumBootConfig.Tracing tracing = new SeleniumBootConfig.Tracing();
        tracing.setEnabled(true);
        assertTrue(tracing.isEffectivelyEnabled(false));
    }

    @Test
    public void captureOnPassStaysOffByDefault() {
        assertFalse(new SeleniumBootConfig.Tracing().isCaptureOnPass());
    }
}
