package com.bettingengine.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;

public abstract class LocalKubernetesTestSupport {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    protected void assumeLocalKubernetesEnabled() {
        Assumptions.assumeTrue(Boolean.getBoolean("local.k8s.enabled"));
    }

    protected HttpResponse<String> publishOutcome(long eventId, String eventName, long eventWinnerId, String correlationId)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/event-outcomes"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("X-Correlation-Id", correlationId)
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "eventId": %d,
                          "eventName": "%s",
                          "eventWinnerId": %d
                        }
                        """.formatted(eventId, eventName, eventWinnerId)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + path))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    protected void waitForAsyncProcessing() throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(5).toMillis());
    }

    protected void assertLogContains(String correlationId, String expectedFragment) throws IOException, InterruptedException {
        String logs = applicationLogs();
        assertThat(logs).contains(correlationId);
        assertThat(logs).contains(expectedFragment);
    }

    protected void assertSettlementLogCount(String correlationId, int expectedCount) throws IOException, InterruptedException {
        assertThat(settlementLogCount(correlationId)).isEqualTo(expectedCount);
    }

    protected long settlementLogCount(String correlationId) throws IOException, InterruptedException {
        String logs = applicationLogs();
        return logs.lines()
                .filter(line -> line.contains(correlationId))
                .filter(line -> line.contains("Published settlement message to RocketMQ"))
                .count();
    }

    protected void assertLogLineCount(String correlationId, String expectedFragment, int expectedCount)
            throws IOException, InterruptedException {
        assertThat(logLineCount(correlationId, expectedFragment)).isEqualTo(expectedCount);
    }

    protected long logLineCount(String correlationId, String expectedFragment) throws IOException, InterruptedException {
        String logs = applicationLogs();
        return logs.lines()
                .filter(line -> line.contains(correlationId))
                .filter(line -> line.contains(expectedFragment))
                .count();
    }

    protected void assertRedisClaimCount(long eventId, int expectedCount) throws IOException, InterruptedException {
        String output = runCommand(List.of(
                kubectl(),
                "-n", namespace(),
                "exec", "deployment/redis", "--",
                "sh", "-lc",
                "redis-cli --scan --pattern 'settlement-claims:*:" + eventId + "' | wc -l"
        )).trim();

        assertThat(Integer.parseInt(output)).isEqualTo(expectedCount);
    }

    protected void assertDeploymentReady(String deploymentName) throws IOException, InterruptedException {
        String desired = runCommand(List.of(
                kubectl(),
                "-n", namespace(),
                "get", "deployment", deploymentName,
                "-o", "jsonpath={.status.replicas}"
        )).trim();
        String available = runCommand(List.of(
                kubectl(),
                "-n", namespace(),
                "get", "deployment", deploymentName,
                "-o", "jsonpath={.status.availableReplicas}"
        )).trim();

        assertThat(available)
                .as("available replicas for deployment %s", deploymentName)
                .isEqualTo(desired);
    }

    protected String uniqueCorrelationId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    protected String baseUrl() {
        return System.getProperty("local.k8s.base-url", "http://localhost:8080");
    }

    protected String namespace() {
        return System.getProperty("local.k8s.namespace", "betting-engine-local");
    }

    protected String kubectl() {
        return System.getProperty("kubectl.bin", "kubectl");
    }

    protected String applicationLogs() throws IOException, InterruptedException {
        String podName = runCommand(List.of(
                kubectl(),
                "-n", namespace(),
                "get", "pods",
                "-l", "app=betting-engine",
                "-o", "jsonpath={.items[0].metadata.name}"
        )).trim();

        return runCommand(List.of(
                kubectl(),
                "-n", namespace(),
                "logs", podName,
                "-c", "betting-engine",
                "--tail=500"
        ));
    }

    private String runCommand(List<String> command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        StringBuilder output = new StringBuilder();
        Thread readerThread = Thread.ofVirtual().start(() -> {
            try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to read command output: " + String.join(" ", command), exception);
            }
        });

        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Command timed out: " + String.join(" ", command));
        }

        readerThread.join(Duration.ofSeconds(5));
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + System.lineSeparator() + output);
        }

        return output.toString();
    }
}
