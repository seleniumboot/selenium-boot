package com.seleniumboot.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates a JSON string against a JSON Schema file loaded from the classpath.
 * Requires {@code com.networknt:json-schema-validator} on the classpath.
 *
 * <p>Schema files should be placed under {@code src/test/resources/schemas/}.
 *
 * <pre>
 * ApiClient.get("/api/users/1").send().assertSchema("schemas/user.json");
 * </pre>
 */
class SchemaValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static void validate(String responseBody, String schemaPath) {
        ensureValidatorAvailable();
        try {
            InputStream schemaStream = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream(schemaPath);

            if (schemaStream == null) {
                throw new IllegalArgumentException(
                        "[SchemaValidator] Schema file not found on classpath: '" + schemaPath + "'. "
                        + "Place it under src/test/resources/" + schemaPath);
            }

            com.networknt.schema.JsonSchemaFactory factory =
                    com.networknt.schema.JsonSchemaFactory.getInstance(
                            com.networknt.schema.SpecVersion.VersionFlag.V7);

            com.networknt.schema.JsonSchema schema = factory.getSchema(schemaStream);
            JsonNode responseNode = MAPPER.readTree(responseBody);

            Set<com.networknt.schema.ValidationMessage> violations = schema.validate(responseNode);
            if (!violations.isEmpty()) {
                String details = violations.stream()
                        .map(com.networknt.schema.ValidationMessage::getMessage)
                        .collect(Collectors.joining("\n  - ", "\n  - ", ""));
                throw new AssertionError(
                        "[ApiResponse] Schema validation failed against '" + schemaPath + "':" + details);
            }

        } catch (AssertionError | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("[SchemaValidator] Validation error for schema '" + schemaPath + "'", e);
        }
    }

    private static void ensureValidatorAvailable() {
        try {
            Class.forName("com.networknt.schema.JsonSchemaFactory");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "[SchemaValidator] assertSchema() requires 'com.networknt:json-schema-validator' on the classpath. "
                    + "Add it to your pom.xml:\n"
                    + "  <dependency>\n"
                    + "    <groupId>com.networknt</groupId>\n"
                    + "    <artifactId>json-schema-validator</artifactId>\n"
                    + "    <version>1.4.3</version>\n"
                    + "  </dependency>");
        }
    }
}
