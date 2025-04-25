package com.blackcode.app_check_server.service;

import com.blackcode.app_check_server.model.Merchant;
import com.blackcode.app_check_server.repository.MerchantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


@Service
public class MerchantServiceImpl implements MerchantService{

    @Autowired
    MerchantRepository merchantRepository;
    @Override
    public List<Merchant> getListAll() {
        return merchantRepository.findAll();
    }

    @Override
    public Optional<Merchant> getMerchantById(Long id) {
        return merchantRepository.findById(id);
    }

    @Override
    public Merchant addMerchant(Merchant merchant) {
        Merchant merchant1 = new Merchant();
        merchant1.setMerchant_name(merchant.getMerchant_name());
        merchant1.setMerchant_ket(merchant.getMerchant_ket());
        merchant1.setUrl(merchant.getUrl());
        merchant1.setRequestHeader(merchant.getRequestHeader());
        merchant1.setRequestBody(merchant.getRequestBody());
        merchant1.setResponseCode(merchant.getResponseCode());
        merchant1.setResponseHeader(merchant.getResponseHeader());
        merchant1.setResponseBody(merchant.getResponseBody());
        merchant1.setResponseStatus(merchant.getResponseStatus());
        merchant1.setResponseMessage(merchant.getResponseMessage());
        merchant1.setMessageError(merchant.getMessageError());
        return merchantRepository.save(merchant1);
    }

    @Override
    public Merchant updateMerchant(Long id, Merchant merchant) {
        Optional<Merchant> merchant1 = merchantRepository.findById(id);
        Merchant merchantTemp = merchant1.get();
        merchantTemp.setMerchant_name(merchant.getMerchant_name());
        merchantTemp.setMerchant_ket(merchant.getMerchant_ket());
        merchantTemp.setUrl(merchant.getUrl());
        merchantTemp.setRequestHeader(merchant.getRequestHeader());
        merchantTemp.setRequestBody(merchant.getRequestBody());
        merchantTemp.setResponseCode(merchant.getResponseCode());
        merchantTemp.setResponseHeader(merchant.getResponseHeader());
        merchantTemp.setResponseBody(merchant.getResponseBody());
        merchantTemp.setResponseStatus(merchant.getResponseStatus());
        merchantTemp.setResponseMessage(merchant.getResponseMessage());
        merchantTemp.setMessageError(merchant.getMessageError());
        return merchantRepository.save(merchantTemp);
    }

    @Override
    public boolean deleteMerchant(Long id) {
        return false;
    }
}
