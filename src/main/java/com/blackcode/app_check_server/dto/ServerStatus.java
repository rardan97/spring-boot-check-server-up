package com.blackcode.app_check_server;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServerStatus {
    private String status;
    private Integer statusCode;
    private String error;
    private LocalDateTime timestamp;
    private int attempts;
    private String message;
    private String nextAttemptIn;
}
