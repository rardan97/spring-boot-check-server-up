package com.blackcode.app_check_server.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AsyncServerCheckService2 {
    private final WebClient webClient;

    public AsyncServerCheckService2(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public Mono<Map<String, Object>> checkServerWithDynamicDelay(String url, int maxRetry) {
        AtomicInteger attempt = new AtomicInteger(1);

        return Flux.generate(sink -> {
            int currentAttempt = attempt.get();
    
            if (currentAttempt > maxRetry) {
                sink.complete(); // Stop kalau sudah melebihi max attempt
            } else {
                sink.next(currentAttempt);
            }
        })
        .cast(Integer.class)
        .concatMap(attemptNumber -> 
            Mono.delay(getDelayBasedOnAttempt(attemptNumber)) // delay berdasarkan urutan
                .then(checkServer(url, attemptNumber))
        )
        .doOnNext(result -> {
            int attemptNum = (int) result.get("attempts");
            int statusCode = (int) result.get("responseCode");
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    
            System.out.println("🕒 " + time + " | Attempt #" + attemptNum + " | Status: " + statusCode);
        })
        .takeUntil(result -> (int) result.get("responseCode") == 200) // STOP jika 200
        .next() // Ambil hasil pertama yang memenuhi kondisi atau terakhir
        .switchIfEmpty(Mono.just(Map.of(
            "responseCode", 500,
            "message", "Server tidak merespons dalam semua percobaan.",
            "attempts", maxRetry
        )));
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
                result.put("message", response.getStatusCode().value() == 200
                        ? "Server is online"
                        : "Server returned error");
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
        return switch (attempt) {
            case 1 -> Duration.ofSeconds(10);
            case 2 -> Duration.ofSeconds(120);
            default -> Duration.ofSeconds(120);
        };
    }
}
