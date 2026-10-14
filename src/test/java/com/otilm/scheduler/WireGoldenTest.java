package com.otilm.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Holds the REST JSON core exchanges with the scheduler, the Spring Data REST root and the health endpoints to goldens
 * recorded on the Spring Boot 3.5 line, so a change of JSON library cannot alter what core and the probes read. Record
 * with {@code -Dwire.golden.write=true} on the 3.5 line only.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WireGoldenTest {

    private static final Path GOLDENS = Path.of("src/test/resources/wire");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern TIMESTAMP = Pattern
            .compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})?");
    /** The parser's own account of a body it could not read, which each parser words differently. */
    private static final Pattern PARSER_MESSAGE = Pattern
            .compile("\"message\"\\s*:\\s*\"JSON parse error:(?:[^\"\\\\]|\\\\.)*\"");
    private static final String SCHEDULER = "/v1/scheduler";
    private static final String JOB_NAME = "WireGoldenTask";
    private static final String CLASS_NAME = "com.otilm.core.tasks.WireGoldenTask";
    /** Far enough ahead that the job never fires while the suite runs. */
    private static final String CRON = "0 0 0 1 1 ? 2099";
    private static final String UPDATED_CRON = "0 30 0 1 1 ? 2099";

    private final HttpClient http = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Test
    void jobLifecycle() throws Exception {
        assertGolden("v1-create", post(SCHEDULER + "/create", request(JOB_NAME, CRON)));
        assertGolden("v1-create-existing", post(SCHEDULER + "/create", request(JOB_NAME, UPDATED_CRON)));
        assertGolden("v1-list", get(SCHEDULER + "/list"));
        assertGolden("v1-disable", get(SCHEDULER + "/" + JOB_NAME + "/disable"));
        assertGolden("v1-list-disabled", get(SCHEDULER + "/list"));
        assertGolden("v1-enable", get(SCHEDULER + "/" + JOB_NAME + "/enable"));
        assertGolden("v1-update", getWithBody(SCHEDULER + "/update", request(JOB_NAME, UPDATED_CRON)));
        assertGolden("v1-list-updated", get(SCHEDULER + "/list"));
        assertGolden("v1-delete", delete(SCHEDULER + "/" + JOB_NAME));
        assertGolden("v1-list-empty", get(SCHEDULER + "/list"));
    }

    @Test
    void errors() throws Exception {
        assertGolden("v1-create-invalid-cron", post(SCHEDULER + "/create", request("WireGoldenInvalid", "never")));
        assertGolden("v1-create-unreadable-body", post(SCHEDULER + "/create", "{"));
    }

    @Test
    void dataRestRoot() throws Exception {
        assertGolden("data-rest-root", get("/"));
        assertGolden("data-rest-profile", get("/profile"));
    }

    /** The broker the test profile names is unreachable, so the overall health shows whether the broker counts. */
    @Test
    void health() throws Exception {
        assertGolden("health", get("/health"));
        assertGolden("health-liveness", get("/health/liveness"));
        assertGolden("health-readiness", get("/health/readiness"));
    }

    /** A job as core writes it: every field of the DTO, the ones it leaves unset as null. */
    private static String request(String jobName, String cronExpression) {
        return """
                {"schedulerJob":{"uuidJob":null,"jobName":"%s","cronExpression":"%s","classNameToBeExecuted":"%s",
                 "nextFireTime":null,"previousFireTime":null,"triggerState":null}}"""
                .formatted(jobName, cronExpression, CLASS_NAME);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path)).GET());
    }

    /** Core sends the update as a GET with a body. */
    private HttpResponse<String> getWithBody(String path, String body) throws IOException, InterruptedException {
        return send(HttpRequest
                .newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString(body)));
    }

    private HttpResponse<String> post(String path, String body) throws IOException, InterruptedException {
        return send(HttpRequest
                .newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)));
    }

    private HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path)).DELETE());
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws IOException, InterruptedException {
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    /** Masks what differs between runs and hosts: the server port, the time and zone, and the parser's wording. */
    private String normalized(String body) {
        String masked = body.replace("localhost:" + port, "localhost:<port>");
        masked = PARSER_MESSAGE.matcher(masked).replaceAll("\"message\":\"JSON parse error: <parser message>\"");
        return TIMESTAMP
                .matcher(masked)
                .replaceAll(match -> match
                        .group()
                        .replaceAll("(Z|[+-]\\d{2}:?\\d{2})$", "<zone>")
                        .replaceAll("[0-9]", "9")
                        .replaceAll("\\.9+", ".9"));
    }

    /**
     * Sorts every array and object, since neither order is part of the contract and Jackson 3 writes properties
     * alphabetically.
     */
    private static JsonNode inAnyOrder(JsonNode node) {
        if (node.isArray()) {
            List<JsonNode> elements = new ArrayList<>();
            node.forEach(element -> elements.add(inAnyOrder(element)));
            elements.sort(Comparator.comparing(JsonNode::toString));
            return JSON.createArrayNode().addAll(elements);
        }
        if (node.isObject()) {
            ObjectNode sorted = JSON.createObjectNode();
            node
                    .properties()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(field -> sorted.set(field.getKey(), inAnyOrder(field.getValue())));
            return sorted;
        }
        return node;
    }

    private void assertGolden(String name, HttpResponse<String> response) throws IOException {
        ObjectNode actual = JSON.createObjectNode();
        actual.put("status", response.statusCode());
        actual.put("contentType", response.headers().firstValue("Content-Type").map(t -> t.split(";")[0]).orElse(""));
        String body = normalized(response.body());
        actual.set("body", body.isEmpty() ? null : inAnyOrder(JSON.readTree(body)));

        Path golden = GOLDENS.resolve(name + ".json");
        if (Boolean.getBoolean("wire.golden.write")) {
            Files.writeString(golden, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(actual) + "\n");
        }
        JsonNode expected = JSON.readTree(Files.readString(golden));
        assertEquals(expected, actual, "Wire output drifted from " + golden + ": " + actual);
    }
}
