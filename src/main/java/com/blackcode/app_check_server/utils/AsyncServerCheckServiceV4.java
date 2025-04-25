package com.blackcode.app_check_server.utils;

import com.blackcode.app_check_server.model.Merchant;
import com.blackcode.app_check_server.service.EmailService;
import com.blackcode.app_check_server.service.MerchantService;
import com.google.gson.Gson;
import jakarta.mail.MessagingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

public class AsyncServerCheckServiceV4 {


    @Autowired
    MerchantService merchantService;

    @Autowired
    EmailService emailService;


    private static final Logger logger = LoggerFactory.getLogger(AsyncServerCheckService.class);

    private final WebClient webClient;
    private final Scheduler scheduler;

    private final Map<String, List<String>> processLogMap = new ConcurrentHashMap<>();

    @Autowired
    public AsyncServerCheckServiceV4(WebClient webClient) {
        this.webClient = webClient;
//        this.scheduler = Schedulers.boundedElastic();
        this.scheduler =  Schedulers.newSingle("server-check");
    }


    public Mono<Map<String, String>> checkServerStatusAsync(String url, Map<String, String> reqHeader, String reqBody, int maxRetry) {
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        String processId = UUID.randomUUID().toString();
        processLogMap.put(processId, new ArrayList<>());

        log(processId, "Mulai pengecekan ke " + url + " (maxRetry: " + maxRetry + ")");

        return checkServerWithRetry(url, reqHeader, reqBody, maxRetry, processId)
                .subscribeOn(scheduler)
                .flatMap(result -> {
                    int attempt = (int) result.get("attempts");
                    int responseCode = (int) result.get("responseCode");
                    String resCode = Integer.toString(responseCode);
                    HttpHeaders headers = (HttpHeaders) result.get("responseHeaders");
//                    String headersString = headers.toSingleValueMap().toString();
                    String headersString = (headers != null) ? headers.toSingleValueMap().toString() : "No headers available";
                    String time = LocalDateTime.now().format(timeFormat);
                    String status = "";

                    if (responseCode == 200) {
                        status = "success";
                        logger.info("[{}] | Success : Server SUCCESS at [{}] on attempt #{} | Status code: {}", processId, time, attempt, responseCode);
                    } else {
                        status = "failed";
                        logger.warn("[{}] | Warning : Server error response on attempt #{} | Status code: {}", processId, attempt, responseCode);
                        String to = "risalardan@gmail.com";
                        String subject = "testsubject";
                        String name = "testname";
                        String message = "ttest Email";

                        sendEmailOnFailure(to, subject, message);


                    }

                    if (result.containsKey("error")) {
                        status = "error";
                        logger.error("Error detail: {}", result.get("error"));

                        String to = "risalardan@gmail.com";
                        String subject = "testsubject";
                        String name = "testname";
                        String message = "ttest Email";

                        sendEmailOnFailure(to, subject, message);
                    }

                    Gson gson = new Gson();
                    Merchant merchant = gson.fromJson(reqBody, Merchant.class);
                    System.out.println("Merchant_id : "+merchant.getMerchant_id());
                    System.out.println("Merchant_name : "+merchant.getMerchant_name());
                    System.out.println("Merchant_ket : "+merchant.getMerchant_ket());

                    Optional<Merchant> merchant1 = merchantService.getMerchantById(merchant.getMerchant_id());
                    if(merchant1.isPresent()){
                        merchant1.get().setMerchant_name(merchant.getMerchant_name());
                        merchant1.get().setMerchant_ket(merchant.getMerchant_ket());
                        merchant1.get().setUrl(url);
                        merchant1.get().setRequestHeader(reqHeader.toString());
                        merchant1.get().setRequestBody(reqBody);
                        merchant1.get().setResponseCode(resCode);
                        merchant1.get().setResponseHeader(headersString);
                        merchant1.get().setResponseBody((String) result.get("responseBody"));
                        merchant1.get().setResponseStatus(status);
                        merchant1.get().setResponseMessage((String) result.get("responseMessage"));
                        merchant1.get().setMessageError((String) result.get("responseError"));
                        merchantService.updateMerchant(merchant1.get().getMerchant_id(), merchant1.get());
                        System.out.println("Merchant Di Update pada proccess attempt :"+attempt);
                    }else{
                        Merchant merchantTemp = new Merchant();
                        merchantTemp.setMerchant_name(merchant.getMerchant_name());
                        merchantTemp.setMerchant_ket(merchant.getMerchant_ket());
                        merchantTemp.setUrl(url);
                        merchantTemp.setRequestHeader(reqHeader.toString());
                        merchantTemp.setRequestBody(reqBody);
                        merchantTemp.setResponseCode(resCode);
                        merchantTemp.setResponseHeader(headersString);
                        merchantTemp.setResponseBody((String) result.get("responseBody"));
                        merchantTemp.setResponseStatus(status);
                        merchantTemp.setResponseMessage((String) result.get("responseMessage"));
                        merchantTemp.setMessageError((String) result.get("responseError"));
                        merchantService.addMerchant(merchantTemp);
                        System.out.println("Merchant Di Tambahkan pada proccess attempt :"+attempt);
                    }

                    // Buat Map<String, String> untuk hasil
                    Map<String, String> response = new HashMap<>();
                    response.put("status", responseCode == 200 ? "success" : "failed");
                    response.put("attempt", String.valueOf(attempt));
                    response.put("processId", processId);
                    response.put("timestamp", time);

                    if (result.containsKey("error")) {
                        response.put("error", String.valueOf(result.get("error")));
                    }

                    return Mono.just(response);
                })
                .doOnError(e -> {
                    logger.error("Error : Exception terjadi saat pengecekan server [{}]: {}", processId, e.getMessage(), e);

                    String to = "risalardan@gmail.com";
                    String subject = "testsubject";
                    String name = "testname";
                    String message = "ttest Email";

                    sendEmailOnFailure(to, subject, message);

                })
                .doOnTerminate(() -> logger.info("[{}] Done checking server", processId));
    }

    public Mono<Map<String, Object>> checkServerWithRetry(String url, Map<String, String> reqHeader, String reqBody, int maxRetry, String processId) {
        return tryCheck(url, reqHeader, reqBody, 1, maxRetry, processId);  // Mulai dengan percobaan pertama
    }

    // Method rekursif untuk mencoba cek server
    private Mono<Map<String, Object>> tryCheck(String url, Map<String, String> reqHeader, String reqBody, int attempt, int maxAttempt, String processId) {
        return checkServer(url, reqHeader, reqBody, attempt, processId)  // Lakukan pengecekan server
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
                    Duration delay = getDelayBasedOnAttempt(attempt);

                    log(processId, "Delay " + delay.getSeconds() + "s before retry → Thread: " + Thread.currentThread().getName());

                    return Mono.delay(delay, scheduler)
                            .flatMap(ignored -> tryCheck(url, reqHeader, reqBody, attempt + 1, maxAttempt, processId));  // Coba lagi
                });
    }



    // Method untuk cek server di URL
    private Mono<Map<String, Object>> checkServer(String url, Map<String, String> reqHeader, String reqBody, int attempt, String processId) {
        return webClient.post()
                .uri(url)
                .headers(httpHeaders -> {
                    if(reqHeader != null && !reqHeader.isEmpty()){
                        reqHeader.forEach((key, value) -> {
                            httpHeaders.set(key, value);
                        });
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(reqBody) // Mengirim body jika dibutuhkan
                .retrieve()
                .toEntity(String.class)
                .timeout(Duration.ofSeconds(5)) // Timeout jika server lambat
                .publishOn(scheduler)
                .map(response -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("responseCode", response.getStatusCode().value());
                    result.put("attempts", attempt);
                    result.put("responseMessage", response.getStatusCode().value() == 200 ?
                            "Server is online" : "Server returned error => processID : " + processId);
                    result.put("responseError", "");
                    result.put("responseHeaders", response.getHeaders()); // ✅ Tambahkan ini
                    result.put("responseBody", response.getBody());       // Optional: untuk simpan body juga
                    return result;
                })
                .onErrorResume(e -> {
                    // Jika terjadi error (misalnya server down), return 503
                    Map<String, Object> result = new HashMap<>();
                    result.put("responseCode", 503);
                    result.put("attempts", attempt);
                    result.put("responseMessage", "Server is unreachable => processID : "+ processId);
                    result.put("responseError", e.getMessage());
                    if (e instanceof WebClientResponseException webEx) {
                        result.put("responseHeaders", webEx.getHeaders());
                        result.put("responseBody", webEx.getResponseBodyAsString());
                    }
                    return Mono.just(result);
                });
    }

    private Duration getDelayBasedOnAttempt(int attempt) {
        switch (attempt) {
            case 1: return Duration.ofSeconds(10);
            case 2: return Duration.ofSeconds(40);
            default: return Duration.ofSeconds(50);
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


    public void sendEmailOnFailure(String to, String subject, String message) {
        // Menggunakan Mono.fromRunnable untuk eksekusi asinkron
        Mono.fromRunnable(() -> {
                    try {
                        // Mengirim email secara asinkron menggunakan @Async di EmailService
                        emailService.sendEmail(to, subject, "Admin", message);
                    } catch (MessagingException e) {
                        // Menangani exception pengiriman email
                        logger.error("Failed to send email notification: {}", e.getMessage(), e);
                    }
                })
                // Penanganan error lebih lanjut jika terjadi kesalahan saat menjalankan task
                .doOnError(e -> logger.error("Error while executing email task: {}", e.getMessage(), e))
                // Subscribe untuk memulai eksekusi asinkron
                .subscribe();
    }
}
