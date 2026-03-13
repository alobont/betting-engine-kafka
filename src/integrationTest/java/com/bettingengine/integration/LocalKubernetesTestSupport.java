package com.bettingengine.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.sql.ResultSet;
import org.apache.ignite.sql.SqlRow;
import org.junit.jupiter.api.Assumptions;

public abstract class LocalKubernetesTestSupport {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    protected void assumeLocalKubernetesEnabled() {
        Assumptions.assumeTrue(Boolean.getBoolean("local.k8s.enabled"));
    }

    protected void assumeLocalHighAvailabilityEnabled() {
        Assumptions.assumeTrue(localHaEnabled());
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
        Thread.sleep(Duration.ofSeconds(localHaEnabled() ? 8 : 5).toMillis());
    }

    protected void assertLogContains(String correlationId, String expectedFragment) throws IOException, InterruptedException {
        await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(3)).untilAsserted(() -> {
            String logs = applicationLogs();
            assertThat(logs).contains(correlationId);
            assertThat(logs).contains(expectedFragment);
        });
    }

    protected void assertSettlementLogCount(String correlationId, int expectedCount) throws IOException, InterruptedException {
        await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(settlementLogCount(correlationId)).isEqualTo(expectedCount));
    }

    protected long settlementLogCount(String correlationId) throws IOException, InterruptedException {
        return logLineCount(correlationId, "Published settlement message to RocketMQ");
    }

    protected void assertLogLineCount(String correlationId, String expectedFragment, int expectedCount)
            throws IOException, InterruptedException {
        await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(logLineCount(correlationId, expectedFragment)).isEqualTo(expectedCount));
    }

    protected long logLineCount(String correlationId, String expectedFragment) throws IOException, InterruptedException {
        String logs = applicationLogs();
        return logs.lines()
                .filter(line -> line.contains(correlationId))
                .filter(line -> line.contains(expectedFragment))
                .count();
    }

    protected void assertIgniteClaimCount(long eventId, int expectedCount) {
        await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(3)).untilAsserted(() ->
                assertThat(settlementClaimCount(eventId)).isEqualTo(expectedCount));
    }

    protected void clearSettlementClaims(long eventId) {
        withIgniteClient(client -> {
            client.sql().execute(null,
                    "DELETE FROM SETTLEMENT_CLAIMS WHERE SETTLEMENT_KEY LIKE ?",
                    "%:" + eventId);
            return null;
        });
    }

    protected void assertDeploymentReady(String deploymentName) throws IOException, InterruptedException {
        assertDeploymentReady(deploymentName, replicaCount("deployment", deploymentName));
    }

    protected void assertDeploymentReady(String deploymentName, int expectedReplicas) throws IOException, InterruptedException {
        int readyReplicas = currentReplicaValue("deployment", deploymentName, "{.status.readyReplicas}");
        int availableReplicas = currentReplicaValue("deployment", deploymentName, "{.status.availableReplicas}");

        assertThat(readyReplicas)
                .as("ready replicas for deployment %s", deploymentName)
                .isEqualTo(expectedReplicas);
        assertThat(availableReplicas)
                .as("available replicas for deployment %s", deploymentName)
                .isEqualTo(expectedReplicas);
    }

    protected void assertStatefulSetReady(String statefulSetName, int expectedReplicas) throws IOException, InterruptedException {
        int readyReplicas = currentReplicaValue("statefulset", statefulSetName, "{.status.readyReplicas}");
        assertThat(readyReplicas)
                .as("ready replicas for statefulset %s", statefulSetName)
                .isEqualTo(expectedReplicas);
    }

    protected void waitForDeploymentReady(String deploymentName, int expectedReplicas) {
        await().atMost(Duration.ofMinutes(4)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertDeploymentReady(deploymentName, expectedReplicas);
        });
    }

    protected void waitForStatefulSetReady(String statefulSetName, int expectedReplicas) {
        await().atMost(Duration.ofMinutes(4)).pollInterval(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertStatefulSetReady(statefulSetName, expectedReplicas);
        });
    }

    protected List<String> podNames(String labelSelector) throws IOException, InterruptedException {
        String output = runKubectl(List.of(
                "-n", namespace(),
                "get", "pods",
                "-l", labelSelector,
                "-o", "jsonpath={range .items[*]}{.metadata.name}{'\\n'}{end}"
        ));

        return output.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    protected String firstPodName(String labelSelector) throws IOException, InterruptedException {
        return podNames(labelSelector).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No pods found for selector " + labelSelector));
    }

    protected void deletePod(String podName) throws IOException, InterruptedException {
        runKubectl(List.of(
                "-n", namespace(),
                "delete", "pod", podName,
                "--wait=false"
        ));
    }

    protected void awaitHttpOk(String path) {
        await().atMost(Duration.ofMinutes(2)).pollInterval(Duration.ofSeconds(2)).untilAsserted(() -> {
            HttpResponse<String> response = get(path);
            assertThat(response.statusCode()).isEqualTo(200);
        });
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

    protected String kubectlContext() {
        return System.getProperty("kubectl.context", "kind-betting-engine-local");
    }

    protected boolean localHaEnabled() {
        return Boolean.getBoolean("local.k8s.ha");
    }

    protected String applicationLogs() throws IOException, InterruptedException {
        return podNames("app=betting-engine").stream()
                .map(podName -> {
                    try {
                        return "=== " + podName + " ===" + System.lineSeparator()
                                + runKubectl(List.of(
                                "-n", namespace(),
                                "logs", podName,
                                "-c", "betting-engine",
                                "--tail=500"
                        ));
                    } catch (IOException | InterruptedException exception) {
                        throw new IllegalStateException("Failed to read logs for pod " + podName, exception);
                    }
                })
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private long settlementClaimCount(long eventId) {
        return withIgniteClient(client -> {
            try (ResultSet<SqlRow> resultSet = client.sql().execute(
                    null,
                    "SELECT COUNT(*) AS CLAIM_COUNT FROM SETTLEMENT_CLAIMS WHERE SETTLEMENT_KEY LIKE ?",
                    "%:" + eventId
            )) {
                SqlRow row = resultSet.next();
                return row.longValue("CLAIM_COUNT");
            }
        });
    }

    private int replicaCount(String resourceKind, String resourceName) throws IOException, InterruptedException {
        return currentReplicaValue(resourceKind, resourceName, "{.spec.replicas}");
    }

    private int currentReplicaValue(String resourceKind, String resourceName, String jsonPath)
            throws IOException, InterruptedException {
        String output = runKubectl(List.of(
                "-n", namespace(),
                "get", resourceKind, resourceName,
                "-o", "jsonpath=" + jsonPath
        )).trim();

        return Integer.parseInt(output.isBlank() ? "0" : output);
    }

    private List<String> igniteAddresses() {
        return Arrays.stream(System.getProperty("local.k8s.ignite-addresses", "127.0.0.1:10800").split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private <T> T withIgniteClient(IgniteAction<T> action) {
        try (IgniteClient igniteClient = IgniteClient.builder()
                .addresses(igniteAddresses().toArray(String[]::new))
                .build()) {
            return action.apply(igniteClient);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to execute Ignite test operation.", exception);
        }
    }

    private String runKubectl(List<String> args) throws IOException, InterruptedException {
        List<String> command = new java.util.ArrayList<>();
        command.add(kubectl());
        command.add("--context");
        command.add(kubectlContext());
        command.addAll(args);
        return runCommand(command);
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

        if (!process.waitFor(60, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException("Command timed out: " + String.join(" ", command));
        }

        readerThread.join(Duration.ofSeconds(5));
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Command failed: " + String.join(" ", command) + System.lineSeparator() + output);
        }

        return output.toString();
    }

    @FunctionalInterface
    private interface IgniteAction<T> {
        T apply(IgniteClient igniteClient) throws Exception;
    }
}
