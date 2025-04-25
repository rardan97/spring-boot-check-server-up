package com.blackcode.app_check_server.controller;



import com.blackcode.app_check_server.dto.MerchantDto;
import com.blackcode.app_check_server.service.ServerCheckService;
import com.blackcode.app_check_server.utils.AsyncServerCheckService;
import com.blackcode.app_check_server.utils.AsyncServerCheckService1;
import com.blackcode.app_check_server.utils.AsyncServerCheckService2;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/server")
@RequiredArgsConstructor
public class ServerCheckController {
    private final AsyncServerCheckService asyncServerCheckService;

    private final AsyncServerCheckService1 asyncServerCheckService1;

    private final AsyncServerCheckService2 asyncServerCheckService2;


    @Autowired
    ServerCheckService serverCheckService;

    @GetMapping(value = "/status")
    public ResponseEntity<String> checkServerStatus(@RequestParam String url) {
        asyncServerCheckService.checkServerStatusAsync(url, 3);
        return ResponseEntity.ok("Pengecekan server sedang berjalan secara async di background.");
    }


    @PostMapping(value = "/status-proccess")
    public ResponseEntity<String> checkServerStatusProccess(@RequestBody MerchantDto merchantDto) {
        Map<String, String> reqHeader = new HashMap<>();
        reqHeader.put("Accept", "application/json");
        String url = "http://localhost:3000/";

        Gson gson = new Gson();
        String merchantJsonString = gson.toJson(merchantDto);

        serverCheckService.processMessagePaymentPageCallback(url, reqHeader, merchantJsonString);
        return ResponseEntity.ok("Pengecekan server sedang berjalan secara async di background.");
    }

//    @GetMapping(value = "/status1")
//    public ResponseEntity<Map<String, Object>> checkServerStatus1(@RequestParam String url) {
//        Map<String, Object> result =  serverCheckService1.checkServerStatus(url, 3);
//        return ResponseEntity.status((int) result.get("status")).body(result);
//    }
//
//    @GetMapping(value = "/status2")
//    public Mono<Map<String, Object>> checkServerStatus2(@RequestParam String url) {
//        return serverCheckService2.checkServerWithDynamicDelay(url, 3);
//        // return ResponseEntity.status((int) result.get("status")).body(result);
//    }
}