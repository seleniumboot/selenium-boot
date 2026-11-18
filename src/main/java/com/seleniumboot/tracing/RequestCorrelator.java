package com.seleniumboot.tracing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Matches CDP response/failure events to the request they belong to, in whatever order they arrive.
 *
 * <p>Selenium hands each CDP message to a pooled thread, so {@code responseReceived} or
 * {@code loadingFailed} can be handled before the {@code requestWillBeSent} of the same request.
 * An action for a request that is not known yet is parked and runs as soon as the request arrives,
 * or with {@code null} from {@link #flush()} if it never does.
 */
final class RequestCorrelator {

    record Request(String method, String url) {}

    private final Map<String, Request> requests = new HashMap<>();
    private final Map<String, List<Consumer<Request>>> parked = new HashMap<>();

    /** Records a request and runs any actions that were waiting for it. */
    void requestSent(String requestId, String method, String url) {
        Request request = new Request(method, url);
        List<Consumer<Request>> waiting;
        synchronized (this) {
            requests.put(requestId, request);
            waiting = parked.remove(requestId);
        }
        if (waiting != null) waiting.forEach(a -> a.accept(request));
    }

    /** Runs {@code action} with the request now if it is known, otherwise once it arrives. */
    void whenKnown(String requestId, Consumer<Request> action) {
        Request request;
        synchronized (this) {
            request = requests.get(requestId);
            if (request == null) {
                parked.computeIfAbsent(requestId, k -> new ArrayList<>()).add(action);
                return;
            }
        }
        action.accept(request);
    }

    /** Runs every still-parked action with {@code null}: its request never arrived. */
    void flush() {
        List<Consumer<Request>> orphans = new ArrayList<>();
        synchronized (this) {
            parked.values().forEach(orphans::addAll);
            parked.clear();
        }
        orphans.forEach(a -> a.accept(null));
    }
}
