package com.seleniumboot.ai;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

/**
 * The LLM backends the AI features can talk to. Selected with {@code ai.provider}; the request
 * shape, auth header and response path are the only things that differ.
 *
 * <ul>
 *   <li>{@link #ANTHROPIC} — Claude Messages API (default)</li>
 *   <li>{@link #GEMINI} — Google Gemini {@code generateContent}</li>
 *   <li>{@link #OPENAI} — any OpenAI-compatible {@code /chat/completions} endpoint (OpenAI, Azure,
 *       Groq, Ollama, vLLM, ...); point {@code ai.baseUrl} at it</li>
 * </ul>
 */
enum AiProvider {

    ANTHROPIC("https://api.anthropic.com/v1", "claude-haiku-4-5-20251001"),
    GEMINI("https://generativelanguage.googleapis.com/v1beta", "gemini-flash-latest"),
    OPENAI("https://api.openai.com/v1", "gpt-4o-mini");

    final String defaultBaseUrl;
    final String defaultModel;

    AiProvider(String defaultBaseUrl, String defaultModel) {
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModel = defaultModel;
    }

    /** Parses {@code ai.provider}; blank means Anthropic. Returns {@code null} for an unknown name. */
    static AiProvider parse(String name) {
        if (name == null || name.isBlank()) return ANTHROPIC;
        String n = name.strip().toLowerCase();
        if (n.equals("claude")) return ANTHROPIC;
        if (n.equals("google")) return GEMINI;
        for (AiProvider p : values()) {
            if (p.name().equalsIgnoreCase(n)) return p;
        }
        return null;
    }

    /** Model to use: the configured one, or this provider's default when none is set. */
    String modelOrDefault(String configured) {
        return configured == null || configured.isBlank() ? defaultModel : configured;
    }

    /** Full endpoint URL for {@code model}; {@code baseUrl} overrides the provider default. */
    String endpoint(String baseUrl, String model) {
        String base = baseUrl == null || baseUrl.isBlank() ? defaultBaseUrl : baseUrl.strip();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        switch (this) {
            case GEMINI:  return base + "/models/" + model + ":generateContent";
            case OPENAI:  return base + "/chat/completions";
            default:      return base + "/messages";
        }
    }

    HttpRequest request(String baseUrl, String model, String apiKey, String prompt, int timeoutSeconds) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(endpoint(baseUrl, model)))
                .header("content-type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSeconds));
        switch (this) {
            case GEMINI:
                b.header("x-goog-api-key", apiKey);
                break;
            case OPENAI:
                b.header("authorization", "Bearer " + apiKey);
                break;
            default:
                b.header("x-api-key", apiKey).header("anthropic-version", "2023-06-01");
        }
        return b.POST(HttpRequest.BodyPublishers.ofString(body(model, prompt))).build();
    }

    String body(String model, String prompt) {
        String p = "\"" + escape(prompt) + "\"";
        switch (this) {
            case GEMINI:
                // Gemini 2.5 spends output tokens on thinking, so leave headroom for the answer.
                return "{\"contents\":[{\"parts\":[{\"text\":" + p + "}]}],"
                     + "\"generationConfig\":{\"maxOutputTokens\":2048}}";
            case OPENAI:
                return "{\"model\":\"" + escape(model) + "\",\"max_tokens\":512,"
                     + "\"messages\":[{\"role\":\"user\",\"content\":" + p + "}]}";
            default:
                return "{\"model\":\"" + escape(model) + "\",\"max_tokens\":512,"
                     + "\"messages\":[{\"role\":\"user\",\"content\":" + p + "}]}";
        }
    }

    /** The generated text from a successful response body, or {@code null} when absent. */
    String extract(String json) {
        return AiFailureAnalyzer.extractField(json, this == OPENAI ? "content" : "text");
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n");  break;
                case '\t': sb.append("\\t");  break;
                case '\r': break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }
}
