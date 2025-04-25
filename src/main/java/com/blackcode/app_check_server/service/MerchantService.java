package com.blackcode.app_check_server.service;

import com.blackcode.app_check_server.model.Merchant;

import java.util.List;
import java.util.Optional;

public interface MerchantService {
    List<Merchant> getListAll();

    Optional<Merchant> getMerchantById(Long id);

    Merchant addMerchant(Merchant merchant);

    Merchant updateMerchant(Long id, Merchant merchant);

    boolean deleteMerchant(Long id);
}
