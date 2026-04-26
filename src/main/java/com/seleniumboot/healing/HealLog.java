package com.seleniumboot.healing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Thread-safe log of every {@link HealEvent} that occurred during the suite.
 * Exported to {@code target/healed-locators.json} at suite end.
 */
public final class HealLog {

    private static final Logger LOG = Logger.getLogger(HealLog.class.getName());
    private static final List<HealEvent> EVENTS = new CopyOnWriteArrayList<>();

    private HealLog() {}

    public static void record(HealEvent event) {
        EVENTS.add(event);
        LOG.warning("[SelfHealing] Healed locator in test '" + event.getTestId() + "': "
                + event.getOriginalLocator() + " → " + event.getHealedLocator()
                + " (strategy: " + event.getStrategy() + ")");
    }

    public static List<HealEvent> getAll() {
        return new ArrayList<>(EVENTS);
    }

    public static int countForTest(String testId) {
        return (int) EVENTS.stream().filter(e -> testId.equals(e.getTestId())).count();
    }

    public static void clear() {
        EVENTS.clear();
    }

    public static void export() {
        if (EVENTS.isEmpty()) return;
        try {
            File dir = new File("target");
            dir.mkdirs();
            File out = new File(dir, "healed-locators.json");

            List<Map<String, Object>> entries = new ArrayList<>();
            for (HealEvent e : EVENTS) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("testId",          e.getTestId());
                m.put("originalLocator", e.getOriginalLocator());
                m.put("healedLocator",   e.getHealedLocator());
                m.put("strategy",        e.getStrategy());
                m.put("timestamp",       e.getTimestamp());
                entries.add(m);
            }

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("totalHeals", EVENTS.size());
            root.put("events",     entries);

            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(out, root);
            System.out.println("[Selenium Boot] Healed locators report → " + out.getPath()
                    + " (" + EVENTS.size() + " heals)");
        } catch (IOException e) {
            LOG.warning("[HealLog] Failed to export healed-locators.json: " + e.getMessage());
        }
    }
}
