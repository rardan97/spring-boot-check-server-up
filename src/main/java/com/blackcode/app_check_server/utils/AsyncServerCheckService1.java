package com.blackcode.app_check_server.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AsyncServerCheckService1 {
    private final WebClient webClient;

    public AsyncServerCheckService1(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Map<String, Object> checkServerStatus(String url, int maxRetry) {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

        Map<String, Object> result = checkServerStatusWithSmartStop(url, maxRetry)
            .doOnNext(r -> {
                int attempt = (int) r.get("attempts");
                int responseCode = (int) r.get("responseCode");
                String time = LocalDateTime.now().format(timeFormat);

                System.out.println("🕒 " + time + " | Attempt: " + attempt + " | Status: " + responseCode);

                if (responseCode == 200) {
                    System.out.println("✅ Server success at " + time + " on attempt #" + attempt);
                }
            })
            .doOnTerminate(() -> System.out.println("🔚 Pengecekan selesai"))
            .blockLast();

        if (result != null) {
            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("status", result.get("responseCode"));
            jsonResponse.put("message", result.get("message"));
            jsonResponse.put("attempt", result.get("attempts"));
            return jsonResponse;
        } else {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 500);
            error.put("message", "Tidak mendapatkan respons dari server.");
            error.put("attempt", maxRetry);
            return error;
        }
    }

    public Flux<Map<String, Object>> checkServerStatusWithSmartStop(String url, int maxRetry) {
        return Flux.range(1, maxRetry)
            .concatMap(attempt ->
                checkServer(url, attempt)
                    .delaySubscription(getDelayBasedOnAttempt(attempt))
            )
            .takeUntil(result -> (int) result.get("responseCode") == 200);
    }

    private Mono<Map<String, Object>> checkServer(String url, int attempt) {
        return webClient.get()
            .uri(url)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(5))
            .map(response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("responseCode", response.getStatusCode().value());
                result.put("attempts", attempt);
                result.put("message", response.getStatusCode().is2xxSuccessful() ?
                        "Server is online" : "Server returned error");
                return result;
            })
            .onErrorResume(e -> {
                Map<String, Object> result = new HashMap<>();
                result.put("responseCode", 503);
                result.put("attempts", attempt);
                result.put("message", "Server is unreachable");
                result.put("error", e.getMessage());
                return Mono.just(result);
            });
    }

    private Duration getDelayBasedOnAttempt(int attempt) {
        switch (attempt) {
            case 1: return Duration.ZERO;          // no delay for first try
            case 2: return Duration.ofSeconds(50);  // 5s for second try
            case 3: return Duration.ofSeconds(50); // 10s for third
            default: return Duration.ofSeconds(55);
        }
    }
}
