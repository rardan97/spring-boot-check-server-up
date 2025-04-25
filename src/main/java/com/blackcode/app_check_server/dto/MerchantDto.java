package com.blackcode.app_check_server.dto;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class MerchantDto {

    private Long merchant_id;

    private String merchant_name;

    private String merchant_ket;

}
