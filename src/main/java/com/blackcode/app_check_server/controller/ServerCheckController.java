package com.blackcode.app_check_server.controller;



import com.blackcode.app_check_server.service.AsyncServerCheckService;
import com.blackcode.app_check_server.service.AsyncServerCheckService1;
import com.blackcode.app_check_server.service.AsyncServerCheckService2;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
public class ServerCheckController {
    private final AsyncServerCheckService serverCheckService;

    private final AsyncServerCheckService1 serverCheckService1;

    private final AsyncServerCheckService2 serverCheckService2;

    @GetMapping(value = "/status")
    public ResponseEntity<String> checkServerStatus(@RequestParam String url) {
        serverCheckService.checkServerStatusAsync(url, 3);
        return ResponseEntity.ok("Pengecekan server sedang berjalan secara async di background.");
    }

    @GetMapping(value = "/status1")
    public ResponseEntity<Map<String, Object>> checkServerStatus1(@RequestParam String url) {
        Map<String, Object> result =  serverCheckService1.checkServerStatus(url, 3);
        return ResponseEntity.status((int) result.get("status")).body(result);
    }

    @GetMapping(value = "/status2")
    public Mono<Map<String, Object>> checkServerStatus2(@RequestParam String url) {
        return serverCheckService2.checkServerWithDynamicDelay(url, 3);
        // return ResponseEntity.status((int) result.get("status")).body(result);
    }
}