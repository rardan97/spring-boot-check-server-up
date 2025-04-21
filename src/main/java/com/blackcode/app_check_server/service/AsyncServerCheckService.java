package com.blackcode.app_check_server;


import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class AsyncServerCheckService {
    private final WebClient webClient;
    private final Scheduler scheduler;

    @Autowired
    public AsyncServerCheckService(WebClient webClient) {
        this.webClient = webClient;
        this.scheduler = Schedulers.boundedElastic();
    }

    public Map<String, Object> checkServerStatus(String url, int maxRetry) {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

        Map<String, Object> result = checkServerWithRetry(url, maxRetry)
            .doOnNext(r -> {
                int attempt = (int) r.get("attempts");
                int responseCode = (int) r.get("responseCode");
                String time = LocalDateTime.now().format(timeFormat);

                System.out.println("🕒 " + time + " | Attempt: " + attempt + " | Status: " + responseCode);

                // Jika server berhasil (200), log waktu sukses
                if (responseCode == 200) {
                    System.out.println("Success : Server success at " + time + " on attempt #" + attempt);
                }
            })
            .doOnTerminate(() -> System.out.println("🔚 Pengecekan selesai"))
            .block();

        if (result != null) {
            System.out.println("Status: " + result.get("message") +
                " (Code: " + result.get("responseCode") + ") on Attempt #" + result.get("attempts"));
                Map<String, Object> jsonResponse = new HashMap<>();
                jsonResponse.put("status", result.get("responseCode"));
                jsonResponse.put("message", result.get("message"));
                jsonResponse.put("attempt", result.get("attempts"));
                return jsonResponse;
        } else {
            System.out.println("Gagal : Tidak mendapatkan respons dari server.");

            Map<String, Object> error = new HashMap<>();
            error.put("status", 500);
            error.put("message", "Tidak mendapatkan respons dari server.");
            error.put("attempt", maxRetry);
            return error;
        }
    }

    public Mono<Map<String, Object>> checkServerWithRetry(String url, int maxRetry) {
        return tryCheck(url, 1, maxRetry);  // Mulai dengan percobaan pertama
    }

     // Method rekursif untuk mencoba cek server
     private Mono<Map<String, Object>> tryCheck(String url, int attempt, int maxAttempt) {
        return checkServer(url, attempt)  // Lakukan pengecekan server
            .flatMap(result -> {
                int status = (int) result.get("responseCode");
                // Jika status 200 atau sudah mencapai maxAttempt, berhenti dan return hasil
                if (status == 200) {
                    return Mono.just(result);
                }

                if(attempt >= maxAttempt){
                    return Mono.just(result);
                }

                // Cetak pesan jika gagal
                System.out.println("Gagal : Attempt " + attempt + " failed. Retrying...");

                // Tentukan waktu delay yang berbeda antara setiap percobaan
                // Duration delay;
                Duration delay = getDelayBasedOnAttempt(attempt);

                System.out.println("Gagal : Attempt #" + attempt + " gagal. Delay " + delay.getSeconds() + "s → retry...");
                
                // Jika gagal, tunggu beberapa detik dan coba lagi
                return Mono.delay(delay)
                           .flatMap(ignored -> tryCheck(url, attempt + 1, maxAttempt));  // Coba lagi
            });
    }

    

    // Method untuk cek server di URL tertentu
    private Mono<Map<String, Object>> checkServer(String url, int attempt) {
        return webClient.get()
            .uri(url)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(5))  // Timeout jika server lambat
            .map(response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("responseCode", response.getStatusCode().value());
                result.put("attempts", attempt);
                result.put("message", response.getStatusCode().value() == 200 ?
                        "Server is online" : "Server returned error");
                return result;
            })
            .onErrorResume(e -> {
                // Jika terjadi error (misalnya server down), return 503
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
            case 1: return Duration.ofSeconds(40);
            case 2: return Duration.ofSeconds(50);
            default: return Duration.ofSeconds(60);
        }
    }
}
