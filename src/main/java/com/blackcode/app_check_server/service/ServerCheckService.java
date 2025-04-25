package com.blackcode.app_check_server.service;

import com.blackcode.app_check_server.utils.AsyncServerCheckService;
import com.blackcode.app_check_server.utils.AsyncServerCheckServiceV4;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class ServerCheckService {

    @Autowired
    AsyncServerCheckServiceV4 asyncServerCheckServiceV4;

    private static final Logger logger = LoggerFactory.getLogger(AsyncServerCheckService.class);




    public boolean processMessagePaymentPageCallback(String url, Map<String, String> reqHeader, String reqBody) {
        String TAG = "sTime=" + System.currentTimeMillis() + "|paymentPageCallback|";
        System.out.println("Prosess Service : "+reqBody);
        boolean result = false;
        try {
            asyncServerCheckServiceV4.checkServerStatusAsync(url, reqHeader, reqBody, 3).subscribe();
            result = true;
        } catch (Exception e) {
            logger.error(TAG + "processMessagePaymentPageCallback|error=" + e.getMessage(), e);
        }
        return result;
    }



}
