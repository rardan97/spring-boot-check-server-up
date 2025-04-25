package com.blackcode.app_check_server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Table(name = "merchant")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long merchant_id;

    private String merchant_name;

    private String merchant_ket;

    private String url;

    private String requestHeader;

    private String requestBody;

    private String responseCode;

    private String responseHeader;

    private String responseBody;

    private String responseStatus;

    private String responseMessage;

    private String messageError;


}
