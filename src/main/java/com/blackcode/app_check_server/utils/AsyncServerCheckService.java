package com.blackcode.app_check_server.utils;


import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AsyncServerCheckService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncServerCheckService.class);

    private final WebClient webClient;
    private final Scheduler scheduler;

    private final Map<String, List<String>> processLogMap = new ConcurrentHashMap<>();

    @Autowired
    public AsyncServerCheckService(WebClient webClient) {
        this.webClient = webClient;
//        this.scheduler = Schedulers.boundedElastic();
        this.scheduler =  Schedulers.newSingle("server-check");
    }


    public void checkServerStatusAsync(String url, int maxRetry) {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        String processId = UUID.randomUUID().toString();
        processLogMap.put(processId, new ArrayList<>());
        log(processId, "Mulai pengecekan ke " + url + " (maxRetry: " + maxRetry + ")");
        logger.info("Memulai pengecekan server async ke URL: {} dengan maxRetry: {}", url, maxRetry);
        checkServerWithRetry(url, maxRetry, processId)
                .subscribeOn(scheduler)
                .doOnSubscribe(s -> logger.debug("Subscribed untuk pengecekan server di thread: {}", Thread.currentThread().getName()))
                .doOnNext(r -> {
                    Thread current = Thread.currentThread();
                    logger.info("[doOnNext] Thread: {} | (ID Thread : {} )", current.getName(), current.getId());
                    int attempt = (int) r.get("attempts");
                    int responseCode = (int) r.get("responseCode");
                    String time = LocalDateTime.now().format(timeFormat);
                    if (responseCode == 200) {
                        logger.info("[{}] | Success : Server SUCCESS at [{}] on attempt #{} | Status code: {}", processId, time, attempt, responseCode);
                    } else {
                        logger.warn("[{}] | Warning : Server error response on attempt #{} | Status code: {}", processId, attempt, responseCode);
                    }

                    if (r.containsKey("error")) {
                        logger.error("Error detail: {}", r.get("error"));
                    }
                })
                .doOnError(e -> logger.error("Error : Exception terjadi saat pengecekan server: {}", e.getMessage(), e))
                .doOnTerminate(() -> logger.info("[{}] Done checking server", processId))
                .subscribe();
    }

    public Mono<Map<String, Object>> checkServerWithRetry(String url, int maxRetry, String processId) {
        return tryCheck(url, 1, maxRetry, processId);  // Mulai dengan percobaan pertama
    }

     // Method rekursif untuk mencoba cek server
     private Mono<Map<String, Object>> tryCheck(String url, int attempt, int maxAttempt, String processId) {
        return checkServer(url, attempt, processId)  // Lakukan pengecekan server
            .flatMap(result -> {
                int status = (int) result.get("responseCode");
                if (status == 200) {
                    return Mono.just(result);
                }

                if(attempt >= maxAttempt){
                    return Mono.just(result);
                }

                // Cetak pesan jika gagal
                logger.info("{} Failed : Attempt {} failed. Retrying...",processId, attempt);

                // Tentukan waktu delay yang berbeda antara setiap percobaan
                // Duration delay;
                Duration delay = getDelayBasedOnAttempt(attempt);

                log(processId, "Delay " + delay.getSeconds() + "s before retry → Thread: " + Thread.currentThread().getName());
                // Jika gagal, tunggu beberapa detik dan coba lagi
                return Mono.delay(delay, scheduler)
                        .flatMap(ignored -> tryCheck(url, attempt + 1, maxAttempt, processId));  // Coba lagi
            });
    }

    

    // Method untuk cek server di URL
    private Mono<Map<String, Object>> checkServer(String url, int attempt, String processId) {
        return webClient.get()
            .uri(url)
            .retrieve()
            .toBodilessEntity()
            .timeout(Duration.ofSeconds(5))  // Timeout jika server lambat
            .publishOn(scheduler)
            .map(response -> {
                Map<String, Object> result = new HashMap<>();
                result.put("responseCode", response.getStatusCode().value());
                result.put("attempts", attempt);
                result.put("message", response.getStatusCode().value() == 200 ?
                        "Server is online" : "Server returned error => proccessID : " + processId);
                return result;
            })
            .onErrorResume(e -> {
                // Jika terjadi error (misalnya server down), return 503
                Map<String, Object> result = new HashMap<>();
                result.put("responseCode", 503);
                result.put("attempts", attempt);
                result.put("message", "Server is unreachable => proccessID : "+ processId);
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



    private void log(String processId, String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String fullMessage = "[" + timestamp + "] " + message;

        processLogMap.computeIfAbsent(processId, k -> new ArrayList<>()).add(fullMessage);
        logger.info("[{}] {}", processId, fullMessage);
    }

    // Endpoint atau akses log (optional)
    public Map<String, List<String>> getAllProcessLogs() {
        return processLogMap;
    }
}
